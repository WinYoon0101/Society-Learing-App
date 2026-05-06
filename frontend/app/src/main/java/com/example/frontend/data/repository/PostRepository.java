package com.example.frontend.data.repository;

import android.content.Context;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.ReactionItem;
import com.example.frontend.data.model.ReactionRequest;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Callback;

public class PostRepository {
    private final ApiService apiService;

    public PostRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    // 1. Lấy danh sách bài viết
    public void fetchAllPosts(Callback<ApiResponse<List<Post>>> callback) {
        apiService.getAllPosts().enqueue(callback);
    }

    // ==============================================================
    // 2. Đăng bài viết mới (Bổ sung đủ 4 tham số cho khớp với ApiService)
    // ==============================================================
    public void uploadPost(
            RequestBody content,
            RequestBody privacy,
            RequestBody groupId,
            List<MultipartBody.Part> images,
            Callback<ApiResponse<Post>> callback) {

        apiService.createPost(content, privacy, groupId, images).enqueue(callback);
    }

    // Reaction
    public void toggleReaction(String targetId, String targetType, String type, Callback<ApiResponse<Object>> callback) {
        ReactionRequest request = new ReactionRequest(targetId, targetType, type);
        apiService.toggleReaction(request).enqueue(callback);
    }

    public void getReactionsOfPost(String targetId, retrofit2.Callback<ApiResponse<List<ReactionItem>>> callback) {
        apiService.getReactionsOfPost(targetId).enqueue(callback);
    }

    public void deletePost(String token, String postId, Callback<ApiResponse<Object>> callback) {
        apiService.deletePost(token, postId).enqueue(callback);
    }

    public void getMyPosts(Callback<ApiResponse<List<Post>>> callback) {
        apiService.getMyPosts().enqueue(callback);
    }
}