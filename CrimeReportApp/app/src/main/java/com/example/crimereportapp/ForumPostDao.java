package com.example.crimereportapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ForumPostDao {
    @Insert
    long insert(ForumPost post);

    @Update
    void update(ForumPost post);

    @Delete
    void delete(ForumPost post);

    @Query("SELECT * FROM forum_posts ORDER BY timestamp DESC")
    LiveData<List<ForumPost>> getAllPosts();

    @Query("SELECT * FROM forum_posts WHERE id = :postId")
    LiveData<ForumPost> getPostById(long postId);

    @Query("UPDATE forum_posts SET likes = likes + 1 WHERE id = :postId")
    void incrementLikes(long postId);

    @Query("UPDATE forum_posts SET likes = likes - 1 WHERE id = :postId")
    void decrementLikes(long postId);
}