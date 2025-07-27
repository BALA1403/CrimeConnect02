package com.example.crimereportapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class CrimeReportDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CrimeReportApp.db";
    private static final int DATABASE_VERSION = 1;

    // Singleton instance
    private static CrimeReportDBHelper instance;

    // Singleton pattern implementation
    public static synchronized CrimeReportDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CrimeReportDBHelper(context.getApplicationContext());
        }
        return instance;
    }

    // User Table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_EMAIL = "email";

    // Crime Reports Table
    private static final String TABLE_CRIME_REPORTS = "crime_reports";
    private static final String COLUMN_REPORT_ID = "id";
    private static final String COLUMN_REPORT_TITLE = "title";
    private static final String COLUMN_REPORT_DESC = "description";
    private static final String COLUMN_REPORT_LOCATION = "location";
    private static final String COLUMN_REPORT_IMAGE = "image_path";
    private static final String COLUMN_REPORT_VIDEO = "video_path";
    private static final String COLUMN_REPORT_AUDIO = "audio_path";
    private static final String COLUMN_REPORT_AI_ANALYSIS = "ai_analysis"; // AI Detection Result
    private static final String COLUMN_REPORT_TIMESTAMP = "timestamp";

    // Community Forum Table
    private static final String TABLE_COMMUNITY_FORUM = "community_forum";
    private static final String COLUMN_POST_ID = "id";
    private static final String COLUMN_POST_TITLE = "title";
    private static final String COLUMN_POST_DESC = "description";
    private static final String COLUMN_POST_AI_MODERATION = "ai_moderation"; // AI moderation
    private static final String COLUMN_POST_TIMESTAMP = "timestamp";

    // SOS Alerts Table
    private static final String TABLE_SOS_ALERTS = "sos_alerts";
    private static final String COLUMN_ALERT_ID = "id";
    private static final String COLUMN_ALERT_CONTACTS = "contacts"; // Store as JSON or CSV
    private static final String COLUMN_ALERT_LOCATION = "location";
    private static final String COLUMN_ALERT_TIMESTAMP = "timestamp";

    public CrimeReportDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_USER_ID, COLUMN_USERNAME, COLUMN_EMAIL},
                COLUMN_USERNAME + "=?",
                new String[]{username},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
            cursor.close();
        }
        return user;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_EMAIL + " TEXT UNIQUE)";

        String createCrimeReportsTable = "CREATE TABLE " + TABLE_CRIME_REPORTS + " (" +
                COLUMN_REPORT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_REPORT_TITLE + " TEXT, " +
                COLUMN_REPORT_DESC + " TEXT, " +
                COLUMN_REPORT_LOCATION + " TEXT, " +
                COLUMN_REPORT_IMAGE + " TEXT, " +
                COLUMN_REPORT_VIDEO + " TEXT, " +
                COLUMN_REPORT_AUDIO + " TEXT, " +
                COLUMN_REPORT_AI_ANALYSIS + " TEXT, " +
                COLUMN_REPORT_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String createSOSAlertsTable = "CREATE TABLE " + TABLE_SOS_ALERTS + " (" +
                COLUMN_ALERT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ALERT_CONTACTS + " TEXT, " +
                COLUMN_ALERT_LOCATION + " TEXT, " +
                COLUMN_ALERT_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        db.execSQL(createUsersTable);
        db.execSQL(createCrimeReportsTable);
        db.execSQL(createSOSAlertsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CRIME_REPORTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SOS_ALERTS);
        onCreate(db);
    }

    public List<CrimeReport> getAllReports() {
        List<CrimeReport> reportList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_CRIME_REPORTS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_REPORT_TIMESTAMP + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                CrimeReport report = new CrimeReport();
                report.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REPORT_ID)));
                report.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_TITLE)));
                report.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_DESC)));
                report.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_LOCATION)));
                report.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_IMAGE)));
                report.setVideoPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_VIDEO)));
                report.setAudioPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_AUDIO)));
                report.setAiAnalysis(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_AI_ANALYSIS)));
                report.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_TIMESTAMP)));

                reportList.add(report);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return reportList;
    }

    // Insert User (Signup)
    public boolean insertUser(String username, String password, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_EMAIL, email);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Validate User (Login)
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS +
                        " WHERE " + COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Insert Crime Report
    public boolean insertCrimeReport(String title, String description, String location,
                                     String imagePath, String videoPath, String audioPath, String aiAnalysis) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REPORT_TITLE, title);
        values.put(COLUMN_REPORT_DESC, description);
        values.put(COLUMN_REPORT_LOCATION, location);
        values.put(COLUMN_REPORT_IMAGE, imagePath);
        values.put(COLUMN_REPORT_VIDEO, videoPath);
        values.put(COLUMN_REPORT_AUDIO, audioPath);
        values.put(COLUMN_REPORT_AI_ANALYSIS, aiAnalysis);

        long result = db.insert(TABLE_CRIME_REPORTS, null, values);
        return result != -1;
    }

    // Insert Community Forum Post
    public boolean insertForumPost(String title, String description, String aiModeration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_POST_TITLE, title);
        values.put(COLUMN_POST_DESC, description);
        values.put(COLUMN_POST_AI_MODERATION, aiModeration);

        long result = db.insert(TABLE_COMMUNITY_FORUM, null, values);
        return result != -1;
    }

    // Insert SOS Alert
    public boolean insertSOSAlert(String contacts, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ALERT_CONTACTS, contacts);
        values.put(COLUMN_ALERT_LOCATION, location);

        long result = db.insert(TABLE_SOS_ALERTS, null, values);
        return result != -1;
    }
}