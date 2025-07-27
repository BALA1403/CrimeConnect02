package com.example.crimereportapp;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ForumRepository {
    private ForumPostDao postDao;
    private ForumCommentDao commentDao;

    public ForumRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        postDao = database.forumPostDao();
        commentDao = database.forumCommentDao();
    }

    public LiveData<List<ForumPost>> getAllPosts() {
        return postDao.getAllPosts();
    }

    public LiveData<ForumPost> getPostById(long postId) {
        return postDao.getPostById(postId);
    }

    public LiveData<List<ForumComment>> getCommentsForPost(long postId) {
        return commentDao.getCommentsForPost(String.valueOf(postId));
    }

    public long insertPost(ForumPost post) {
        return postDao.insert(post);
    }

    public void updatePost(ForumPost post) {
        postDao.update(post);
    }

    public void deletePost(ForumPost post) {
        postDao.delete(post);
    }

    public void incrementLikes(long postId) {
        postDao.incrementLikes(postId);
    }

    public void decrementLikes(long postId) {
        postDao.decrementLikes(postId);
    }

    public long insertComment(ForumComment comment) {
        return commentDao.insert(comment);
    }
}