package com.example.frontend.data.repository;

import android.content.Context;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.ReactionRequest; // Import model ReactionRequest
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Callback;

public class PostRepository {
    private final ApiService apiService;

    public PostRepository(Context context) {
        // Truyền context vào theo đúng yêu cầu của ApiClient bạn đang dùng
        this.apiService = ApiClient.getApiService(context);
    }

    // 1. Lấy danh sách bài viết
    public void fetchAllPosts(Callback<ApiResponse<List<Post>>> callback) {
        apiService.getAllPosts().enqueue(callback);
    }

    // 2. Đăng bài viết mới
    public void uploadPost(RequestBody content, MultipartBody.Part image, Callback<ApiResponse<Post>> callback) {
        apiService.createPost(content, image).enqueue(callback);
    }

    // Reaction
    public void toggleReaction(String targetId, String targetType, String type, Callback<ResponseBody> callback) {
        ReactionRequest request = new ReactionRequest(targetId, targetType, type);
        apiService.toggleReaction(request).enqueue(callback);
    }
}