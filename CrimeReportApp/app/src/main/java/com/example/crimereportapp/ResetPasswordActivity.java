package com.example.crimereportapp;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {
    private EditText newPasswordInput, confirmPasswordInput;
    private Button resetPasswordBtn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn);

        resetPasswordBtn.setOnClickListener(v -> {
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (validatePasswords(newPassword, confirmPassword)) {
                resetPassword(newPassword);
            }
        });
    }

    private boolean validatePasswords(String newPassword, String confirmPassword) {
        // Password validation
        if (newPassword.isEmpty()) {
            newPasswordInput.setError("Password cannot be empty");
            return false;
        }

        if (newPassword.length() < 6) {
            newPasswordInput.setError("Password must be at least 6 characters");
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void resetPassword(String newPassword) {
        // Get current user from Firebase
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password Reset Successful", Toast.LENGTH_SHORT).show();
                            // Navigate back to login or main screen
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Password Reset Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}