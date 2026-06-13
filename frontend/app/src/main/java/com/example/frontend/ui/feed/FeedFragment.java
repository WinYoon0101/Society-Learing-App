package com.example.frontend.ui.feed;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import java.util.ArrayList;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.StoryGroup;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedFragment extends Fragment {
    private FeedViewModel viewModel;
    private PostAdapter adapter;
    private StoryAdapter storyAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        // =======================================================
        // CẬP NHẬT GIAO DIỆN THANH ĐĂNG BÀI (AVATAR VÀ TÊN USER)
        // =======================================================
        ImageView imgMyAvatarInFeed = view.findViewById(R.id.imgMyAvatarInFeed);
        TextView tvCreatePostHint = view.findViewById(R.id.tvCreatePostHint);

        // Lấy thông tin từ SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String myAvatarUrl = prefs.getString("USER_AVATAR", "");
        String myUsername = prefs.getString("USERNAME", "Bạn");

        // 1. Cài đặt chữ (Cắt lấy tên cuối cho giống Facebook)
        if (tvCreatePostHint != null) {
            String shortName = myUsername;
            if (myUsername.contains(" ")) {
                shortName = myUsername.substring(myUsername.lastIndexOf(" ") + 1); // Lấy chữ cuối cùng
            }
            tvCreatePostHint.setText(shortName + " ơi, bạn muốn chia sẻ kiến thức gì?");
        }

        // 2. Cài đặt Avatar
        if (!myAvatarUrl.isEmpty() && imgMyAvatarInFeed != null) {
            Glide.with(this)
                    .load(myAvatarUrl)
                    .placeholder(R.drawable.ic_user)
                    .into(imgMyAvatarInFeed);
        }

        // =======================================================
        // STORY STRIP
        // =======================================================
        RecyclerView rvStories = view.findViewById(R.id.rvStories);
        rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        storyAdapter = new StoryAdapter(getContext());
        rvStories.setAdapter(storyAdapter);
        loadStories();

        // =======================================================
        // 1. Kết nối ViewModel
        // =======================================================
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        viewModel.init(getContext());

        // =======================================================
        // 2. Setup RecyclerView
        // =======================================================
        RecyclerView rcv = view.findViewById(R.id.rvPosts);
        rcv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Khởi tạo Adapter kèm Interface Lắng nghe Reaction
        adapter = new PostAdapter(getContext(), new ArrayList<>(), (targetId, type) -> {
            // Fix lỗi truyền Null cho Backend khi người dùng ấn Hủy Like
            String reactionToSend = (type == null) ? "Like" : type;

            if (viewModel != null) {
                // CHÚ Ý: Truyền đúng chữ "Post" (không có s) để Backend Node.js nhận diện đúng
                Log.d("DEBUG_REACT", "👉 Đang gửi API thả tim lên Server: " + reactionToSend);
                viewModel.toggleReaction(targetId, "Post", reactionToSend);
            }
        });

        // =======================================================
        // ĐÃ THÊM MỚI: BẮT SÓNG LỆNH LƯU TỪ ADAPTER TRUYỀN RA
        // =======================================================
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

        rcv.setAdapter(adapter);

        // =======================================================
        // 3. Quan sát dữ liệu từ ViewModel
        // =======================================================
        viewModel.getPosts().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {

                // Filter bài viết: chỉ hiện bài đăng ở home (groupId == null), không hiện bài đăng trong nhóm
                List<Post> homePostsOnly = new ArrayList<>();
                for (Post post : list) {
                    if (post.getGroupId() == null || post.getGroupId().isEmpty()) {
                        homePostsOnly.add(post);
                    }
                }
                adapter.updateData(homePostsOnly);

            } else {
                Toast.makeText(getContext(), "Không có bài viết nào hoặc lỗi tải tin", Toast.LENGTH_SHORT).show();
            }
        });

        // Lắng nghe báo cáo kết quả XÓA từ ViewModel
        viewModel.getDeleteStatus().observe(getViewLifecycleOwner(), status -> {
            if ("SUCCESS".equals(status)) {
                Toast.makeText(getContext(), "Xóa bài viết thành công!", Toast.LENGTH_SHORT).show();
            } else if (status != null) {
                Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
            }
        });

        // =======================================================
        // 4. Nút mở màn hình tạo bài viết
        // =======================================================
        view.findViewById(R.id.btnOpenCreatePost).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CreatePostFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadStories() {
        ApiService api = ApiClient.getApiService(getContext());
        api.getFeedStories().enqueue(new Callback<ApiResponse<List<StoryGroup>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<StoryGroup>>> call,
                                   Response<ApiResponse<List<StoryGroup>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    storyAdapter.submit(response.body().getData());
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<StoryGroup>>> call, Throwable t) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadPosts();
        loadStories();
    }
}