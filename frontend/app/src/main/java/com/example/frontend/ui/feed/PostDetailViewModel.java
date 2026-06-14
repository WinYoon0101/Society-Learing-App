package com.example.frontend.ui.feed;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Comment;
import com.example.frontend.data.model.CommentRequest;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.User;
import com.example.frontend.data.repository.CommentRepository;
import com.example.frontend.data.repository.PostRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailViewModel extends AndroidViewModel {

    private CommentRepository repository;
    private PostRepository postRepository;

    // --- CÁC LIVEDATA ĐỂ ACTIVITY "THEO DÕI" (OBSERVE) ---
    private MutableLiveData<List<Comment>> commentsLiveData = new MutableLiveData<>();
    private MutableLiveData<String> messageLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> actionSuccessLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Integer> commentCountLiveData = new MutableLiveData<>();
    private MutableLiveData<Post> postLiveData = new MutableLiveData<>();

    public PostDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new CommentRepository(application);
        postRepository = new PostRepository(application);
    }

    // Các hàm Getter cho Activity theo dõi
    public LiveData<List<Comment>> getCommentsLiveData() { return commentsLiveData; }
    public LiveData<String> getMessageLiveData() { return messageLiveData; }
    public LiveData<Boolean> getActionSuccessLiveData() { return actionSuccessLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Integer> getCommentCountLiveData() { return commentCountLiveData; }
    public LiveData<Post> getPostLiveData() { return postLiveData; }

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

                    List<Comment> flatList = new ArrayList<>();
                    flattenComments(rootComments, flatList);

                    commentsLiveData.setValue(flatList);
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
                if (response.isSuccessful() && response.body() != null) {
                    actionSuccessLiveData.setValue(true);

                    Comment created = response.body().getData();

                    if (created != null && (created.getUserId() == null || created.getUserId().getAvatar() == null)) {
                        Context ctx = getApplication().getApplicationContext();
                        String myId = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).getString("USER_ID", null);
                        String myName = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).getString("USERNAME", null);
                        String myAvatar = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).getString("USER_AVATAR", null);
                        if (created.getUserId() == null) {
                            created.setUserId(new User(myId, myName, myAvatar));
                        } else if (created.getUserId().getAvatar() == null) {
                            User u = created.getUserId();
                            User replaced = new User(u.getId(), u.getUsername() != null ? u.getUsername() : myName, myAvatar);
                            created.setUserId(replaced);
                        }
                    }

                    List<Comment> current = commentsLiveData.getValue();
                    if (current == null) current = new ArrayList<>();
                    if (created != null) current.add(created);
                    commentsLiveData.setValue(current);
                    commentCountLiveData.setValue(current.size());

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
                    fetchComments(postId);
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

    // ==========================================
    // LOGIC 4: LẤY CHI TIẾT BÀI VIẾT BẰNG ID (Dùng cho Click Thông Báo)
    // ==========================================
    public void fetchPostById(String postId) {
        // Thêm Log để theo dõi id bài viết được truyền vào
        android.util.Log.d("API_DEBUG", "Đang gọi API lấy bài viết ID: " + postId);

        postRepository.getPostById(postId).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("API_DEBUG", "Tải bài viết THÀNH CÔNG!");
                    postLiveData.setValue(response.body().getData());
                } else {
                    // BẮT LỖI CHI TIẾT TỪ BACKEND
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Không rõ";
                        android.util.Log.e("API_DEBUG", "Lỗi Backend trả về. Mã lỗi: " + response.code() + " - Chi tiết: " + errorBody);

                        // Hiển thị Toast mã lỗi lên màn hình điện thoại
                        messageLiveData.setValue("Lỗi Backend: " + response.code());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
                android.util.Log.e("API_DEBUG", "Lỗi mạng hoặc sập Server: " + t.getMessage());
                messageLiveData.setValue("Lỗi mạng khi tải bài viết");
            }
        });
    }
}