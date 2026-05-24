package com.example.frontend.ui.feed;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.repository.PostRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavedViewModel extends AndroidViewModel {

    private PostRepository repository;
    private final MutableLiveData<List<Post>> savedPostsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>();

    public SavedViewModel(@NonNull Application application) {
        super(application);
        repository = new PostRepository(application.getApplicationContext());
    }

    public LiveData<List<Post>> getSavedPosts() {
        return savedPostsLiveData;
    }

    public LiveData<String> getMessage() {
        return messageLiveData;
    }

    // Lấy danh sách bài đã lưu
    public void fetchSavedPosts(String token) {
        repository.getSavedPosts(token, new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Post>>> call, Response<ApiResponse<List<Post>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    savedPostsLiveData.setValue(response.body().getData());
                } else {
                    savedPostsLiveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {
                messageLiveData.setValue("Lỗi mạng: Không tải được dữ liệu");
            }
        });
    }

    // Bỏ lưu bài viết (Toggle)
    public void unsavePost(String token, String postId) {
        repository.toggleSavePost(token, postId, new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    messageLiveData.setValue("Đã bỏ lưu bài viết");
                    fetchSavedPosts(token); // Load lại danh sách sau khi bỏ lưu
                } else {
                    messageLiveData.setValue("Có lỗi xảy ra, thử lại sau");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                messageLiveData.setValue("Lỗi mạng");
            }
        });
    }
}