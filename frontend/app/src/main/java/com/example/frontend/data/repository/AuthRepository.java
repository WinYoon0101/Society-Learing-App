package com.example.frontend.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.LoginRequest;
import com.example.frontend.data.remote.RegisterRequest;
import com.example.frontend.utils.Result;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private ApiService apiService;

    public AuthRepository() {
        apiService = ApiClient.getApiService();
    }

    public LiveData<Result<LoginResponse>> login(String email, String password) {
        MutableLiveData<Result<LoginResponse>> loginResult = new MutableLiveData<>();

        loginResult.setValue(Result.loading(null));

        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    ApiResponse<LoginResponse> body = response.body();

                    if (body.success) {
                        loginResult.setValue(Result.success(body.data));
                    } else {
                        loginResult.setValue(Result.error(body.message, null));
                    }

                } else {
                    try {
                        String error = response.errorBody().string();
                        loginResult.setValue(Result.error(error, null));
                    } catch (Exception e) {
                        loginResult.setValue(Result.error("Unknown error", null));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                loginResult.setValue(Result.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });

        return loginResult;
    }

    public LiveData<Result<LoginResponse>> register(String username, String email, String password, String dateOfBirth, String gender) {
        MutableLiveData<Result<LoginResponse>> registerResult = new MutableLiveData<>();

        registerResult.setValue(Result.loading(null));

        RegisterRequest request = new RegisterRequest(username, email, password, dateOfBirth, gender);

        apiService.register(request).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    ApiResponse<LoginResponse> body = response.body();

                    if (body.success) {
                        registerResult.setValue(Result.success(body.data));
                    } else {
                        registerResult.setValue(Result.error(body.message, null));
                    }

                } else {
                    try {
                        String error = response.errorBody().string();
                        registerResult.setValue(Result.error(error, null));
                    } catch (Exception e) {
                        registerResult.setValue(Result.error("Unknown error", null));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                registerResult.setValue(Result.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });

        return registerResult;
    }
}