package com.example.frontend.ui.profile;

import android.Manifest;
import android.content.Context;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.Media;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.utils.FileUtils;
import com.example.frontend.utils.Result;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int TYPE_AVATAR = 1;
    private static final int TYPE_COVER  = 2;

    private int currentType = TYPE_AVATAR;
    private Uri currentSelectedUri = null;
    private String oldAvatarUrl = null;
    private String oldCoverUrl  = null;
    private boolean isSelectingImage = false;

    private ImageView imgAvatar, imgCover;
    private TextView tvName, tvStats, tvBio;
    private Button btnEdit;
    private LinearLayout tabAll, tabFriends, tabPic;
    private View lineAll, lineFriends, linePic;
    private FrameLayout contentContainer;

    private UserRepository repository;
    private ApiService apiService;

    // ─── Launchers ────────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleImageSelected(uri);
            });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                for (Boolean v : result.values()) {
                    if (v) { openGallery(); return; }
                }
                Toast.makeText(requireContext(), "Cần cấp quyền truy cập ảnh", Toast.LENGTH_LONG).show();
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgAvatar      = view.findViewById(R.id.imgAvatar);
        imgCover       = view.findViewById(R.id.imgCover);
        tvName         = view.findViewById(R.id.tvUserName);
        tvStats        = view.findViewById(R.id.tvStats);
        btnEdit        = view.findViewById(R.id.btnEdit);
        tvBio          = view.findViewById(R.id.tvBio);
        tabAll         = view.findViewById(R.id.tabAll);
        tabFriends     = view.findViewById(R.id.tabFriends);
        tabPic         = view.findViewById(R.id.tabPic);
        lineAll        = view.findViewById(R.id.lineAll);
        lineFriends    = view.findViewById(R.id.lineFriends);
        linePic        = view.findViewById(R.id.linePic);
        contentContainer = view.findViewById(R.id.contentContainer);

        repository = new UserRepository(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        loadProfile();

        btnEdit.setOnClickListener(v -> showEditOptions());

        tabAll.setOnClickListener(v -> { selectTab(lineAll); showTabAll(); });
        tabFriends.setOnClickListener(v -> { selectTab(lineFriends); showTabFriends(); });
        tabPic.setOnClickListener(v -> { selectTab(linePic); showTabPictures(); });

        selectTab(lineAll);
        showTabAll();
    }

    // ─── Load profile header ──────────────────────────────────────────────────
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
                oldAvatarUrl = user.getAvatar();
                oldCoverUrl  = user.getCover();
                if (!isSelectingImage) {
                    Glide.with(requireContext()).load(user.getAvatar())
                            .placeholder(R.drawable.ic_profile)
                            .skipMemoryCache(true).into(imgAvatar);
                    Glide.with(requireContext()).load(user.getCover())
                            .placeholder(R.drawable.bg_cover_default)
                            .skipMemoryCache(true).into(imgCover);
                }
            }
        });
    }

    // ─── Tab ALL: bài viết + chi tiết ────────────────────────────────────────
    private void showTabAll() {
        contentContainer.removeAllViews();
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_profile_all, contentContainer, true);

        TextView tvLocation = v.findViewById(R.id.tvLocation);
        TextView tvHometown = v.findViewById(R.id.tvHometown);
        TextView tvBirthday = v.findViewById(R.id.tvBirthday);
        TextView tvGender   = v.findViewById(R.id.tvGender);
        Button btnEditDetails = v.findViewById(R.id.btnEditDetails);


        // FIX: Tải ảnh avatar vào phần "Bạn đang nghĩ gì"
        ImageView imgPostAvatar = v.findViewById(R.id.imgPostAvatar);

        // Load lại profile để hiện chi tiết & avatar đăng bài

        repository.getProfile().observe(getViewLifecycleOwner(), result -> {
            if (result.status != Result.Status.SUCCESS || result.data == null) return;
            User user = result.data;
            setOptional(tvLocation, user.getLocation() != null ? "Đang ở " + user.getLocation() : null);
            setOptional(tvHometown, user.getHometown() != null ? "Đến từ " + user.getHometown() : null);
            setOptional(tvBirthday, user.getBirthday() != null ? "Sinh ngày " + user.getBirthday() : null);
            setOptional(tvGender,   user.getGender()   != null ? "Giới tính: " + user.getGender() : null);


            // Nếu có ô avatar đăng bài thì hiển thị ảnh của user
            if (imgPostAvatar != null && user.getAvatar() != null) {
                Glide.with(requireContext()).load(user.getAvatar())
                        .placeholder(R.drawable.ic_profile)
                        .into(imgPostAvatar);
            }

        });

        btnEditDetails.setOnClickListener(b ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        FrameLayout feedContainer = v.findViewById(R.id.feedContainer);
        getChildFragmentManager().beginTransaction()
                .replace(feedContainer.getId(), new ProfileFeedFragment())
                .commit();
    }

    // ─── Tab FRIENDS ─────────────────────────────────────────────────────────
    private void showTabFriends() {
        contentContainer.removeAllViews();
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_profile_friends, contentContainer, true);

        RecyclerView rv = v.findViewById(R.id.rvFriends);
        TextView tvEmpty = v.findViewById(R.id.tvNoFriends);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        apiService.getFriends().enqueue(new Callback<ApiResponse<List<Friend>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Friend>>> call,
                                   Response<ApiResponse<List<Friend>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<Friend> friends = response.body().getData();
                    if (friends == null || friends.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    } else {
                        rv.setAdapter(new FriendsAdapter(friends));
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Friend>>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Lỗi tải bạn bè", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Tab PICTURES ─────────────────────────────────────────────────────────
    private void showTabPictures() {
        contentContainer.removeAllViews();
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_profile_picture, contentContainer, true);

        RecyclerView rv = v.findViewById(R.id.rvPhotos);
        TextView tvEmpty = v.findViewById(R.id.tvNoPhotos);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        // Lấy ảnh và cover
        apiService.getMyProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call,
                                   Response<ApiResponse<User>> response) {
                if (!isAdded()) return;
                String coverUrl = (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess())
                        ? response.body().getData().getCover()
                        : null;

                apiService.getMyMedia("image").enqueue(new Callback<ApiResponse<List<Media>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Media>>> call2,
                                           Response<ApiResponse<List<Media>>> response2) {
                        if (!isAdded()) return;
                        if (response2.isSuccessful() && response2.body() != null
                                && response2.body().isSuccess()) {
                            List<Media> photos = response2.body().getData();
                            if (photos == null) photos = new ArrayList<>();

                            // Thêm cover vào đầu danh sách nếu có
                            if (coverUrl != null && !coverUrl.isEmpty()) {
                                Media coverMedia = new Media();
                                coverMedia.setUrl(coverUrl);
                                coverMedia.setFileType("cover");
                                photos.add(0, coverMedia);
                            }

                            if (photos.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                rv.setVisibility(View.GONE);
                            } else {
                                rv.setAdapter(new PhotosAdapter(photos));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<List<Media>>> call2, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Lỗi tải ảnh", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                if (!isAdded()) return;
                // Nếu lấy profile thất bại, vẫn tải media
                apiService.getMyMedia("image").enqueue(new Callback<ApiResponse<List<Media>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Media>>> call2,
                                           Response<ApiResponse<List<Media>>> response2) {
                        if (!isAdded()) return;
                        if (response2.isSuccessful() && response2.body() != null
                                && response2.body().isSuccess()) {
                            List<Media> photos = response2.body().getData();
                            if (photos == null || photos.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                rv.setVisibility(View.GONE);
                            } else {
                                rv.setAdapter(new PhotosAdapter(photos));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<List<Media>>> call2, Throwable t2) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Lỗi tải ảnh", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ─── Inline adapters ──────────────────────────────────────────────────────
    class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.VH> {
        private final List<Friend> items;
        FriendsAdapter(List<Friend> items) { this.items = new ArrayList<>(items); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_profile_friend, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Friend f = items.get(pos);
            h.tvName.setText(f.getUsername());
            if (f.getAvatar() != null && !f.getAvatar().isEmpty()) {
                Glide.with(h.img).load(f.getAvatar())
                        .placeholder(R.drawable.ic_user).into(h.img);
            } else {
                h.img.setImageResource(R.drawable.ic_user);
            }

            h.btnUnfriend.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Hủy kết bạn")
                        .setMessage("Hủy kết bạn với " + f.getUsername() + "?")
                        .setPositiveButton("Hủy bạn", (d, w) -> {
                            apiService.removeFriend(f.getId()).enqueue(
                                    new Callback<ApiResponse<Object>>() {
                                        @Override
                                        public void onResponse(Call<ApiResponse<Object>> c,
                                                               Response<ApiResponse<Object>> r) {
                                            if (!isAdded()) return;
                                            int p = h.getAdapterPosition();
                                            if (p >= 0) { items.remove(p); notifyItemRemoved(p); }
                                            Toast.makeText(requireContext(),
                                                    "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                                        }
                                        @Override
                                        public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {}
                                    });
                        })
                        .setNegativeButton("Không", null).show();
            });

            // Click vào tên/avatar → mở FriendProfileActivity
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), FriendProfileActivity.class);
                intent.putExtra("FRIEND_ID", f.getId());
                intent.putExtra("FRIEND_NAME", f.getUsername());
                intent.putExtra("FRIEND_AVATAR", f.getAvatar());
                startActivity(intent);
            });
        }
        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView img;
            TextView tvName;
            MaterialButton btnUnfriend;
            VH(@NonNull View v) {
                super(v);
                img         = v.findViewById(R.id.imgFriendAvatar);
                tvName      = v.findViewById(R.id.tvFriendName);
                btnUnfriend = v.findViewById(R.id.btnUnfriend);
            }
        }
    }

    class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.VH> {
        private final List<Media> items;
        PhotosAdapter(List<Media> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_photo_grid, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            String url = items.get(pos).getUrl();
            Glide.with(h.img).load(url)
                    .centerCrop().placeholder(R.drawable.bg_cover_default).into(h.img);
            h.img.setOnClickListener(v -> showFullScreenImage(url));
        }
        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView img;
            VH(@NonNull View v) { super(v); img = v.findViewById(R.id.imgPhoto); }
        }
    }

    private void showFullScreenImage(String imageUrl) {
        ImageView fullImage = new ImageView(requireContext());
        fullImage.setAdjustViewBounds(true);
        fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.bg_card)
                .into(fullImage);
        new AlertDialog.Builder(requireContext())
                .setView(fullImage)
                .setPositiveButton("Đóng", null)
                .show();
    }

    // ─── Edit avatar / cover ──────────────────────────────────────────────────
    private void showEditOptions() {
        new AlertDialog.Builder(requireContext())
                .setItems(new String[]{"Đổi Avatar", "Đổi Ảnh bìa"}, (dialog, which) -> {
                    currentType = (which == 0) ? TYPE_AVATAR : TYPE_COVER;
                    checkPermissionAndOpenGallery();
                }).show();
    }

    private void checkPermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        }
    }

    private void openGallery() { pickImageLauncher.launch("image/*"); }

    private void handleImageSelected(Uri uri) {
        isSelectingImage = true;
        currentSelectedUri = uri;
        if (currentType == TYPE_AVATAR) imgAvatar.setImageURI(uri);
        else imgCover.setImageURI(uri);
        confirmUpload(uri);
    }

    private void confirmUpload(Uri previewUri) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận")
                .setMessage("Cập nhật ảnh này?")
                .setPositiveButton("OK", (d, w) -> {
                    Toast.makeText(requireContext(), "Đang tải lên...", Toast.LENGTH_SHORT).show();
                    File file = FileUtils.getFileFromUri(requireContext(), previewUri);
                    if (file == null) {
                        Toast.makeText(requireContext(), "Không đọc được file", Toast.LENGTH_SHORT).show();
                        isSelectingImage = false; return;
                    }
                    if (currentType == TYPE_AVATAR) {
                        repository.uploadAvatar(file).observe(getViewLifecycleOwner(), r -> {
                            if (r.status == Result.Status.SUCCESS) {
                                oldAvatarUrl = r.data;
                                isSelectingImage = false;
                                requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                                        .edit().putString("USER_AVATAR", r.data).apply();
                                Toast.makeText(requireContext(), "Đổi ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                            } else if (r.status == Result.Status.ERROR) {
                                isSelectingImage = false;
                                rollbackImage();
                                Toast.makeText(requireContext(), "Upload thất bại", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        repository.uploadCover(file).observe(getViewLifecycleOwner(), r -> {
                            if (r.status == Result.Status.SUCCESS) {
                                oldCoverUrl = r.data;
                                isSelectingImage = false;
                                Toast.makeText(requireContext(), "Đổi ảnh bìa thành công!", Toast.LENGTH_SHORT).show();
                                // Refresh tab Ảnh để hiển thị cover mới
                                showTabPictures();
                            } else if (r.status == Result.Status.ERROR) {
                                isSelectingImage = false;
                                rollbackImage();
                                Toast.makeText(requireContext(), "Upload thất bại", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", (d, w) -> { isSelectingImage = false; rollbackImage(); })
                .show();
    }

    private void rollbackImage() {
        if (currentType == TYPE_AVATAR)
            Glide.with(requireContext()).load(oldAvatarUrl).placeholder(R.drawable.ic_profile).into(imgAvatar);
        else
            Glide.with(requireContext()).load(oldCoverUrl).placeholder(R.drawable.bg_cover_default).into(imgCover);
    }

    private void selectTab(View activeLine) {
        lineAll.setVisibility(View.INVISIBLE);
        lineFriends.setVisibility(View.INVISIBLE);
        linePic.setVisibility(View.INVISIBLE);
        activeLine.setVisibility(View.VISIBLE);
    }

    private void setOptional(TextView tv, String text) {
        if (tv == null) return;
        if (text != null && !text.isEmpty()) { tv.setVisibility(View.VISIBLE); tv.setText(text); }
        else tv.setVisibility(View.GONE);
    }

}


