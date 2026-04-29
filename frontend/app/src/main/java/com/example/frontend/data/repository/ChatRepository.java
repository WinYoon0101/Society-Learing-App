package com.example.frontend.data.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.Message;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class
ChatRepository {
    private final ApiService apiService;

    public ChatRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    public void getConversations(MutableLiveData<Result<List<Conversation>>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        apiService.getConversations().enqueue(new Callback<ApiResponse<List<Conversation>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Conversation>>> call, Response<ApiResponse<List<Conversation>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.postValue(Result.success(response.body().getData()));
                } else {
                    resultLiveData.postValue(Result.error("Không tải được danh sách chat", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Conversation>>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    public void getOrCreateConversation(String targetUserId, MutableLiveData<Result<Conversation>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        Map<String, String> body = new HashMap<>();
        body.put("targetUserId", targetUserId);

        apiService.getOrCreateConversation(body).enqueue(new Callback<ApiResponse<Conversation>>() {
            @Override
            public void onResponse(Call<ApiResponse<Conversation>> call, Response<ApiResponse<Conversation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.postValue(Result.success(response.body().getData()));
                } else {
                    resultLiveData.postValue(Result.error("Không mở được cuộc trò chuyện", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Conversation>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    public void getMessages(String conversationId, MutableLiveData<Result<List<Message>>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        apiService.getMessages(conversationId).enqueue(new Callback<ApiResponse<List<Message>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Message>>> call, Response<ApiResponse<List<Message>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.postValue(Result.success(response.body().getData()));
                } else {
                    resultLiveData.postValue(Result.error("Không tải được tin nhắn", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Message>>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    public void sendMessage(String conversationId, String text, MutableLiveData<Result<Message>> resultLiveData) {
        resultLiveData.postValue(Result.loading(null));

        Map<String, String> body = new HashMap<>();
        body.put("conversationId", conversationId);
        body.put("text", text);

        apiService.sendMessage(body).enqueue(new Callback<ApiResponse<Message>>() {
            @Override
            public void onResponse(Call<ApiResponse<Message>> call, Response<ApiResponse<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.postValue(Result.success(response.body().getData()));
                } else {
                    resultLiveData.postValue(Result.error("Không gửi được tin nhắn", null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Message>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }
}
