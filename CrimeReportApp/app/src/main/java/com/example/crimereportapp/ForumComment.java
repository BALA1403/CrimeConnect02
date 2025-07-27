package com.example.crimereportapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "forum_comments")
public class ForumComment {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String userName;
    private String comment;
    private long timestamp;
    private String postId;

    public ForumComment(String userName, String comment, long timestamp, String postId) {
        this.userName = userName;
        this.comment = comment;
        this.timestamp = timestamp;
        this.postId = postId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
}