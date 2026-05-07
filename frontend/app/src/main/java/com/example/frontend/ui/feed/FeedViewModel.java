package com.example.frontend.ui.feed;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.repository.PostRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedViewModel extends ViewModel {
    private PostRepository repository;
    private final MutableLiveData<List<Post>> postsLiveData = new MutableLiveData<>();

    // ĐÃ THÊM: Biến theo dõi trạng thái xóa bài viết
    private final MutableLiveData<String> deleteStatus = new MutableLiveData<>();

    public void init(Context context) {
        if (repository == null) {
            repository = new PostRepository(context);
        }
    }

    public LiveData<List<Post>> getPosts() {
        return postsLiveData;
    }

    // ĐÃ THÊM: Getter cho trạng thái xóa
    public LiveData<String> getDeleteStatus() {
        return deleteStatus;
    }

    public void loadPosts() {
        if (repository != null) {
            repository.fetchAllPosts(new Callback<ApiResponse<List<Post>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Post>>> call, Response<ApiResponse<List<Post>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        postsLiveData.setValue(response.body().getData());
                    } else {
                        postsLiveData.setValue(null);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {
                    postsLiveData.setValue(null);
                }
            });
        }
    }

    // ==========================================================
    // HÀM MỚI BỔ SUNG: XỬ LÝ GỌI API XÓA BÀI VIẾT
    // ==========================================================
    public void deletePost(String token, String postId) {
        if (repository != null) {
            repository.deletePost(token, postId, new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        deleteStatus.setValue("SUCCESS");
                        loadPosts(); // Tự động load lại bảng tin sau khi xóa thành công
                    } else {
                        deleteStatus.setValue("Lỗi khi xóa bài viết: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    deleteStatus.setValue("Lỗi mạng, không thể xóa: " + t.getMessage());
                }
            });
        }
    }

    // ==========================================================
    // HÀM XỬ LÝ GỌI API THẢ CẢM XÚC LÊN SERVER
    // ==========================================================
    public void toggleReaction(String targetId, String targetType, String type) {
        if (repository != null) {
            repository.toggleReaction(targetId, targetType, type, new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        Log.d("DEBUG_REACT", "✅ Gửi Cảm xúc lên Server THÀNH CÔNG!");
                    } else {
                        Log.e("DEBUG_REACT", "❌ Server báo lỗi khi thả Cảm xúc: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    Log.e("DEBUG_REACT", "❌ Lỗi mạng, không thể kết nối tới Server: " + t.getMessage());
                }
            });
        }
    }
}