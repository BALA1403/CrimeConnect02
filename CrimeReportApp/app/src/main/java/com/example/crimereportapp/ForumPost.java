package com.example.crimereportapp;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "forum_posts")
public class ForumPost {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String userId;
    private String userName;
    private String message;
    private String mediaUrl;
    private long timestamp;
    private int likes;

    // Default constructor
    public ForumPost() {
    }

    // Room will use this constructor
    public ForumPost(String userId, String userName, String message, String mediaUrl, long timestamp, int likes) {
        this.userId = userId;
        this.userName = userName;
        this.message = message;
        this.mediaUrl = mediaUrl;
        this.timestamp = timestamp;
        this.likes = likes;
    }

    // Ignore this constructor for Room
    @Ignore
    public ForumPost(String userId, String userName, String message) {
        this.userId = userId;
        this.userName = userName;
        this.message = message;
    }

    // Getter and Setter for id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // Getter and Setter for userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Getter and Setter for userName
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    // Getter and Setter for message
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Getter and Setter for mediaUrl
    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    // Getter and Setter for timestamp
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Getter and Setter for likes
    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }
}