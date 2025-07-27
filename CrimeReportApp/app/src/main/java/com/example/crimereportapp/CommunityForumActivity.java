package com.example.crimereportapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityForumActivity extends AppCompatActivity implements PostAdapter.OnItemClickListener {

    private static final int REQUEST_MULTIPLE_PERMISSIONS = 100;
    private static final int REQUEST_APP_SETTINGS = 107;
    private static final String TAG = "CommunityForum";

    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private ArrayList<ForumPost> postList;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentMediaPath;
    private BottomSheetDialog postDialog;
    private ImageView previewImage;
    private boolean isUploading = false;
    private ForumViewModel viewModel;
    private ExecutorService executorService;
    private UserManager userManager;

    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> loginLauncher;
    private boolean shouldOpenPostDialogAfterLogin = false; // Flag to reopen dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_forum);

        executorService = Executors.newFixedThreadPool(4);
        userManager = UserManager.getInstance(this);

        setupViewModel();
        setupViews();
        setupLaunchers();
        requestPermissions();
        checkLoginAndProceed();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ForumViewModel.class);
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        FloatingActionButton fabNewPost = findViewById(R.id.fabNewPost);
        if (fabNewPost == null) {
            Log.e(TAG, "fabNewPost is null - check your layout XML");
            return;
        }
        fabNewPost.setOnClickListener(v -> {
            Log.d(TAG, "+ button clicked");
            if (checkLogin()) {
                showPostDialog();
            } else {
                shouldOpenPostDialogAfterLogin = true; // Set flag to open dialog after login
            }
        });

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this);
        recyclerViewPosts.setAdapter(postAdapter);
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(this::fetchPosts);
    }

    private void setupLaunchers() {
        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleMediaResult(result.getData().getData());
                    } else {
                        Log.w(TAG, "Media picker canceled or failed");
                        showToast("Media selection canceled");
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.containsKey("data")) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            handleCameraResult(imageBitmap);
                        }
                    } else {
                        Log.w(TAG, "Camera capture canceled or failed");
                        showToast("Camera capture canceled");
                    }
                }
        );

        loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "Login successful");
                        User currentUser = userManager.getCurrentUser();
                        if (currentUser != null) {
                            Log.d(TAG, "User logged in: " + currentUser.getUsername());
                            fetchPosts(); // Load posts after login
                            if (shouldOpenPostDialogAfterLogin) {
                                showPostDialog(); // Open dialog if triggered by FAB
                                shouldOpenPostDialogAfterLogin = false;
                            }
                        } else {
                            Log.e(TAG, "User still null after login—check UserManager");
                            showToast("Login failed—user not set");
                            finish();
                        }
                    } else {
                        Log.w(TAG, "Login canceled or failed");
                        showToast("Login required to proceed");
                        finish(); // Close if login canceled
                    }
                }
        );
    }

    private void requestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        String[] requiredPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                } :
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                };

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_MULTIPLE_PERMISSIONS);
            Log.d(TAG, "Requesting permissions: " + permissionsNeeded);
        } else {
            Log.d(TAG, "All permissions already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MULTIPLE_PERMISSIONS) {
            boolean allGranted = true;
            List<String> deniedPermissions = new ArrayList<>();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    deniedPermissions.add(permissions[i]);
                }
                Log.d(TAG, "Permission " + permissions[i] + ": " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            }

            if (!allGranted) {
                Log.w(TAG, "Some permissions denied: " + deniedPermissions);
                showToast("Some permissions denied—media features may be limited");
            } else {
                Log.d(TAG, "All permissions granted");
            }
        }
    }

    private boolean checkLogin() {
        User currentUser = userManager.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User not logged in—redirecting to LoginActivity");
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginLauncher.launch(loginIntent);
            return false;
        }
        Log.d(TAG, "User logged in: " + currentUser.getUsername());
        return true;
    }

    private void checkLoginAndProceed() {
        if (!checkLogin()) {
            // Activity will wait for login result via launcher
        } else {
            fetchPosts();
        }
    }

    private void showPostDialog() {
        Log.d(TAG, "showPostDialog method called");
        postDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_post, null);
        if (dialogView == null) {
            Log.e(TAG, "Dialog view is null");
            return;
        }
        postDialog.setContentView(dialogView);

        EditText etMessage = dialogView.findViewById(R.id.etMessage);
        MaterialButton btnMedia = dialogView.findViewById(R.id.btnMedia);
        MaterialButton btnCamera = dialogView.findViewById(R.id.btnCamera);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSend);
        previewImage = dialogView.findViewById(R.id.previewImage);
        ProgressBar uploadProgress = dialogView.findViewById(R.id.uploadProgress);

        if (etMessage == null || btnMedia == null || btnCamera == null || btnSend == null || previewImage == null || uploadProgress == null) {
            Log.e(TAG, "One or more views are null in dialog_new_post.xml");
            showToast("Dialog setup error");
            postDialog.dismiss();
            return;
        }

        btnMedia.setOnClickListener(v -> openMediaPicker());
        btnCamera.setOnClickListener(v -> openCamera());
        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty() || currentMediaPath != null) {
                if (!isUploading) {
                    createPost(message);
                } else {
                    showToast("Please wait for media processing to complete");
                }
            } else {
                showToast("Please enter a message or attach media");
            }
        });

        postDialog.show();
        Log.d(TAG, "Post dialog shown");
    }

    private void openMediaPicker() {
        if (checkPermissions(getStorageReadPermission("image")) && checkPermissions(getStorageReadPermission("video"))) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/* video/*");
            mediaPickerLauncher.launch(intent);
        } else {
            requestMediaPermissions(new String[]{getStorageReadPermission("image"), getStorageReadPermission("video")},
                    this::openMediaPicker);
        }
    }

    private void openCamera() {
        if (checkPermissions(Manifest.permission.CAMERA) && checkPermissions(getStorageWritePermission())) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(intent);
            } else {
                showToast("No camera app available");
            }
        } else {
            requestMediaPermissions(new String[]{Manifest.permission.CAMERA, getStorageWritePermission()},
                    this::openCamera);
        }
    }

    private void handleMediaResult(Uri uri) {
        if (uri != null) {
            previewImage.setVisibility(View.VISIBLE);
            previewImage.setImageURI(uri);
            saveMediaToInternalStorage(uri);
        }
    }

    private void handleCameraResult(Bitmap bitmap) {
        if (bitmap != null) {
            previewImage.setVisibility(View.VISIBLE);
            previewImage.setImageBitmap(bitmap);
            saveMediaToInternalStorage(bitmap);
        }
    }

    private void saveMediaToInternalStorage(Object mediaSource) {
        ProgressBar uploadProgress = postDialog.findViewById(R.id.uploadProgress);
        if (uploadProgress != null) {
            uploadProgress.setVisibility(View.VISIBLE);
        }
        isUploading = true;

        executorService.execute(() -> {
            try {
                String fileName = UUID.randomUUID().toString() + ".jpg";
                File directory = new File(getFilesDir(), "forum_media");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File file = new File(directory, fileName);
                FileOutputStream fos = new FileOutputStream(file);

                if (mediaSource instanceof Uri) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), (Uri) mediaSource);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                } else if (mediaSource instanceof Bitmap) {
                    ((Bitmap) mediaSource).compress(Bitmap.CompressFormat.JPEG, 90, fos);
                }

                fos.close();
                currentMediaPath = file.getAbsolutePath();

                runOnUiThread(() -> {
                    if (uploadProgress != null) {
                        uploadProgress.setVisibility(View.GONE);
                    }
                    isUploading = false;
                    showToast("Media saved successfully");
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    if (uploadProgress != null) {
                        uploadProgress.setVisibility(View.GONE);
                    }
                    isUploading = false;
                    showToast("Failed to save media: " + e.getMessage());
                    Log.e(TAG, "Failed to save media", e);
                });
            }
        });
    }

    private void createPost(String message) {
        try {
            progressBar.setVisibility(View.VISIBLE);

            User currentUser = userManager.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "User still null after login check—should not happen");
                showToast("User session error");
                progressBar.setVisibility(View.GONE);
                return;
            }

            ForumPost post = new ForumPost();
            post.setUserId(currentUser.getId());
            post.setUserName(currentUser.getUsername());
            post.setMessage(message);
            post.setMediaUrl(currentMediaPath);
            post.setTimestamp(System.currentTimeMillis());
            post.setLikes(0);

            viewModel.insertPost(post).observe(this, postId -> {
                progressBar.setVisibility(View.GONE);
                if (postId != null && postId > 0) {
                    showToast("Post created successfully with ID: " + postId);
                    if (postDialog != null && postDialog.isShowing()) {
                        try {
                            clearPostDialog();
                            postDialog.dismiss();
                            fetchPosts();
                        } catch (Exception e) {
                            Log.e(TAG, "Error dismissing dialog", e);
                        }
                    }
                } else {
                    showToast("Failed to create post");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in createPost", e);
            showToast("Error creating post: " + e.getMessage());
            progressBar.setVisibility(View.GONE);
        }
    }

    private void fetchPosts() {
        try {
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "Fetching posts from database");

            viewModel.getAllPosts().observe(this, posts -> {
                try {
                    if (posts != null) {
                        postList.clear();
                        Log.d(TAG, "Received " + posts.size() + " posts from database");
                        postList.addAll(posts);
                        if (postAdapter != null) {
                            postAdapter.notifyDataSetChanged();
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    if (recyclerViewPosts != null && !postList.isEmpty()) {
                        recyclerViewPosts.smoothScrollToPosition(0);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing posts", e);
                    progressBar.setVisibility(View.GONE);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchPosts", e);
            progressBar.setVisibility(View.GONE);
        }
    }


    @Override
    public void onItemClick(ForumPost post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("POST_ID", post.getId());
        startActivity(intent);
    }

    private void clearPostDialog() {
        if (postDialog != null) {
            EditText etMessage = postDialog.findViewById(R.id.etMessage);
            if (etMessage != null) {
                etMessage.setText("");
            }
            if (previewImage != null) {
                previewImage.setVisibility(View.GONE);
            }
            currentMediaPath = null;
        }
    }

    private boolean checkPermissions(String permission) {
        int status = ContextCompat.checkSelfPermission(this, permission);
        Log.d(TAG, "Checking permission " + permission + ": " + (status == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        return status == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMediaPermissions(String[] permissions, Runnable onGranted) {
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (!checkPermissions(permission)) {
                permissionsNeeded.add(permission);
            }
        }

        if (permissionsNeeded.isEmpty()) {
            onGranted.run();
        } else {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_MULTIPLE_PERMISSIONS);
        }
    }

    private String getStorageWritePermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : // Proxy for write with FileProvider
                Manifest.permission.WRITE_EXTERNAL_STORAGE;
    }

    private String getStorageReadPermission(String type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            switch (type) {
                case "image": return Manifest.permission.READ_MEDIA_IMAGES;
                case "video": return Manifest.permission.READ_MEDIA_VIDEO;
                case "audio": return Manifest.permission.READ_MEDIA_AUDIO;
                default: return Manifest.permission.READ_MEDIA_IMAGES;
            }
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
