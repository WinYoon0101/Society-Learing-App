package com.example.frontend.data.repository;

import android.content.Context;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Quiz;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.QuizRequest;

import retrofit2.Callback;

public class QuizRepository {
    private ApiService apiService;

    public QuizRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public void generateAIQuiz(String text, int num, String title, Callback<ApiResponse<Quiz>> callback) {
        QuizRequest request = new QuizRequest(text, num, title);
        apiService.generateQuiz(request).enqueue(callback);
    }
}
