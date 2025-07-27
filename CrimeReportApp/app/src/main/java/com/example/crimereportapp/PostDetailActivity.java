package com.example.crimereportapp;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {
    private static final String TAG = "PostDetailActivity";

    private TextView tvUsername;
    private TextView tvPostDescription;
    private ImageView ivPostMedia;
    private TextView tvPostTimestamp;
    private TextView tvLikes; // Added
    private RecyclerView rvComments; // Added
    private CommentAdapter commentAdapter; // Added
    private ForumViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Initialize views
        tvUsername = findViewById(R.id.tvUsername);
        tvPostDescription = findViewById(R.id.tvMessage); // Changed to match the layout
        ivPostMedia = findViewById(R.id.ivPostMedia);
        tvPostTimestamp = findViewById(R.id.tvPostTimestamp);
        tvLikes = findViewById(R.id.tvLikes); // Added
        rvComments = findViewById(R.id.rvComments); // Added

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ForumViewModel.class);

        // Setup RecyclerView for comments
        List<ForumComment> commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        // Get post ID from intent
        long postId = getIntent().getLongExtra("POST_ID", -1);
        if (postId == -1) {
            Log.e(TAG, "No POST_ID provided in intent");
            Toast.makeText(this, "Error loading post details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch post details
        viewModel.getPostById(postId).observe(this, post -> {
            if (post != null) {
                Log.d(TAG, "Post loaded: " + post.getUserName() + ", " + post.getMessage());
                tvUsername.setText(post.getUserName() != null ? post.getUserName() : "Anonymous");
                tvPostDescription.setText(post.getMessage() != null ? post.getMessage() : "No message available");
                tvLikes.setText("Likes: " + post.getLikes()); // Added
                if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
                    ivPostMedia.setVisibility(View.VISIBLE);
                    loadImage(post.getMediaUrl());
                } else {
                    ivPostMedia.setVisibility(View.GONE);
                }
                tvPostTimestamp.setText(formatTimestamp(post.getTimestamp()));
            } else {
                Log.e(TAG, "Post not found for ID: " + postId);
                Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Fetch comments
        viewModel.getCommentsForPost(postId).observe(this, comments -> {
            if (comments != null) {
                Log.d(TAG, "Comments loaded: " + comments.size());
                commentAdapter.updateComments(comments);
            } else {
                Log.d(TAG, "No comments found for post ID: " + postId);
            }
        });

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post Detail");
        }
    }

    private void loadImage(String mediaUrl) {
        try {
            Glide.with(this)
                    .load(Uri.parse(mediaUrl))
                    .centerCrop()
                    .error(R.drawable.error_image)
                    .into(ivPostMedia);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            ivPostMedia.setVisibility(View.GONE);
        }
    }

    private String formatTimestamp(long timestamp) {
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp", e);
            return "Time not available";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
