package com.example.frontend.data.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Group;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.Result;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupRepository {

    private final ApiService apiService;

    public GroupRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
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
