package com.example.crimereportapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserManager {
    private static final String PREF_NAME = "user_preferences";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    private static UserManager instance;
    private SharedPreferences preferences;
    private User currentUser;
    private CrimeReportDBHelper dbHelper;
    private ExecutorService executorService;

    private UserManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        dbHelper = new CrimeReportDBHelper(context.getApplicationContext());
        executorService = Executors.newSingleThreadExecutor();
        loadUserFromPreferences();
    }

    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadUserFromPreferences() {
        String userId = preferences.getString(KEY_USER_ID, null);
        if (userId != null) {
            currentUser = new User();
            currentUser.setId(userId);
            currentUser.setUsername(preferences.getString(KEY_USERNAME, "Anonymous"));
            currentUser.setEmail(preferences.getString(KEY_EMAIL, ""));
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public LiveData<Boolean> login(String username, String password) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        executorService.execute(() -> {
            try {
                // Check if credentials match
                boolean userExists = dbHelper.checkUser(username, password);
                if (userExists) {
                    // Fetch full user details
                    User user = dbHelper.getUserByUsername(username);
                    if (user != null) {
                        preferences.edit()
                                .putString(KEY_USER_ID, user.getId())
                                .putString(KEY_USERNAME, user.getUsername())
                                .putString(KEY_EMAIL, user.getEmail())
                                .apply();

                        currentUser = user;
                        result.postValue(true);
                    } else {
                        result.postValue(false);
                    }
                } else {
                    result.postValue(false);
                }
            } catch (Exception e) {
                result.postValue(false);
            }
        });

        return result;
    }
    public void logout() {
        preferences.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USERNAME)
                .remove(KEY_EMAIL)
                .apply();

        currentUser = null;
    }

    public LiveData<Boolean> registerUser(String username, String email, String password) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        executorService.execute(() -> {
            try {
                // Insert new user into the database
                boolean success = dbHelper.insertUser(username, password, email);
                if (success) {
                    // Save user details to preferences
                    String userId = "user_" + System.currentTimeMillis();
                    preferences.edit()
                            .putString(KEY_USER_ID, userId)
                            .putString(KEY_USERNAME, username)
                            .putString(KEY_EMAIL, email)
                            .apply();

                    currentUser = new User();
                    currentUser.setId(userId);
                    currentUser.setUsername(username);
                    currentUser.setEmail(email);

                    result.postValue(true);
                } else {
                    result.postValue(false);
                }
            } catch (Exception e) {
                result.postValue(false);
            }
        });

        return result;
    }
}