package com.example.frontend.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.User;
import com.example.frontend.data.repository.PostRepository;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.ui.feed.FeedFragment;
import com.example.frontend.ui.feed.PostAdapter;
import com.example.frontend.utils.FileUtils;
import com.example.frontend.utils.Result;
import com.example.frontend.ui.profile.ProfilePostAdapter;
import com.example.frontend.ui.profile.ProfileFeedFragment;


import java.io.File;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE = 100;

    private ImageView imgAvatar;
    private TextView tvName, tvBio, tvStats;
    private Button btnEdit;

    private UserRepository repository;

    private ImageView imgCover;
    private RecyclerView rvPosts;
    private ProfilePostAdapter adapter;
    private PostRepository postRepository;
    private TextView tvEmptyPost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvName = view.findViewById(R.id.tvUserName);
        tvBio = view.findViewById(R.id.tvBio);
        tvStats = view.findViewById(R.id.tvStats);
        btnEdit = view.findViewById(R.id.btnEdit);
        imgCover = view.findViewById(R.id.imgCover);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPosts.setNestedScrollingEnabled(false);

        repository = new UserRepository(requireContext());

        loadProfile();
        loadMyPosts();

        imgAvatar.setOnClickListener(v -> openGallery());
        btnEdit.setOnClickListener(v -> showEditOptions());

        return view;
    }

    private void loadProfile() {
        repository.getProfile().observe(getViewLifecycleOwner(), result -> {

            if (result.status == Result.Status.SUCCESS && result.data != null) {
                User user = result.data;

                // NAME
                String name = user.getUsername();
                tvName.setText(
                        (name != null && !name.trim().isEmpty())
                                ? name
                                : "Người dùng"
                );

                // BIO
                String bio = user.getBio();
                if (bio != null && !bio.isEmpty()) {
                    tvBio.setText(bio);
                    tvBio.setVisibility(View.VISIBLE);
                } else {
                    tvBio.setVisibility(View.GONE);
                }

                // STATS
                int friends = user.getFriendCount();
                int groups = user.getGroupCount();
                tvStats.setText(String.format("%d friends • %d groups", friends, groups));

                // AVATAR
                String avatar = user.getAvatar();
                if (avatar != null && !avatar.isEmpty()) {
                    Glide.with(this)
                            .load(avatar)
                            .placeholder(R.drawable.ic_profile)
                            .into(imgAvatar);
                } else {
                    imgAvatar.setImageResource(R.drawable.ic_profile);
                }

                // COVER
                String cover = user.getCover();
                if (cover != null && !cover.isEmpty()) {
                    Glide.with(this)
                            .load(cover)
                            .placeholder(R.drawable.bg_cover_default)
                            .into(imgCover);
                } else {
                    imgCover.setImageResource(R.drawable.bg_cover_default);
                }

                //FEED
//                Fragment feedFragment = new ProfileFeedFragment(true);

//                getChildFragmentManager()
//                        .beginTransaction()
//                        .replace(R.id.feedContainer, feedFragment)
//                        .commit();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();

            imgAvatar.setImageURI(uri);

            File file = FileUtils.getFileFromUri(requireContext(), uri);

            repository.uploadAvatar(file).observe(getViewLifecycleOwner(), result -> {
                if (result.status == Result.Status.SUCCESS) {
                    loadProfile();
                }
            });
        }
    }

    private void showEditOptions() {
        String[] options = {"Đổi Avatar"};

        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    openGallery();
                })
                .show();
    }

    private void loadMyPosts() {
        postRepository.getMyPosts(new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Post>>> call, Response<ApiResponse<List<Post>>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    List<Post> posts = response.body().data;

                    if (posts == null || posts.isEmpty()) {
                        rvPosts.setVisibility(View.GONE);
                        tvEmptyPost.setVisibility(View.VISIBLE);
                    } else {
                        rvPosts.setVisibility(View.VISIBLE);
                        tvEmptyPost.setVisibility(View.GONE);

                        adapter = new ProfilePostAdapter(getContext(), posts);
                        rvPosts.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {

            }
        });
    }
}