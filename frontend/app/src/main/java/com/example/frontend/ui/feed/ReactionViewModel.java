package com.example.frontend.ui.feed;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.ReactionItem;
import com.example.frontend.data.repository.PostRepository;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReactionViewModel extends ViewModel {
    private PostRepository repository;
    private final MutableLiveData<List<ReactionItem>> reactionList = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public void init(Context context) {
        if (repository == null) {
            repository = new PostRepository(context);
        }
    }

    public LiveData<List<ReactionItem>> getReactionList() { return reactionList; }
    public LiveData<String> getError() { return error; }

    public void fetchReactions(String targetId) {
        repository.getReactionsOfPost(targetId, new Callback<ApiResponse<List<ReactionItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ReactionItem>>> call, Response<ApiResponse<List<ReactionItem>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reactionList.setValue(response.body().getData());
                } else {
                    error.setValue("Không thể tải danh sách");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<ReactionItem>>> call, Throwable t) {
                error.setValue(t.getMessage());
            }
        });
    }
}