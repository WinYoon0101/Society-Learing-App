package com.example.frontend.data.repository;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.utils.Result;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRepository {
    private ApiService apiService;

    public FriendRepository(Context context) {
        // Sử dụng ApiClient  và truyền context
        apiService = ApiClient.getApiService(context);
    }

    // 1. Lấy danh sách bạn bè
    public void getFriends(MutableLiveData<Result<List<Friend>>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        apiService.getFriends().enqueue(new Callback<ApiResponse<List<Friend>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Friend>>> call, Response<ApiResponse<List<Friend>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.postValue(Result.success(response.body().getData()));
                } else {
                    resultLiveData.postValue(Result.error("Lỗi lấy danh sách bạn bè", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Friend>>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    // 2. Lấy danh sách lời mời kết bạn (Pending)
    public void getPendingRequests(MutableLiveData<Result<List<Friend>>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        apiService.getPendingRequests().enqueue(new Callback<ApiResponse<List<Friend>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Friend>>> call, Response<ApiResponse<List<Friend>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.postValue(Result.success(response.body().getData()));
                } else {
                    resultLiveData.postValue(Result.error("Lỗi lấy danh sách lời mời", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Friend>>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    // 3. Gửi lời mời kết bạn
    public void sendFriendRequest(String userId, MutableLiveData<Result<Object>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        apiService.sendFriendRequest(userId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    resultLiveData.postValue(Result.success(response.body()));
                } else {
                    resultLiveData.postValue(Result.error("Lỗi khi gửi lời mời", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    // 4. Chấp nhận lời mời
    public void acceptFriendRequest(String userId, MutableLiveData<Result<Object>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        apiService.acceptFriendRequest(userId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    resultLiveData.postValue(Result.success(response.body()));
                } else {
                    resultLiveData.postValue(Result.error("Lỗi khi chấp nhận lời mời", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    // 5. Từ chối lời mời
    public void declineFriendRequest(String userId, MutableLiveData<Result<Object>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        apiService.declineFriendRequest(userId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    resultLiveData.postValue(Result.success(response.body()));
                } else {
                    resultLiveData.postValue(Result.error("Lỗi khi từ chối lời mời", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    // 6. Huỷ kết bạn
    public void removeFriend(String userId, MutableLiveData<Result<Object>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        apiService.removeFriend(userId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    resultLiveData.postValue(Result.success(response.body()));
                } else {
                    resultLiveData.postValue(Result.error("Lỗi khi huỷ kết bạn", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }


// Lấy danh sách Gợi ý kết bạn
public void getFriendSuggestions(MutableLiveData<Result<List<Friend>>> resultLiveData) {
    resultLiveData.postValue(Result.loading(null));

    apiService.getFriendSuggestions().enqueue(new Callback<ApiResponse<List<Friend>>>() {
        @Override
        public void onResponse(Call<ApiResponse<List<Friend>>> call, Response<ApiResponse<List<Friend>>> response) {
            if (response.isSuccessful() && response.body() != null) {
                resultLiveData.postValue(Result.success(response.body().getData()));
            } else {
                try {
                    String realError = "Lỗi không xác định";
                    if (response.errorBody() != null) {
                        realError = response.errorBody().string();
                    }
                    resultLiveData.postValue(Result.error("Lỗi server: " + realError, null));
                } catch (Exception e) {
                    resultLiveData.postValue(Result.error("Lỗi lấy danh sách gợi ý", null));
                }
            }
        }

        @Override
        public void onFailure(Call<ApiResponse<List<Friend>>> call, Throwable t) {
            resultLiveData.postValue(Result.error(t.getMessage(), null));
        }
    });
}
}