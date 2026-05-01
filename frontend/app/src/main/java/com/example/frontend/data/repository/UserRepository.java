package com.example.frontend.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.UpdateProfile;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.AvatarResponse;
import com.example.frontend.data.model.CoverResponse;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.utils.Result;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private ApiService api;

    public UserRepository(Context context) {
        api = ApiClient.getApiService(context);
    }

    // ================= GET PROFILE =================
    public LiveData<Result<User>> getProfile() {
        MutableLiveData<Result<User>> liveData = new MutableLiveData<>();
        liveData.setValue(Result.loading());

        api.getMyProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(Result.success(response.body().getData()));
                } else {
                    liveData.postValue(Result.error("Get profile failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });

        return liveData;
    }

    // ================= UPLOAD AVATAR =================
    public LiveData<Result<String>> uploadAvatar(File file) {

        MutableLiveData<Result<String>> liveData = new MutableLiveData<>();
        liveData.setValue(Result.loading());

        RequestBody requestFile =
                RequestBody.create(MediaType.parse("image/*"), file);

        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        api.uploadAvatar(body).enqueue(new Callback<ApiResponse<AvatarResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvatarResponse>> call,
                                   Response<ApiResponse<AvatarResponse>> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                    String url = response.body().getData().getAvatar();
                    liveData.postValue(Result.success(url));

                } else {
                    liveData.postValue(Result.error("Upload avatar failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvatarResponse>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });

        return liveData;
    }

    // ================= UPLOAD COVER =================
    public LiveData<Result<String>> uploadCover(File file) {

        MutableLiveData<Result<String>> liveData = new MutableLiveData<>();
        liveData.setValue(Result.loading());

        RequestBody requestFile =
                RequestBody.create(MediaType.parse("image/*"), file);

        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        api.uploadCover(body).enqueue(new Callback<ApiResponse<CoverResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CoverResponse>> call,
                                   Response<ApiResponse<CoverResponse>> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                    String url = response.body().getData().getCover();
                    liveData.postValue(Result.success(url));

                } else {
                    liveData.postValue(Result.error("Upload cover failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CoverResponse>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });

        return liveData;
    }
    public LiveData<Result<User>> updateProfile(UpdateProfile request) {

        MutableLiveData<Result<User>> liveData = new MutableLiveData<>();
        liveData.setValue(Result.loading());

        api.updateProfile(request).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call,
                                   Response<ApiResponse<User>> response) {

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {

                    liveData.postValue(Result.success(response.body().getData()));

                } else {
                    liveData.postValue(Result.error("Update profile failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });

        return liveData;
    }
}