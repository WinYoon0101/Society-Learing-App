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
                    List<Comment> flatList = new ArrayList<>(rootComments);
                    commentsLiveData.setValue(flatList);
                    commentCountLiveData.setValue(flatList.size());

                    for (Comment root : rootComments) {
                        fetchRepliesForComment(root.getId());
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

    private void fetchRepliesForComment(String parentId) {
        repository.getReplies(parentId).enqueue(new Callback<ApiResponse<List<Comment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Comment>>> call, Response<ApiResponse<List<Comment>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Comment> replies = response.body().getData();
                    if (replies != null && !replies.isEmpty()) {
                        List<Comment> current = commentsLiveData.getValue();
                        if (current == null) return;

                        int insertIndex = -1;
                        for (int i = 0; i < current.size(); i++) {
                            if (current.get(i).getId().equals(parentId)) {
                                insertIndex = i + 1;
                                break;
                            }
                        }

                        if (insertIndex != -1) {
                            current.addAll(insertIndex, replies);
                            commentsLiveData.setValue(current);
                            commentCountLiveData.setValue(current.size());
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

                        if (created.getUserId() == null) {
                            created.setUserId(new User(myId, myName, myAvatar));
                        } else {
                            User u = created.getUserId();
                            created.setUserId(new User(u.getId(), u.getUsername() != null ? u.getUsername() : myName, myAvatar));
                        }
                    }

                    List<Comment> current = commentsLiveData.getValue();
                    if (current == null) current = new ArrayList<>();

                    if (created != null) {
                        if (parentId == null) {
                            current.add(0, created);
                        } else {
                            int insertPos = current.size();
                            for (int i = 0; i < current.size(); i++) {
                                if (current.get(i).getId().equals(parentId)) {
                                    insertPos = i + 1;
                                    while (insertPos < current.size() && current.get(insertPos).getParentId() != null && current.get(insertPos).getParentId().equals(parentId)) {
                                        insertPos++;
                                    }
                                    break;
                                }
                            }
                            current.add(insertPos, created);
                        }
                    }

                    commentsLiveData.setValue(current);
                    commentCountLiveData.setValue(current.size());

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

    // 👉 LOGIC 5: GỌI API THẢ CẢM XÚC (ĐÃ SỬA LẠI SỬ DỤNG TRỰC TIẾP APISERVICE)
    public void toggleCommentReaction(String commentId, String reactionType) {
        ReactionRequest request = new ReactionRequest(commentId, "Comment", reactionType);

        // Gọi ApiService thông qua ApiClient, interceptor sẽ tự đính kèm token
        ApiService apiService = ApiClient.getApiService(getApplication().getApplicationContext());

        apiService.toggleReaction(request).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (!response.isSuccessful()) {
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                messageLiveData.setValue("Lỗi mạng: " + t.getMessage());
            }
        });
    }
}