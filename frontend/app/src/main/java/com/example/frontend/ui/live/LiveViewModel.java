package com.example.frontend.ui.live;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.LiveModel;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.LiveRequest;
import com.example.frontend.data.repository.LiveRepository;
import com.example.frontend.utils.Result;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveViewModel extends ViewModel {
    private LiveRepository repository;

    // Quản lý danh sách Live bằng Result để hiện loading khi đang tải
    public MutableLiveData<Result<List<LiveModel>>> liveListResult = new MutableLiveData<>();
    public MutableLiveData<User> currentUser = new MutableLiveData<>();
    public MutableLiveData<Result<LiveModel>> createLiveResult = new MutableLiveData<>();

    public void init(Context context) {
        if (repository == null) repository = new LiveRepository(context);
    }

    public void loadData() {
        // Load danh sách phòng kèm trạng thái Loading
        liveListResult.postValue(Result.loading());

        repository.getActiveLives(new Callback<ApiResponse<List<LiveModel>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LiveModel>>> call, Response<ApiResponse<List<LiveModel>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveListResult.postValue(Result.success(response.body().getData()));
                } else {
                    liveListResult.postValue(Result.error("Không thể lấy danh sách Live"));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<LiveModel>>> call, Throwable t) {
                liveListResult.postValue(Result.error("Lỗi kết nối mạng"));
            }
        });

        // Load Profile (Vẫn giữ đơn giản vì thường load rất nhanh)
        repository.fetchProfile(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null)
                    currentUser.postValue(response.body().getData());
            }
            @Override public void onFailure(Call<ApiResponse<User>> call, Throwable t) {}
        });
    }


    public void createLive(String liveId, String title) {
        createLiveResult.postValue(Result.loading());
        repository.startLive(new LiveRequest(liveId, title), new Callback<ApiResponse<LiveModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<LiveModel>> call, Response<ApiResponse<LiveModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    createLiveResult.postValue(Result.success(response.body().getData()));
                } else {
                    // Lấy message lỗi thật từ server (ví dụ: Trùng ID phòng)
                    createLiveResult.postValue(Result.error("Server Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LiveModel>> call, Throwable t) {
                createLiveResult.postValue(Result.error("Mất kết nối: " + t.getMessage()));
            }
        });
    }
}