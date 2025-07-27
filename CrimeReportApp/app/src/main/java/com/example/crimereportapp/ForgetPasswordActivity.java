package com.example.crimereportapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class ForgetPasswordActivity extends AppCompatActivity {
    private EditText emailInput, otpInput;
    private Button sendOtpBtn, verifyOtpBtn;
    private String generatedOtp;
    private long otpGeneratedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Views
        emailInput = findViewById(R.id.emailInput);
        otpInput = findViewById(R.id.otpInput);
        sendOtpBtn = findViewById(R.id.sendOtpBtn);
        verifyOtpBtn = findViewById(R.id.verifyOtpBtn);

        sendOtpBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (validateEmail(email)) {
                generatedOtp = generateOtp();
                otpGeneratedTime = System.currentTimeMillis();
                sendOtpEmail(email, generatedOtp);
            }
        });

        verifyOtpBtn.setOnClickListener(v -> {
            String otpEntered = otpInput.getText().toString().trim();
            verifyOtp(otpEntered);
        });
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            emailInput.setError("Enter your email");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Invalid email format");
            return false;
        }
        return true;
    }

    private String generateOtp() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private void sendOtpEmail(String email, String otp) {
        new Thread(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication("YOUR_EMAIL@gmail.com", "YOUR_APP_PASSWORD");
                        }
                    });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("YOUR_EMAIL@gmail.com"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject("Password Reset OTP");
                message.setText("Your OTP is: " + otp);

                Transport.send(message);
                runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show());
            } catch (MessagingException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void verifyOtp(String enteredOtp) {
        // Check if OTP is empty
        if (enteredOtp.isEmpty()) {
            otpInput.setError("Enter OTP");
            return;
        }

        // Check OTP expiration (10 minutes)
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - otpGeneratedTime;

        if (timeDifference > 10 * 60 * 1000) { // 10 minutes in milliseconds
            Toast.makeText(this, "OTP has expired. Request a new one.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify OTP
        if (enteredOtp.equals(generatedOtp)) {
            // OTP is correct, proceed to reset password
            navigateToResetPasswordScreen();
        } else {
            otpInput.setError("Incorrect OTP");
        }
    }

    private void navigateToResetPasswordScreen() {
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        startActivity(intent);
        finish();
    }
}