package com.example.frontend.data.repository;

import android.content.Context;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.ReactionItem;
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
    // Reaction
    public void toggleReaction(String targetId, String targetType, String type, Callback<ApiResponse<Object>> callback) {
        ReactionRequest request = new ReactionRequest(targetId, targetType, type);
        apiService.toggleReaction(request).enqueue(callback);
    }
    public void getReactionsOfPost(String targetId, retrofit2.Callback<ApiResponse<List<ReactionItem>>> callback) {
        apiService.getReactionsOfPost(targetId).enqueue(callback);
    }
    public void getMyPosts(Callback<ApiResponse<List<Post>>> callback) {
        apiService.getMyPosts().enqueue(callback);
    }
}