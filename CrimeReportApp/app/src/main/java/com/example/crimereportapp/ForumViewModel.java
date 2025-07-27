package com.example.crimereportapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForumViewModel extends AndroidViewModel {
    private ForumRepository repository;
    private ExecutorService executorService;

    public ForumViewModel(@NonNull Application application) {
        super(application);
        repository = new ForumRepository(application);
        executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<ForumPost>> getAllPosts() {
        return repository.getAllPosts();
    }

    public LiveData<ForumPost> getPostById(long postId) {
        return repository.getPostById(postId);
    }

    public LiveData<List<ForumComment>> getCommentsForPost(long postId) {
        return repository.getCommentsForPost(postId);
    }

    public LiveData<Long> insertComment(ForumComment comment) {
        MutableLiveData<Long> result = new MutableLiveData<>();
        executorService.execute(() -> {
            long id = repository.insertComment(comment);
            result.postValue(id > 0 ? id : null); // Handle insertion failure
        });
        return result;
    }

    public LiveData<Long> insertPost(ForumPost post) {
        MutableLiveData<Long> result = new MutableLiveData<>();
        executorService.execute(() -> {
            long id = repository.insertPost(post);
            result.postValue(id > 0 ? id : null); // Handle insertion failure
        });
        return result;
    }

    public void deletePost(ForumPost post) {
        executorService.execute(() -> repository.deletePost(post));
    }

    public void incrementLikes(long postId) {
        executorService.execute(() -> repository.incrementLikes(postId));
    }

    public void decrementLikes(long postId) {
        executorService.execute(() -> repository.decrementLikes(postId));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}