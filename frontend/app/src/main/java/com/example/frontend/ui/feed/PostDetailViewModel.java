package com.example.frontend.ui.feed;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Comment;
import com.example.frontend.data.model.CommentRequest;
import com.example.frontend.data.repository.CommentRepository;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailViewModel extends AndroidViewModel {

    private CommentRepository repository;

    // --- CÁC LIVEDATA ĐỂ ACTIVITY "THEO DÕI" (OBSERVE) ---
    private MutableLiveData<List<Comment>> commentsLiveData = new MutableLiveData<>();
    private MutableLiveData<String> messageLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> actionSuccessLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    // THÊM DÒNG NÀY: LiveData để theo dõi số lượng bình luận
    private MutableLiveData<Integer> commentCountLiveData = new MutableLiveData<>();

    public PostDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new CommentRepository(application);
    }

    // Các hàm Getter cho Activity theo dõi
    public LiveData<List<Comment>> getCommentsLiveData() { return commentsLiveData; }
    public LiveData<String> getMessageLiveData() { return messageLiveData; }
    public LiveData<Boolean> getActionSuccessLiveData() { return actionSuccessLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    // THÊM DÒNG NÀY: Getter cho số lượng bình luận
    public LiveData<Integer> getCommentCountLiveData() { return commentCountLiveData; }

    // ==========================================
    // LOGIC 1: LẤY VÀ ÉP DẸP DANH SÁCH BÌNH LUẬN
    // ==========================================
    public void fetchComments(String postId) {
        isLoading.setValue(true);
        repository.getCommentsByPost(postId).enqueue(new Callback<ApiResponse<List<Comment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Comment>>> call, Response<ApiResponse<List<Comment>>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> rootComments = response.body().getData();

                    // Logic xử lý dữ liệu nằm ở đây, View không cần biết!
                    List<Comment> flatList = new ArrayList<>();
                    flattenComments(rootComments, flatList);

                    // Đẩy danh sách đã xử lý lên LiveData
                    commentsLiveData.setValue(flatList);

                    // THÊM DÒNG NÀY: Cập nhật tổng số bình luận sau khi đã trải phẳng
                    commentCountLiveData.setValue(flatList.size());
                } else {
                    messageLiveData.setValue("Không thể tải bình luận");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Comment>>> call, Throwable t) {
                isLoading.setValue(false);
                messageLiveData.setValue("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // Thuật toán đệ quy (Đã giấu kín trong ViewModel)
    private void flattenComments(List<Comment> treeList, List<Comment> flatList) {
        if (treeList == null) return;
        for (Comment comment : treeList) {
            flatList.add(comment);
            if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
                flattenComments(comment.getReplies(), flatList);
            }
        }
    }

    // ==========================================
    // LOGIC 2: ĐĂNG BÌNH LUẬN (GỐC HOẶC TRẢ LỜI)
    // ==========================================
    public void postComment(String token, String postId, String content, String parentId) {
        CommentRequest request = new CommentRequest(postId, content, parentId);

        repository.createComment(token, request).enqueue(new Callback<ApiResponse<Comment>>() {
            @Override
            public void onResponse(Call<ApiResponse<Comment>> call, Response<ApiResponse<Comment>> response) {
                if (response.isSuccessful()) {
                    actionSuccessLiveData.setValue(true); // Báo hiệu đăng thành công để View xóa ô nhập
                    fetchComments(postId); // Lấy lại danh sách mới
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        android.util.Log.e("API_LỖI", "Mã lỗi: " + response.code() + " - Chi tiết: " + errorBody);
                    } catch (Exception e) {}
                    messageLiveData.setValue("Lỗi đăng bình luận");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Comment>> call, Throwable t) {
                messageLiveData.setValue("Lỗi mạng");
            }
        });
    }

    // ==========================================
    // LOGIC 3: XÓA BÌNH LUẬN
    // ==========================================
    public void deleteComment(String token, String postId, String commentId) {
        repository.deleteComment(token, commentId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    messageLiveData.setValue("Đã xóa bình luận");
                    fetchComments(postId); // Trải nghiệm an toàn nhất là lấy lại list từ server
                } else {
                    messageLiveData.setValue("Bạn không có quyền xóa");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                messageLiveData.setValue("Lỗi mạng");
            }
        });
    }
}