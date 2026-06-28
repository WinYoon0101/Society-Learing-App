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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.StoryGroup;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;

import java.util.ArrayList;
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

        ImageView imgMyAvatarInFeed = view.findViewById(R.id.imgMyAvatarInFeed);
        TextView tvCreatePostHint = view.findViewById(R.id.tvCreatePostHint);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String myAvatarUrl = prefs.getString("USER_AVATAR", "");
        String myUsername = prefs.getString("USERNAME", "Bạn");

        if (tvCreatePostHint != null) {
            String shortName = myUsername;
            if (myUsername.contains(" ")) {
                shortName = myUsername.substring(myUsername.lastIndexOf(" ") + 1);
            }
            tvCreatePostHint.setText(shortName + " ơi, bạn muốn chia sẻ kiến thức gì?");
        }

        if (!myAvatarUrl.isEmpty() && imgMyAvatarInFeed != null) {
            Glide.with(this)
                    .load(myAvatarUrl)
                    .placeholder(R.drawable.ic_user)
                    .into(imgMyAvatarInFeed);
        }

        RecyclerView rvStories = view.findViewById(R.id.rvStories);
        rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        storyAdapter = new StoryAdapter(getContext());
        rvStories.setAdapter(storyAdapter);
        loadStories();

        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        viewModel.init(getContext());

        RecyclerView rcv = view.findViewById(R.id.rvPosts);
        rcv.setLayoutManager(new LinearLayoutManager(getContext()));
        rcv.setNestedScrollingEnabled(false);

        adapter = new PostAdapter(getContext(), new ArrayList<>(), (targetId, type) -> {
            String reactionToSend = (type == null) ? "Like" : type;

            if (viewModel != null) {
                Log.d("DEBUG_REACT", "Đang gửi API thả cảm xúc lên Server: " + reactionToSend);
                viewModel.toggleReaction(targetId, "Post", reactionToSend);
            }
        });

        adapter.setOnPostSaveListener(postId -> {
            String token = "Bearer " + requireActivity()
                    .getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    .getString("JWT_TOKEN", "");

            Toast.makeText(getContext(), "Đang xử lý...", Toast.LENGTH_SHORT).show();
            viewModel.toggleSavePost(token, postId);
        });

        adapter.setOnPostDeleteListener(postId -> {
            String token = "Bearer " + requireActivity()
                    .getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    .getString("JWT_TOKEN", "");

            Toast.makeText(getContext(), "Đang xóa...", Toast.LENGTH_SHORT).show();
            viewModel.deletePost(token, postId);
        });

        rcv.setAdapter(adapter);

        viewModel.getPosts().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
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

        viewModel.getDeleteStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == null) return;

            if ("SUCCESS".equals(status)) {
                Toast.makeText(getContext(), "Xóa bài viết thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
            }

            viewModel.clearDeleteStatus();
        });

        viewModel.getSaveStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == null) return;

            Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();

            viewModel.clearSaveStatus();
        });

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

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {
                    storyAdapter.submit(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<StoryGroup>>> call, Throwable t) {
                Log.e("DEBUG_STORY", "Lỗi tải stories: " + t.getMessage());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (viewModel != null) {
            viewModel.loadPosts();
        }

        loadStories();
    }
}