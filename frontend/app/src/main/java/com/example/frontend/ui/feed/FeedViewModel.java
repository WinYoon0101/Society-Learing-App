package com.example.frontend.ui.feed;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.repository.PostRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedViewModel extends ViewModel {
    private final MutableLiveData<List<Post>> posts = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private PostRepository repository;

    public LiveData<List<Post>> getPosts() { return posts; }
    public LiveData<String> getError() { return error; }

    public void init(Context context) {
        if (repository == null) repository = new PostRepository(context);
        loadPosts();
    }

    public void loadPosts() {
        repository.fetchAllPosts(new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Post>>> call, Response<ApiResponse<List<Post>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    posts.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {
                error.setValue(t.getMessage());
            }
        });
    }
}
