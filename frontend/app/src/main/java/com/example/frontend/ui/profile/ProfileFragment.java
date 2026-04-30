package com.example.frontend.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.User;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.utils.FileUtils;
import com.example.frontend.utils.Result;

import java.io.File;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE = 999;
    private static final int TYPE_AVATAR = 1;
    private static final int TYPE_COVER = 2;

    private int currentType = TYPE_AVATAR;

    private ImageView imgAvatar, imgCover;
    private TextView tvName, tvStats;
    private Button btnEdit;
    private Button btnEditDetails;

    private UserRepository repository;
    private TextView tvBio, tvLocation, tvHometown, tvBirthday, tvGender;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imgAvatar = view.findViewById(R.id.imgAvatar);
        imgCover = view.findViewById(R.id.imgCover);
        tvName = view.findViewById(R.id.tvUserName);
        tvStats = view.findViewById(R.id.tvStats);
        btnEdit = view.findViewById(R.id.btnEdit);
        tvBio = view.findViewById(R.id.tvBio);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvHometown = view.findViewById(R.id.tvHometown);
        tvBirthday = view.findViewById(R.id.tvBirthday);
        tvGender = view.findViewById(R.id.tvGender);
        btnEditDetails = view.findViewById(R.id.btnEditDetails);

        repository = new UserRepository(requireContext());

        loadProfile();

        btnEdit.setOnClickListener(v -> showEditOptions());

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.feedContainer, new ProfileFeedFragment())
                    .commit();
        }

        btnEditDetails.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        return view;
    }

    // ================= LOAD PROFILE =================
    private void loadProfile() {

        repository.getProfile().observe(getViewLifecycleOwner(), result -> {

            if (result.status == Result.Status.SUCCESS && result.data != null) {

                User user = result.data;

                tvName.setText(user.getUsername());

                tvStats.setText(
                        user.getFriendCount() + " friends • " +
                                user.getGroupCount() + " groups"
                );
                if (user.getBio() != null && !user.getBio().isEmpty()) {
                    tvBio.setVisibility(View.VISIBLE);
                    tvBio.setText(user.getBio());
                }
                if (user.getLocation() != null) {
                    tvLocation.setVisibility(View.VISIBLE);
                    tvLocation.setText("Đang ở " + user.getLocation());
                }

                if (user.getHometown() != null) {
                    tvHometown.setVisibility(View.VISIBLE);
                    tvHometown.setText("Đến từ " + user.getHometown());
                }

                if (user.getBirthday() != null) {
                    tvBirthday.setVisibility(View.VISIBLE);
                    tvBirthday.setText("Sinh ngày " + user.getBirthday());
                }

                if (user.getGender() != null) {
                    tvGender.setVisibility(View.VISIBLE);
                    tvGender.setText("Giới tính " + user.getGender());
                }

                Glide.with(requireContext())
                        .load(user.getAvatar())
                        .placeholder(R.drawable.ic_profile)
                        .into(imgAvatar);

                Glide.with(requireContext())
                        .load(user.getCover())
                        .placeholder(R.drawable.bg_cover_default)
                        .into(imgCover);
            }

        });
    }

    // ================= EDIT =================
    private void showEditOptions() {

        String[] options = {"Đổi Avatar", "Đổi Ảnh bìa"};

        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {

                    currentType = (which == 0) ? TYPE_AVATAR : TYPE_COVER;
                    openGallery();

                })
                .show();
    }

    // ================= GALLERY =================
    private void openGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    // ================= RESULT =================
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {

            Uri uri = data.getData();
            File file = FileUtils.getFileFromUri(requireContext(), uri);

            if (currentType == TYPE_AVATAR) {
                imgAvatar.setImageURI(uri);
            } else {
                imgCover.setImageURI(uri);
            }

            confirmUpload(file);
        }
    }

    // ================= CONFIRM =================
    private void confirmUpload(File file) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận")
                .setMessage("Cập nhật ảnh này?")
                .setPositiveButton("OK", (d, w) -> {

                    if (currentType == TYPE_AVATAR) {

                        repository.uploadAvatar(file)
                                .observe(getViewLifecycleOwner(), r -> {
                                    if (r.status == Result.Status.SUCCESS) {
                                        loadProfile(); // 🔥 reload từ server
                                    }
                                });

                    } else {

                        repository.uploadCover(file)
                                .observe(getViewLifecycleOwner(), r -> {
                                    if (r.status == Result.Status.SUCCESS) {
                                        loadProfile();
                                    }
                                });
                    }

                })
                .setNegativeButton("Hủy", (d, w) -> loadProfile())
                .show();
    }
    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }
}