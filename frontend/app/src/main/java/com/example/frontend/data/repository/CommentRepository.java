package com.example.frontend.data.repository;

import android.content.Context;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Comment;
import com.example.frontend.data.model.CommentRequest;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;

import java.util.List;

import retrofit2.Call;

public class CommentRepository {
    private ApiService apiService;

    public CommentRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    public Call<ApiResponse<List<Comment>>> getCommentsByPost(String postId) {
        return apiService.getComments(postId);
    }

    public Call<ApiResponse<Comment>> createComment(String token, CommentRequest request) {
        return apiService.createComment(token, request);
    }
    public Call<ApiResponse<Object>> deleteComment(String token, String commentId) {
        return apiService.deleteComment(token, commentId);
    }
}