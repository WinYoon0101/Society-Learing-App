package com.example.frontend.ui.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.User;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.utils.FileUtils;
import com.example.frontend.utils.Result;

import java.io.File;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int TYPE_AVATAR = 1;
    private static final int TYPE_COVER = 2;

    private int currentType = TYPE_AVATAR;

    //  lưu URI và URL cũ để rollback nếu upload lỗi
    private Uri currentSelectedUri = null;
    private String oldAvatarUrl = null;
    private String oldCoverUrl = null;

    // Flag: đang trong quá trình chọn/upload ảnh — ngăn loadProfile() ghi đè ảnh mới
    private boolean isSelectingImage = false;

    private ImageView imgAvatar, imgCover;
    private TextView tvName, tvStats;
    private Button btnEdit;
    private Button btnEditDetails;

    private UserRepository repository;
    private TextView tvBio, tvLocation, tvHometown, tvBirthday, tvGender;

    // ================= ACTIVITY RESULT LAUNCHERS =================
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Log.d(TAG, "Ảnh được chọn: " + uri);
                    handleImageSelected(uri);
                } else {
                    Log.d(TAG, "Không chọn ảnh nào");
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = false;
                for (Boolean value : result.values()) {
                    if (value) { granted = true; break; }
                }
                if (granted) {
                    openGallery();
                } else {
                    Toast.makeText(requireContext(),
                            "Cần cấp quyền truy cập ảnh để đổi ảnh đại diện/bìa",
                            Toast.LENGTH_LONG).show();
                }
            });

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
                tvStats.setText(user.getFriendCount() + " bạn bè • " + user.getGroupCount() + " nhóm");

                if (user.getBio() != null && !user.getBio().isEmpty()) {
                    tvBio.setVisibility(View.VISIBLE);
                    tvBio.setText(user.getBio());
                }
                if (user.getLocation() != null && !user.getLocation().isEmpty()) {
                    tvLocation.setVisibility(View.VISIBLE);
                    tvLocation.setText("Đang ở " + user.getLocation());
                }
                if (user.getHometown() != null && !user.getHometown().isEmpty()) {
                    tvHometown.setVisibility(View.VISIBLE);
                    tvHometown.setText("Đến từ " + user.getHometown());
                }
                if (user.getBirthday() != null && !user.getBirthday().isEmpty()) {
                    tvBirthday.setVisibility(View.VISIBLE);
                    tvBirthday.setText("Sinh ngày " + user.getBirthday());
                }
                if (user.getGender() != null && !user.getGender().isEmpty()) {
                    tvGender.setVisibility(View.VISIBLE);
                    tvGender.setText("Giới tính " + user.getGender());
                }

                Log.d(TAG, "Avatar URL: " + user.getAvatar());
                Log.d(TAG, "Cover URL: " + user.getCover());

                // Lưu URL cũ để rollback nếu upload lỗi
                oldAvatarUrl = user.getAvatar();
                oldCoverUrl = user.getCover();

                // Chỉ load ảnh nếu không đang trong quá trình chọn/upload ảnh
                // (tránh override ảnh optimistic UI)
                if (!isSelectingImage) {
                    Glide.with(requireContext())
                            .load(user.getAvatar())
                            .placeholder(R.drawable.ic_profile)
                            .skipMemoryCache(true)
                            .into(imgAvatar);

                    Glide.with(requireContext())
                            .load(user.getCover())
                            .placeholder(R.drawable.bg_cover_default)
                            .skipMemoryCache(true)
                            .into(imgCover);
                }

            } else if (result.status == Result.Status.ERROR) {
                Log.e(TAG, "Lỗi tải profile: " + result.message);
            }
        });
    }

    // ================= EDIT =================
    private void showEditOptions() {
        String[] options = {"Đổi Avatar", "Đổi Ảnh bìa"};
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    currentType = (which == 0) ? TYPE_AVATAR : TYPE_COVER;
                    checkPermissionAndOpenGallery();
                })
                .show();
    }

    // ================= PERMISSION =================
    private void checkPermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ dùng READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
            }
        } else {
            // Android 12 trở xuống dùng READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        }
    }

    // ================= GALLERY =================
    private void openGallery() {
        isSelectingImage = true; // Bắt đầu quá trình chọn ảnh
        pickImageLauncher.launch("image/*");
    }

    // ================= HANDLE IMAGE SELECTED =================
    private void handleImageSelected(Uri uri) {
        currentSelectedUri = uri;

        // Dùng Glide load từ URI local — hiển thị ngay, không chờ upload
        ImageView target = (currentType == TYPE_AVATAR) ? imgAvatar : imgCover;
        int placeholder = (currentType == TYPE_AVATAR) ? R.drawable.ic_profile : R.drawable.bg_cover_default;

        Glide.with(requireContext())
                .load(uri)
                .placeholder(placeholder)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .into(target);

        // Convert URI sang File để upload
        File file = FileUtils.getFileFromUri(requireContext(), uri);
        if (file == null || !file.exists()) {
            Toast.makeText(requireContext(), "Không thể đọc file ảnh, thử lại", Toast.LENGTH_SHORT).show();
            rollbackImage(); // Rollback về ảnh cũ
            return;
        }

        Log.d(TAG, "File để upload: " + file.getAbsolutePath() + " | Size: " + file.length() + " bytes");
        confirmUpload(uri, file);
    }

    // ================= ROLLBACK =================
    private void rollbackImage() {
        if (currentType == TYPE_AVATAR) {
            Glide.with(requireContext())
                    .load(oldAvatarUrl)
                    .placeholder(R.drawable.ic_profile)
                    .into(imgAvatar);
        } else {
            Glide.with(requireContext())
                    .load(oldCoverUrl)
                    .placeholder(R.drawable.bg_cover_default)
                    .into(imgCover);
        }
    }

    // ================= CONFIRM =================
    private void confirmUpload(Uri previewUri, File file) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận")
                .setMessage("Cập nhật ảnh này?")
                .setPositiveButton("OK", (d, w) -> {
                    Toast.makeText(requireContext(), "Đang tải lên...", Toast.LENGTH_SHORT).show();

                    if (currentType == TYPE_AVATAR) {
                        repository.uploadAvatar(file).observe(getViewLifecycleOwner(), r -> {
                            if (r.status == Result.Status.SUCCESS) {
                                oldAvatarUrl = r.data;
                                isSelectingImage = false; // Hoàn tất, cho phép loadProfile() cập nhật bình thường
                                Toast.makeText(requireContext(), "Đổi ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Avatar mới: " + r.data);
                            } else if (r.status == Result.Status.ERROR) {
                                isSelectingImage = false;
                                Toast.makeText(requireContext(), "Upload thất bại, khôi phục ảnh cũ", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Upload avatar lỗi: " + r.message);
                                rollbackImage();
                            }
                        });
                    } else {
                        repository.uploadCover(file).observe(getViewLifecycleOwner(), r -> {
                            if (r.status == Result.Status.SUCCESS) {
                                oldCoverUrl = r.data;
                                isSelectingImage = false;
                                Toast.makeText(requireContext(), "Đổi ảnh bìa thành công!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Cover mới: " + r.data);
                            } else if (r.status == Result.Status.ERROR) {
                                isSelectingImage = false;
                                Toast.makeText(requireContext(), "Upload thất bại, khôi phục ảnh cũ", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Upload cover lỗi: " + r.message);
                                rollbackImage();
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", (d, w) -> {
                    isSelectingImage = false; // Hủy → cho phép reload bình thường
                    rollbackImage();
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }
}