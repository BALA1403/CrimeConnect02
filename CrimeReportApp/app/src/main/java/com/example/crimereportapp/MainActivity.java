package com.example.crimereportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button btnSOS, btnCrimeReport, btnCommunityForum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Setting content view: activity_main");
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Crime Report App");
        } else {
            Log.w(TAG, "Action bar not available");
        }

        initializeViews();
        setClickListeners();
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");
        btnSOS = findViewById(R.id.btnSOS);
        btnCrimeReport = findViewById(R.id.btnCrimeReport);
        btnCommunityForum = findViewById(R.id.btnCommunityForum);

        if (btnCrimeReport == null) {
            Log.e(TAG, "btnCrimeReport is null!");
            Toast.makeText(this, "Crime Report button not found", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "btnCrimeReport found");
        }
    }

    private void setClickListeners() {
        Log.d(TAG, "Setting click listeners");
        if (btnSOS != null) {
            btnSOS.setOnClickListener(view -> {
                Log.d(TAG, "SOS button clicked");
                startActivity(new Intent(MainActivity.this, SOSActivity.class));
            });
        }

        if (btnCrimeReport != null) {
            btnCrimeReport.setOnClickListener(view -> {
                Log.d(TAG, "Crime Report button clicked - launching activity");
                try {
                    Intent crimeReportIntent = new Intent(MainActivity.this, CrimeReportActivity.class);
                    startActivity(crimeReportIntent);
                    Log.d(TAG, "CrimeReportActivity started");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start CrimeReportActivity", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Log.e(TAG, "btnCrimeReport listener not set - button is null");
        }

        if (btnCommunityForum != null) {
            btnCommunityForum.setOnClickListener(view -> {
                Log.d(TAG, "Community Forum button clicked");
                startActivity(new Intent(MainActivity.this, CommunityForumActivity.class));
            });
        }
    }
}