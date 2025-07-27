package com.example.crimereportapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class CrimeReportActivity extends AppCompatActivity {

    private static final String TAG = "CrimeReportActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_VIDEO_CAPTURE = 102;
    private static final int REQUEST_AUDIO_CAPTURE = 103;
    private static final int REQUEST_PICK_IMAGE = 104;
    private static final int REQUEST_PICK_VIDEO = 105;
    private static final int REQUEST_PICK_AUDIO = 106;
    private static final int REQUEST_MULTIPLE_PERMISSIONS = 100;
    private static final int REQUEST_APP_SETTINGS = 107;

    private EditText title, description, location, date, time;
    private Spinner crimeTypeSpinner;
    private ImageButton uploadEvidence;
    private Button submitReport, back, home;
    private List<Uri> mediaUris = new ArrayList<>();
    private List<String> mediaTypes = new ArrayList<>();
    private String currentPhotoPath;
    private Map<String, String> permissionExplanations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        try {
            setContentView(R.layout.activity_crime_report);
            Log.d(TAG, "Content view set");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set content view", e);
            Toast.makeText(this, "Layout error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupPermissionExplanations();
        initializeUI();
        Log.d(TAG, "Activity created");
    }

    private void setupPermissionExplanations() {
        permissionExplanations = new HashMap<>();
        permissionExplanations.put(Manifest.permission.CAMERA, "Camera access is needed to capture photos and videos.");
        permissionExplanations.put(Manifest.permission.RECORD_AUDIO, "Microphone access is needed to record audio.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionExplanations.put(Manifest.permission.READ_MEDIA_IMAGES, "Storage access for images.");
            permissionExplanations.put(Manifest.permission.READ_MEDIA_VIDEO, "Storage access for videos.");
            permissionExplanations.put(Manifest.permission.READ_MEDIA_AUDIO, "Storage access for audio.");
        } else {
            permissionExplanations.put(Manifest.permission.READ_EXTERNAL_STORAGE, "Storage access to select media.");
            permissionExplanations.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, "Storage access to save media.");
        }
        permissionExplanations.put(Manifest.permission.INTERNET, "Internet access to upload files and send report.");
    }

    private void initializeUI() {
        Log.d(TAG, "Initializing UI");
        try {
            home = findViewById(R.id.buttonHome);
            uploadEvidence = findViewById(R.id.imageButtonEvidence);
            submitReport = findViewById(R.id.buttonSubmit);
            back = findViewById(R.id.buttonBack);
            title = findViewById(R.id.editTextTitle);
            description = findViewById(R.id.editTextDescription);
            location = findViewById(R.id.editTextLocation);
            date = findViewById(R.id.editTextDate);
            time = findViewById(R.id.editTextTime);
            crimeTypeSpinner = findViewById(R.id.spinnerCrimeType);

            if (title == null || description == null || location == null || date == null || time == null || submitReport == null || uploadEvidence == null) {
                Log.e(TAG, "One or more UI elements are null");
                Toast.makeText(this, "UI setup error", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.crime_types, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            crimeTypeSpinner.setAdapter(adapter);
            Log.d(TAG, "Spinner populated");

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date now = new Date();
            date.setText(dateFormat.format(now));
            time.setText(timeFormat.format(now));

            home.setOnClickListener(v -> navigateTo(MainActivity.class));
            back.setOnClickListener(v -> finish());
            uploadEvidence.setOnClickListener(v -> showMediaPickerDialog());
            submitReport.setOnClickListener(v -> submitCrimeReport());
            Log.d(TAG, "UI initialized");
        } catch (Exception e) {
            Log.e(TAG, "UI initialization failed", e);
            Toast.makeText(this, "UI error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        try {
            startActivity(new Intent(this, targetActivity));
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Navigation failed", e);
            showToast("Navigation error: " + e.getMessage());
        }
    }

    private void showMediaPickerDialog() {
        String[] options = {"Take Photo", "Record Video", "Record Audio", "Upload Image", "Upload Video", "Upload Audio"};
        new AlertDialog.Builder(this)
                .setTitle("Select Media Type")
                .setItems(options, (dialog, which) -> handleMediaSelection(which))
                .show();
    }

    private void handleMediaSelection(int choice) {
        switch (choice) {
            case 0: requestMediaPermissions(new String[]{Manifest.permission.CAMERA, getStorageWritePermission()}, () -> captureImage()); break;
            case 1: requestMediaPermissions(new String[]{Manifest.permission.CAMERA, getStorageWritePermission()}, () -> captureVideo()); break;
            case 2: requestMediaPermissions(new String[]{Manifest.permission.RECORD_AUDIO, getStorageWritePermission()}, () -> captureAudio()); break;
            case 3: requestMediaPermissions(new String[]{getStorageReadPermission("image")}, () -> pickImageFromGallery()); break;
            case 4: requestMediaPermissions(new String[]{getStorageReadPermission("video")}, () -> pickVideoFromGallery()); break;
            case 5: requestMediaPermissions(new String[]{getStorageReadPermission("audio")}, () -> pickAudioFromGallery()); break;
        }
    }

    private String getStorageWritePermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
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

    private void requestMediaPermissions(String[] permissions, Runnable onGranted) {
        boolean allGranted = true;
        List<String> permissionsNeeded = new ArrayList<>();

        for (String permission : permissions) {
            int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
            Log.d(TAG, "Checking permission " + permission + ": " + (permissionStatus == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                permissionsNeeded.add(permission);
            }
        }

        if (allGranted) {
            Log.d(TAG, "All permissions granted for media action");
            onGranted.run();
            return;
        }

        boolean shouldShowRationale = false;
        for (String permission : permissionsNeeded) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                shouldShowRationale = true;
                break;
            }
        }

        if (shouldShowRationale) {
            StringBuilder message = new StringBuilder("This app needs:\n\n");
            for (String permission : permissionsNeeded) {
                String explanation = permissionExplanations.getOrDefault(permission, "Required");
                message.append("• ").append(explanation).append("\n");
            }
            new AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage(message.toString())
                    .setPositiveButton("Grant", (dialog, which) -> ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_MULTIPLE_PERMISSIONS))
                    .setNegativeButton("Cancel", (dialog, which) -> showToast("Action canceled"))
                    .show();
        } else {
            boolean anyPermanentlyDenied = false;
            for (String permission : permissionsNeeded) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission) &&
                        ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    anyPermanentlyDenied = true;
                    break;
                }
            }
            if (anyPermanentlyDenied) {
                new AlertDialog.Builder(this)
                        .setTitle("Permissions Denied")
                        .setMessage("Some permissions are denied. Enable in Settings.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent, REQUEST_APP_SETTINGS);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> showToast("Feature unavailable"))
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_MULTIPLE_PERMISSIONS);
            }
        }
    }

    private void captureImage() {
        Log.d(TAG, "Attempting to capture image");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d(TAG, "Image file created: " + photoFile.getAbsolutePath());
                Uri photoUri = FileProvider.getUriForFile(this, "com.example.crimereportapp.fileprovider", photoFile);
                mediaUris.add(photoUri);
                mediaTypes.add("image");
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                Log.d(TAG, "Camera intent started");
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                showToast("Error creating image file");
            }
        } else {
            Log.w(TAG, "No camera app available");
            showToast("No camera app available");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void captureVideo() {
        Log.d(TAG, "Attempting to capture video");
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            Log.d(TAG, "Video intent started");
        } else {
            showToast("No video app available");
        }
    }

    private void captureAudio() {
        Log.d(TAG, "Attempting to capture audio");
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_AUDIO_CAPTURE);
            Log.d(TAG, "Audio intent started");
        } else {
            showToast("No audio recording app available");
        }
    }

    private void pickImageFromGallery() {
        Log.d(TAG, "Attempting to pick image");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
        Log.d(TAG, "Image picker intent started");
    }

    private void pickVideoFromGallery() {
        Log.d(TAG, "Attempting to pick video");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_PICK_VIDEO);
        Log.d(TAG, "Video picker intent started");
    }

    private void pickAudioFromGallery() {
        Log.d(TAG, "Attempting to pick audio");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_PICK_AUDIO);
        Log.d(TAG, "Audio picker intent started");
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void submitCrimeReport() {
        Log.d(TAG, "submitCrimeReport called");
        if (!isFormValid()) return;

        final String titleText = title.getText().toString().trim();
        final String descriptionText = description.getText().toString().trim();
        final String locationText = location.getText().toString().trim();
        final String dateText = date.getText().toString().trim();
        final String timeText = time.getText().toString().trim();
        final String crimeType;
        try {
            crimeType = crimeTypeSpinner.getSelectedItem().toString();
        } catch (Exception e) {
            Log.e(TAG, "Spinner selection error", e);
            showToast("Please select a crime type");
            return;
        }

        StringBuilder mediaPaths = new StringBuilder();
        for (int i = 0; i < mediaUris.size(); i++) {
            String type = mediaTypes.get(i);
            String path = mediaUris.get(i).toString();
            mediaPaths.append(type.substring(0, 1).toUpperCase()).append(type.substring(1))
                    .append(" ").append(i + 1).append(": ").append(path).append("\n");
        }
        final String mediaPathsText = mediaPaths.length() > 0 ? mediaPaths.toString() : "No media attached";

        new Thread(() -> {
            if (ContextCompat.checkSelfPermission(CrimeReportActivity.this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                try {
                    sendEmail(titleText, descriptionText, locationText, dateText, timeText, crimeType, mediaPathsText);
                    runOnUiThread(() -> {
                        showToast("Report sent to ");
                        navigateTo(MainActivity.class);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Email send failed", e);
                    runOnUiThread(() -> showToast("Email send failed: " + e.getMessage()));
                }
            } else {
                Log.w(TAG, "Internet permission not granted");
                runOnUiThread(() -> showToast("No internet permission—email not sent"));
            }
        }).start();
    }

    private void sendEmail(String title, String description, String location, String date, String time, String crimeType, String mediaPaths) throws MessagingException {
        Log.d(TAG, "sendEmail started");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        Log.d(TAG, "SMTP properties set");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                Log.d(TAG, "Authenticating with username: " + username);
                return new PasswordAuthentication(username, password);
            }
        });
        Log.d(TAG, "SMTP session created");

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(""));
        message.setSubject("Crime Report: " + title);
        message.setText("Crime Type: " + crimeType + "\n" +
                "Description: " + description + "\n" +
                "Location: " + location + "\n" +
                "Date: " + date + "\n" +
                "Time: " + time + "\n" +
                "Media:\n" + mediaPaths);
        Log.d(TAG, "Email message prepared");

        Transport.send(message);
        Log.d(TAG, "Email sent");
    }

    private boolean isFormValid() {
        if (title.getText().toString().trim().isEmpty() ||
                description.getText().toString().trim().isEmpty() ||
                location.getText().toString().trim().isEmpty() ||
                date.getText().toString().trim().isEmpty() ||
                time.getText().toString().trim().isEmpty()) {
            showToast("Please fill all required fields");
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_APP_SETTINGS) {
            handleMediaSelectionAfterSettings();
            return;
        }

        if (resultCode == RESULT_OK) {
            if (data != null && data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    String type = getMediaTypeFromRequestCode(requestCode);
                    mediaUris.add(uri);
                    mediaTypes.add(type);
                    Log.d(TAG, "Added " + type + ": " + uri.toString());
                }
            } else if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                String type = getMediaTypeFromRequestCode(requestCode);
                mediaUris.add(uri);
                mediaTypes.add(type);
                Log.d(TAG, "Added " + type + ": " + uri.toString());
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Log.d(TAG, "Camera image added earlier: " + mediaUris.get(mediaUris.size() - 1));
            }
            updateEvidenceUI("Media added (" + mediaUris.size() + " total)");
        } else {
            Log.w(TAG, "Media capture failed or canceled: resultCode=" + resultCode);
            showToast("Media capture failed or canceled");
        }
    }

    private String getMediaTypeFromRequestCode(int requestCode) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
            case REQUEST_PICK_IMAGE: return "image";
            case REQUEST_VIDEO_CAPTURE:
            case REQUEST_PICK_VIDEO: return "video";
            case REQUEST_AUDIO_CAPTURE:
            case REQUEST_PICK_AUDIO: return "audio";
            default: return "unknown";
        }
    }

    private void handleMediaSelectionAfterSettings() {
        requestAllPermissions();
    }

    private void requestAllPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        String[] requiredPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                new String[] {
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.INTERNET
                } :
                new String[] {
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
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

    private void updateEvidenceUI(String message) {
        showToast(message);
        if (!mediaUris.isEmpty()) {
            uploadEvidence.setImageResource(android.R.drawable.ic_menu_gallery);
            Log.d(TAG, "Media list updated: " + mediaUris.size() + " items");
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
            }

            if (allGranted) {
                Log.d(TAG, "All permissions granted");
                showToast("Permissions granted");
                handleMediaSelectionAfterSettings();
            } else {
                Log.w(TAG, "Some permissions denied: " + deniedPermissions);
                showToast("Some permissions denied—features limited");
            }
        }
    }
}