package com.example.crimereportapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Arrays;

public class SOSActivity extends AppCompatActivity {

    private static final String TAG = "SOSActivity";
    private static final int REQUEST_PERMISSIONS_CODE = 1001;
    private static final int REQUEST_LOCATION_SETTINGS = 1003;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;
    private String[] emergencyContacts;
    private LocationCallback locationCallback;
    private Handler timeoutHandler;
    private Button btnSendSOS;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        setContentView(R.layout.activity_sos);

        db = AppDatabase.getInstance(this);
        btnSendSOS = findViewById(R.id.btnSendSOS);
        if (btnSendSOS == null) {
            Log.e(TAG, "btnSendSOS not found in layout");
            Toast.makeText(this, "UI error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sharedPreferences = getSharedPreferences("EmergencyContacts", MODE_PRIVATE);
        emergencyContacts = getSavedContacts();
        Log.d(TAG, "Emergency contacts loaded: " + Arrays.toString(emergencyContacts));

        if (emergencyContacts.length < 3 || emergencyContacts[0].isEmpty()) {
            Log.w(TAG, "Insufficient emergency contacts");
            Toast.makeText(this, "Please set 3 emergency contacts first!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, EmergencyContactsActivity.class));
            finish();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnSendSOS.setOnClickListener(v -> {
            Log.d(TAG, "SOS button clicked");
            if (isLocationEnabled()) {
                checkPermissions();
            } else {
                Log.w(TAG, "Location services disabled—prompting user");
                Toast.makeText(this, "Please enable location services!", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION_SETTINGS);
            }
        });
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, "GPS enabled: " + gpsEnabled + ", Network enabled: " + networkEnabled);
        return gpsEnabled || networkEnabled;
    }

    private void checkPermissions() {
        Log.d(TAG, "Checking permissions");
        String[] permissionsNeeded = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS
        };
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
                Log.d(TAG, "Permission needed: " + permission);
            } else {
                Log.d(TAG, "Permission already granted: " + permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_CODE);
        } else {
            Log.d(TAG, "All permissions granted—proceeding to get location");
            getLiveLocation();
        }
    }

    private void getLiveLocation() {
        Log.d(TAG, "Attempting to get live location");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission missing after check—aborting");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000);

        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutHandler.postDelayed(() -> {
            if (locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
            Log.w(TAG, "Location request timed out after 60s");
            Toast.makeText(SOSActivity.this, "Location timeout! Using last known location", Toast.LENGTH_SHORT).show();
            getLastKnownLocation();
        }, 60000); // Extended to 60s

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Log.d(TAG, "Location result received");
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "Live location: " + location.getLatitude() + "," + location.getLongitude());
                    timeoutHandler.removeCallbacksAndMessages(null);
                    fusedLocationClient.removeLocationUpdates(this);
                    sendSOSMessage(location);
                } else {
                    Log.w(TAG, "Location result returned null");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updates started"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Location updates failed: " + e.getMessage(), e);
                    Toast.makeText(SOSActivity.this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    getLastKnownLocation();
                });
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "Attempting to get last known location");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission missing for last known—aborting");
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d(TAG, "Last known location: " + location.getLatitude() + "," + location.getLongitude());
                        sendSOSMessage(location);
                    } else {
                        Log.w(TAG, "Last known location unavailable");
                        Toast.makeText(SOSActivity.this, "Unable to get last known location! Please enable location.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get last location: " + e.getMessage(), e);
                    Toast.makeText(SOSActivity.this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private boolean hasValidSimCard() {
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        boolean valid = simState == TelephonyManager.SIM_STATE_READY;
        Log.d(TAG, "SIM state: " + simState + " (" + (valid ? "Valid" : "Invalid") + ")");
        return valid;
    }

    private void sendSOSMessage(Location location) {
        Log.d(TAG, "sendSOSMessage called with location: " + location.getLatitude() + "," + location.getLongitude());
        if (!hasValidSimCard()) {
            Log.w(TAG, "No valid SIM card—cannot send SMS");
            Toast.makeText(this, "No valid SIM card found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String locationLink = "https://www.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();
        String locationString = location.getLatitude() + "," + location.getLongitude();
        String message = "EMERGENCY! I need help! My current location: " + locationLink;

        Log.d(TAG, "SOS message prepared: " + message);
        saveAlertToDatabase(locationString);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "SMS permission not granted after request—aborting");
            Toast.makeText(this, "SMS permission required!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean allMessagesSent = true;
        SmsManager smsManager = SmsManager.getDefault();
        for (String contact : emergencyContacts) {
            if (contact != null && !contact.trim().isEmpty()) {
                Log.d(TAG, "Sending SMS to: " + contact);
                try {
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    if (parts.size() > 1) {
                        smsManager.sendMultipartTextMessage(contact, null, parts, null, null);
                        Log.d(TAG, "Multipart SMS sent to " + contact + " (" + parts.size() + " parts)");
                    } else {
                        smsManager.sendTextMessage(contact, null, message, null, null);
                        Log.d(TAG, "Single SMS sent to " + contact);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send SMS to " + contact + ": " + e.getMessage(), e);
                    allMessagesSent = false;
                }
            } else {
                Log.w(TAG, "Skipping empty/invalid contact");
            }
        }

        Log.d(TAG, "SMS sending completed: " + (allMessagesSent ? "All sent" : "Some failed"));
        Toast.makeText(this, allMessagesSent ? "SOS messages sent successfully!" : "Some messages failed to send", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveAlertToDatabase(String location) {
        Log.d(TAG, "Saving SOS alert to database");
        new Thread(() -> {
            SOSAlert alert = new SOSAlert();
            alert.setContacts(Arrays.toString(emergencyContacts));
            alert.setLocation(location);
            alert.setTimestamp(System.currentTimeMillis());
            long alertId = db.sosAlertDao().insert(alert);
            Log.d(TAG, "SOS Alert saved with ID: " + (alertId != -1 ? alertId : "Failed"));
        }).start();
    }

    private String[] getSavedContacts() {
        String[] contacts = new String[]{
                sharedPreferences.getString("contact1", ""),
                sharedPreferences.getString("contact2", ""),
                sharedPreferences.getString("contact3", "")
        };
        for (int i = 0; i < contacts.length; i++) {
            if (contacts[i] != null && !contacts[i].trim().isEmpty()) {
                String cleaned = contacts[i].trim().replaceAll("[^0-9+]", "");
                if (!cleaned.startsWith("+91") && cleaned.length() == 10) {
                    cleaned = "+91" + cleaned;
                }
                contacts[i] = cleaned;
                Log.d(TAG, "Contact " + (i + 1) + ": " + contacts[i]);
            } else {
                contacts[i] = "";
            }
        }
        return contacts;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Permission denied: " + permissions[i]);
                    allGranted = false;
                }
            }
            if (allGranted) {
                Log.d(TAG, "All permissions granted—proceeding");
                getLiveLocation();
            } else {
                Toast.makeText(this, "Required permissions denied!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION_SETTINGS) {
            if (isLocationEnabled()) {
                Log.d(TAG, "Location enabled by user—retrying");
                checkPermissions();
            } else {
                Log.w(TAG, "User didn’t enable location—aborting");
                Toast.makeText(this, "Location services required!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Log.d(TAG, "Activity destroyed");
    }
}