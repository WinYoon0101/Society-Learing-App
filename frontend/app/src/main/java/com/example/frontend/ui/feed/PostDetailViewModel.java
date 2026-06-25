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
import com.example.frontend.data.model.ReactionRequest;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
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

    public LiveData<List<Comment>> getCommentsLiveData() { return commentsLiveData; }
    public LiveData<String> getMessageLiveData() { return messageLiveData; }
    public LiveData<Boolean> getActionSuccessLiveData() { return actionSuccessLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Integer> getCommentCountLiveData() { return commentCountLiveData; }
    public LiveData<Post> getPostLiveData() { return postLiveData; }

    public void fetchComments(String postId) {
        isLoading.setValue(true);
        repository.getCommentsByPost(postId).enqueue(new Callback<ApiResponse<List<Comment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Comment>>> call, Response<ApiResponse<List<Comment>>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> rootComments = response.body().getData();

                    // Cấp độ 0 cho bình luận gốc
                    for (Comment c : rootComments) c.setDepth(0);

                    List<Comment> flatList = new ArrayList<>(rootComments);
                    commentsLiveData.setValue(flatList);
                    commentCountLiveData.setValue(flatList.size());

                    // Gọi API lấy phản hồi cấp 1
                    for (Comment root : rootComments) {
                        fetchRepliesForComment(root.getId(), 1);
                    }
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

    // 👉 THUẬT TOÁN ĐỆ QUY TÌM VÀ LẮP RÁP BÌNH LUẬN N-CẤP
    private void fetchRepliesForComment(String parentId, int currentDepth) {
        repository.getReplies(parentId).enqueue(new Callback<ApiResponse<List<Comment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Comment>>> call, Response<ApiResponse<List<Comment>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> replies = response.body().getData();
                    if (replies != null && !replies.isEmpty()) {

                        List<Comment> current = commentsLiveData.getValue();
                        if (current == null) return;

                        List<Comment> newList = new ArrayList<>(current);
                        for (Comment r : replies) r.setDepth(currentDepth); // Đặt cấp độ

                        int insertIndex = -1;
                        int parentDepth = -1;

                        // Tìm bình luận cha
                        for (int i = 0; i < newList.size(); i++) {
                            if (newList.get(i).getId().equals(parentId)) {
                                insertIndex = i + 1;
                                parentDepth = newList.get(i).getDepth();

                                // Bỏ qua các comment con/cháu chắt hiện tại để chèn xuống cuối cùng của nhánh
                                while (insertIndex < newList.size() && newList.get(insertIndex).getDepth() > parentDepth) {
                                    insertIndex++;
                                }
                                break;
                            }
                        }

                        if (insertIndex != -1) {
                            newList.addAll(insertIndex, replies);
                            commentsLiveData.setValue(newList);
                            commentCountLiveData.setValue(newList.size());

                            // 👉 GỌI ĐỆ QUY: Tìm tiếp phản hồi của các phản hồi này (Cấp 3, Cấp 4...)
                            for (Comment r : replies) {
                                fetchRepliesForComment(r.getId(), currentDepth + 1);
                            }
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Comment>>> call, Throwable t) {}
        });
    }

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
                        created.setUserId(new User(myId, myName, myAvatar));
                    }

                    List<Comment> current = commentsLiveData.getValue();
                    List<Comment> newList = (current == null) ? new ArrayList<>() : new ArrayList<>(current);

                    if (created != null) {
                        if (parentId == null) {
                            created.setDepth(0);
                            newList.add(0, created);
                        } else {
                            int insertIndex = -1;
                            int parentDepth = 0;
                            for (int i = 0; i < newList.size(); i++) {
                                if (newList.get(i).getId().equals(parentId)) {
                                    insertIndex = i + 1;
                                    parentDepth = newList.get(i).getDepth();
                                    while (insertIndex < newList.size() && newList.get(insertIndex).getDepth() > parentDepth) {
                                        insertIndex++;
                                    }
                                    break;
                                }
                            }
                            if (insertIndex != -1) {
                                created.setDepth(parentDepth + 1);
                                newList.add(insertIndex, created);
                            } else {
                                newList.add(created); // Fallback
                            }
                        }
                    }

                    commentsLiveData.setValue(newList);
                    commentCountLiveData.setValue(newList.size());
                } else {
                    messageLiveData.setValue("Lỗi đăng bình luận");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Comment>> call, Throwable t) {
                messageLiveData.setValue("Lỗi mạng");
            }
        });
    }

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

    public void fetchPostById(String postId) {
        postRepository.getPostById(postId).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postLiveData.setValue(response.body().getData());
                } else {
                    messageLiveData.setValue("Lỗi Backend: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
                messageLiveData.setValue("Lỗi mạng khi tải bài viết");
            }
        });
    }

    public void toggleCommentReaction(String commentId, String reactionType) {
        postRepository.toggleReaction(commentId, "Comment", reactionType, new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (!response.isSuccessful()) {
                    messageLiveData.setValue("Lỗi lưu cảm xúc: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                messageLiveData.setValue("Lỗi mạng: " + t.getMessage());
            }
        });
    }
}