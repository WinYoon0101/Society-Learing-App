package com.example.frontend.data.repository;

import android.content.Context;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Callback;

public class PostRepository {
    private final ApiService apiService;

    public PostRepository(Context context) {
        // Truyền context vào theo đúng yêu cầu của ApiClient bạn đang dùng
        this.apiService = ApiClient.getApiService(context);
    }

    public void fetchAllPosts(Callback<ApiResponse<List<Post>>> callback) {
        apiService.getAllPosts().enqueue(callback);
    }

    public void uploadPost(RequestBody content, MultipartBody.Part image, Callback<ApiResponse<Post>> callback) {
        apiService.createPost(content, image).enqueue(callback);
    }
}