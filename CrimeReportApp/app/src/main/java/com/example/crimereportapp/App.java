package com.example.crimereportapp;

import android.app.Application;

import androidx.room.Room;

public class App extends Application {
    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(this, AppDatabase.class, "")
                .build();
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}