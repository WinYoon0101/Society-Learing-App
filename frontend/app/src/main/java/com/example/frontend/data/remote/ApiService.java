package com.example.frontend.data.remote;

import com.example.frontend.data.model.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/auth/login") // Endpoint đăng nhập
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/auth/register")
    public Call<LoginResponse> register(@Body RegisterRequest request);
}
