package com.example.frontend.data.repository;

import android.content.Context;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.LiveModel;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.LiveRequest;

import java.util.List;

import retrofit2.Callback;

public class LiveRepository {
    private ApiService apiService;

    public LiveRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public void fetchProfile(Callback<ApiResponse<User>> callback) {
        apiService.getMyProfile().enqueue(callback);
    }

    public void getActiveLives(Callback<ApiResponse<List<LiveModel>>> callback) {
        apiService.getActiveLives().enqueue(callback);
    }

    public void startLive(LiveRequest request, Callback<ApiResponse<LiveModel>> callback) {
        apiService.startLive(request).enqueue(callback);
    }

    public void endLive(String liveId, Callback<ApiResponse<Void>> callback) {
        apiService.endLive(liveId).enqueue(callback);
    }
}
