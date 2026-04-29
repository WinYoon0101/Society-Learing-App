package com.example.frontend.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.UpdateProfile;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.Result;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final ApiService apiService;

    public UserRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    //get profile
    public LiveData<Result<User>> getProfile() {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(Result.loading(null));

        apiService.getMyProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(Result.success(response.body().data));
                } else {
                    result.setValue(Result.error("Lỗi load profile", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                result.setValue(Result.error(t.getMessage(), null));
            }
        });

        return result;
    }

    //update profile
    public LiveData<Result<User>> updateProfile(UpdateProfile request) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();

        apiService.updateProfile(request).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(Result.success(response.body().data));
                } else {
                    result.setValue(Result.error("Update fail", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                result.setValue(Result.error(t.getMessage(), null));
            }
        });

        return result;
    }

    //upload avatar
    public LiveData<Result<String>> uploadAvatar(File file) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>();

        RequestBody requestFile =
                RequestBody.create(file, MediaType.parse("image/*"));

        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        apiService.uploadAvatar(body).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    result.setValue(Result.success("ok"));
                } else {
                    result.setValue(Result.error("fail", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                result.setValue(Result.error(t.getMessage(), null));
            }
        });

        return result;
    }
}