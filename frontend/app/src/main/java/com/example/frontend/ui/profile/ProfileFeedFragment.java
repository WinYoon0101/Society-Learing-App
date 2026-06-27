package com.example.frontend.ui.profile;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.ui.feed.CreatePostFragment;
import com.example.frontend.ui.feed.FeedViewModel;
import com.example.frontend.ui.feed.PostAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFeedFragment extends Fragment {

    // Nếu có userId được truyền vào → hiện bài của user đó, không có → hiện bài của mình
    public static final String ARG_USER_ID = "userId";

    private RecyclerView rvPosts;
    private PostAdapter adapter;;
    private ApiService apiService;
    private TextView tvEmpty;
    private FeedViewModel viewModel;

    public static ProfileFeedFragment forUser(String userId) {
        ProfileFeedFragment f = new ProfileFeedFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USER_ID, userId);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvPosts  = view.findViewById(R.id.rvPosts);
        tvEmpty  = view.findViewById(R.id.tvEmptyPosts);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter(getContext(), new ArrayList<>(), (targetId, type) -> {
            // Fix lỗi truyền Null cho Backend khi người dùng ấn Hủy Like
            String reactionToSend = (type == null) ? "Like" : type;

            if (viewModel != null) {
                // CHÚ Ý: Truyền đúng chữ "Post" (không có s) để Backend Node.js nhận diện đúng
                Log.d("DEBUG_REACT", "👉 Đang gửi API thả tim lên Server: " + reactionToSend);
                viewModel.toggleReaction(targetId, "Post", reactionToSend);
            }
        });
        adapter.setOnPostSaveListener(postId -> {
            String token = "Bearer " + requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).getString("JWT_TOKEN", "");
            Toast.makeText(getContext(), "Đang xử lý...", Toast.LENGTH_SHORT).show();
            viewModel.toggleSavePost(token, postId); // Ra lệnh cho ViewModel gọi API Lưu
        });

        // =======================================================
        // BẮT SÓNG LỆNH XÓA TỪ ADAPTER TRUYỀN RA
        // =======================================================
        adapter.setOnPostDeleteListener(postId -> {
            // Lấy Token của bạn để gửi lên Server chứng minh thân phận
            String token = "Bearer " + requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).getString("JWT_TOKEN", "");

            Toast.makeText(getContext(), "Đang xóa...", Toast.LENGTH_SHORT).show();
            viewModel.deletePost(token, postId); // Ra lệnh cho ViewModel gọi API Xóa
        });
        rvPosts.setAdapter(adapter);
        apiService = ApiClient.getApiService(requireContext());

        // Click listener cho nút "Bạn đang nghĩ gì?"
        View btnOpenCreatePost = view.findViewById(R.id.btnOpenCreatePost);
        if (btnOpenCreatePost != null) {
            btnOpenCreatePost.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CreatePostFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        viewModel.init(getContext());
        loadPosts();
    }

    private void loadPosts() {
        String targetUserId = getArguments() != null ? getArguments().getString(ARG_USER_ID) : null;
        String myId = requireContext()
                .getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                .getString("USER_ID", "");

        Callback<ApiResponse<List<Post>>> cb = new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Post>>> call,
                                   Response<ApiResponse<List<Post>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<Post> posts = response.body().getData();
                    
                    // Filter bài viết: chỉ hiện bài đăng ở profile (groupId == null), không hiện bài đăng trong nhóm
                    List<Post> profilePostsOnly = new ArrayList<>();
                    if (posts != null) {
                        for (Post post : posts) {
                            if (post.getGroupId() == null || post.getGroupId().isEmpty()) {
                                profilePostsOnly.add(post);
                            }
                        }
                    }
                    
                    adapter.updateData(profilePostsOnly);
                    boolean empty = profilePostsOnly.isEmpty();
                    if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rvPosts.setVisibility(empty ? View.GONE : View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {}
        };

        boolean isOwnProfile = targetUserId == null || targetUserId.equals(myId);
        if (isOwnProfile) {
            apiService.getMyPosts().enqueue(cb);
        } else {
            apiService.getPostsByUser(targetUserId).enqueue(cb);
        }
    }
}
