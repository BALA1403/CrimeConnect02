package com.example.crimereportapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ForumPost.class, ForumComment.class, SOSAlert.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ForumPostDao forumPostDao();
    public abstract ForumCommentDao forumCommentDao();
    public abstract SOSAlertDao sosAlertDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "crime_connect_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}