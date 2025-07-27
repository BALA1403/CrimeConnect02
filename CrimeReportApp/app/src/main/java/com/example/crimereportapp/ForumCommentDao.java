package com.example.crimereportapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ForumCommentDao {

    @Insert
    long insert(ForumComment comment);

    @Update
    void update(ForumComment comment);

    @Delete
    void delete(ForumComment comment);

    @Query("SELECT * FROM forum_comments WHERE postId = :postId")
    LiveData<List<ForumComment>> getCommentsForPost(String postId);

    @Query("SELECT COUNT(*) FROM forum_comments WHERE postId = :postId")
    LiveData<Integer> getCommentCount(String postId);
}