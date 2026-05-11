package com.example.frontend.data.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Group;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.Result;

import java.io.File;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupRepository {

    private final ApiService apiService;

    public GroupRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    // Tạo nhóm mới — avatarFile có thể null nếu người dùng không chọn ảnh
    public void createGroup(String groupName, String privacy,
                            File avatarFile,
                            MutableLiveData<Result<Group>> resultLiveData) {
        resultLiveData.postValue(Result.loading());

        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), groupName);
        RequestBody descBody = RequestBody.create(MediaType.parse("text/plain"), "");
        RequestBody privacyBody = RequestBody.create(MediaType.parse("text/plain"), privacy);

        MultipartBody.Part avatarPart = null;
        if (avatarFile != null && avatarFile.exists()) {
            RequestBody fileBody = RequestBody.create(
                    MediaType.parse("image/*"), avatarFile);
            avatarPart = MultipartBody.Part.createFormData("images", avatarFile.getName(), fileBody);
        }

        apiService.createGroup(nameBody, descBody, privacyBody, avatarPart)
                .enqueue(new Callback<ApiResponse<Group>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Group>> call,
                                           Response<ApiResponse<Group>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            resultLiveData.postValue(Result.success(response.body().getData()));
                        } else {
                            String msg = (response.body() != null && response.body().getMessage() != null)
                                    ? response.body().getMessage()
                                    : "Tạo nhóm thất bại";
                            resultLiveData.postValue(Result.error(msg));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Group>> call, Throwable t) {
                        resultLiveData.postValue(Result.error(t.getMessage()));
                    }
                });
    }

    // Tab "Nhóm của bạn"
    public void getMyGroups(MutableLiveData<Result<List<Group>>> resultLiveData) {
        resultLiveData.postValue(Result.loading());

        apiService.getMyGroups().enqueue(new Callback<ApiResponse<List<Group>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Group>>> call,
                                   Response<ApiResponse<List<Group>>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {
                    resultLiveData.postValue(Result.success(response.body().getData()));
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage()
                            : "Không tải được danh sách nhóm";
                    resultLiveData.postValue(Result.error(msg, null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Group>>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }
}
