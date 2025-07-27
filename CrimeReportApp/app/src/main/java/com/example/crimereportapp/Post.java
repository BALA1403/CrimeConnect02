package com.example.crimereportapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class Post extends AppCompatActivity {

    public static final int GALLERY_REQUEST = 1;
    ImageButton uploadEvidence;
    Button submitReport, back, home;
    EditText title, description, location, date, time;
    Spinner crimeTypeSpinner;
    Uri evidenceImageUri = null;
    private StorageReference mStorage;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Initialize UI components
        home = findViewById(R.id.buttonHome);
        uploadEvidence = findViewById(R.id.imageButtonEvidence);
        submitReport = findViewById(R.id.buttonSubmit);
        back = findViewById(R.id.buttonBack);

        // Initialize EditText fields
        title = findViewById(R.id.editTextTitle);
        description = findViewById(R.id.editTextDescription);
        location = findViewById(R.id.editTextLocation);
        date = findViewById(R.id.editTextDate);
        time = findViewById(R.id.editTextTime);

        // Initialize Spinner
        crimeTypeSpinner = findViewById(R.id.spinnerCrimeType);

        // Initialize Firebase Storage
        mStorage = FirebaseStorage.getInstance().getReference();

        // Home button click listener
        home.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
            finish();
        });

        // Back button click listener
        back.setOnClickListener(v -> finish());

        // Image upload click listener
        uploadEvidence.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, GALLERY_REQUEST);
        });

        // Initialize progress dialog
        mProgress = new ProgressDialog(this);

        // Submit report click listener
        submitReport.setOnClickListener(v -> submitCrimeReport());
    }

    private void submitCrimeReport() {
        mProgress.setMessage("Submitting crime report...");
        mProgress.show();

        final String titleVal = title.getText().toString().trim();
        final String descriptionVal = description.getText().toString().trim();
        final String locationVal = location.getText().toString().trim();
        final String dateVal = date.getText().toString().trim();
        final String timeVal = time.getText().toString().trim();
        final String crimeTypeVal = crimeTypeSpinner.getSelectedItem().toString();

        if (!TextUtils.isEmpty(titleVal) && !TextUtils.isEmpty(descriptionVal) &&
                !TextUtils.isEmpty(locationVal) && !TextUtils.isEmpty(dateVal) &&
                !TextUtils.isEmpty(timeVal)) {

            if (evidenceImageUri != null) {
                // Upload image if available
                final StorageReference filepath = mStorage.child("Evidence_Images").child(UUID.randomUUID().toString());
                filepath.putFile(evidenceImageUri).addOnSuccessListener(taskSnapshot -> {
                    filepath.getDownloadUrl().addOnSuccessListener(uri -> {
                        sendReportEmail(titleVal, descriptionVal, locationVal, dateVal, timeVal, crimeTypeVal, uri.toString());
                    }).addOnFailureListener(e -> {
                        Toast.makeText(Post.this, "Error while uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        mProgress.dismiss();
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(Post.this, "Error uploading evidence image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mProgress.dismiss();
                });
            } else {
                // Send report without image
                sendReportEmail(titleVal, descriptionVal, locationVal, dateVal, timeVal, crimeTypeVal, null);
            }
        } else {
            mProgress.dismiss();
            Toast.makeText(Post.this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendReportEmail(String title, String description, String location, String date, String time, String crimeType, @Nullable String imageUrl) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"crimeconnectapp@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Crime Report: " + title);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Description: " + description + "\nLocation: " + location + "\nDate: " + date + "\nTime: " + time + "\nCrime Type: " + crimeType);

        if (imageUrl != null) {
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrl));
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send crime report via:"));
            mProgress.dismiss();
        } catch (Exception e) {
            Toast.makeText(this, "Error sending report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            mProgress.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null) {
            evidenceImageUri = data.getData();
            uploadEvidence.setImageURI(evidenceImageUri);
        }
    }
}