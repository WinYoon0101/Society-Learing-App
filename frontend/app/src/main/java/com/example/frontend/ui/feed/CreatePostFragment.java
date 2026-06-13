package com.example.frontend.ui.feed;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.widget.PopupWindow;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.User;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.utils.Result;

import java.util.ArrayList;
import java.util.List;

public class CreatePostFragment extends Fragment {

    private EditText edtContent;
    private ImageView btnBack;
    private Button btnPost;
    private LinearLayout btnPickImage;
    private LinearLayout optFeeling, optTag;

    // View và List chứa nhiều ảnh
    private RecyclerView rvImagePreview;
    private ImagePreviewAdapter previewAdapter;
    private List<Uri> selectedImageUris = new ArrayList<>();

    // View cho Profile
    private TextView tvUserName;
    private ImageView imgAvatar;

    private CreatePostViewModel viewModel;
    private String groupId;
    private TextView tvSelectedMeta;
    private List<User> selectedTags = new ArrayList<>();
    private String selectedReaction = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed_create_post, container, false);

        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
        }

        // 1. Ánh xạ các View cơ bản
        edtContent = view.findViewById(R.id.edtContent);
        btnPost = view.findViewById(R.id.btnPost);
        btnPickImage = view.findViewById(R.id.optImage);
        optFeeling = view.findViewById(R.id.optFeeling);
        optTag = view.findViewById(R.id.optTag);
        tvSelectedMeta = view.findViewById(R.id.tvSelectedMeta);
        btnBack = view.findViewById(R.id.btnClose);
        tvUserName = view.findViewById(R.id.tvUserName);
        imgAvatar = view.findViewById(R.id.imgAvatar);

        // =======================================================
        // 2. LẤY THÔNG TIN USER TỪ SHAREDPREFERENCES ĐỂ HIỂN THỊ
        // =======================================================
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String myUsername = prefs.getString("USERNAME", "Người dùng");
        String myAvatarUrl = prefs.getString("USER_AVATAR", "");

        if (!myUsername.isEmpty() && tvUserName != null) {
            tvUserName.setText(myUsername);
        }

        if (!myAvatarUrl.isEmpty() && imgAvatar != null) {
            Glide.with(this)
                    .load(myAvatarUrl)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(imgAvatar);
        } else {
            loadProfileFallback();
        }

        // =======================================================
        // 3. SETUP RECYCLERVIEW HIỂN THỊ ẢNH PREVIEW (VUỐT NGANG)
        // =======================================================
        rvImagePreview = view.findViewById(R.id.rvImagePreview); // Chú ý: Đảm bảo layout fragment_feed_create_post.xml đã có id này

        // Cài đặt dạng danh sách vuốt ngang
        rvImagePreview.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        previewAdapter = new ImagePreviewAdapter(getContext(), selectedImageUris, new ImagePreviewAdapter.OnImageClickListener() {
            @Override
            public void onRemove(int position) {
                selectedImageUris.remove(position);
                // Dùng các hàm notify này để có hiệu ứng thu hồi ảnh cực mượt
                previewAdapter.notifyItemRemoved(position);
                previewAdapter.notifyItemRangeChanged(position, selectedImageUris.size());

                if (selectedImageUris.isEmpty()) {
                    rvImagePreview.setVisibility(View.GONE);
                }
            }

            @Override
            public void onImageClick(int position) {
                Toast.makeText(getContext(), "Click xem ảnh thứ " + (position + 1), Toast.LENGTH_SHORT).show();
            }
        });
        rvImagePreview.setAdapter(previewAdapter);

        // =======================================================
        // 4. KHỞI TẠO VIEWMODEL & XỬ LÝ SỰ KIỆN NÚT BẤM
        // =======================================================
        viewModel = new ViewModelProvider(this).get(CreatePostViewModel.class);
        observeViewModel();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Gọi hàm chọn nhiều ảnh (Hỗ trợ GetMultipleContents)
        btnPickImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        if (optFeeling != null) {
            optFeeling.setOnClickListener(v -> {
                View popupView = LayoutInflater.from(getContext()).inflate(R.layout.item_feed_reaction_popup, null);
                final PopupWindow popupWindow = new PopupWindow(popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setOutsideTouchable(true);

                ImageView btnReactLike = popupView.findViewById(R.id.btnReactLike);
                ImageView btnReactLove = popupView.findViewById(R.id.btnReactLove);
                ImageView btnReactHaha = popupView.findViewById(R.id.btnReactHaha);
                ImageView btnReactWow = popupView.findViewById(R.id.btnReactWow);
                ImageView btnReactSad = popupView.findViewById(R.id.btnReactSad);
                ImageView btnReactAngry = popupView.findViewById(R.id.btnReactAngry);

                btnReactLike.setOnClickListener(x -> { selectedReaction = "Like"; updateMetaText(); popupWindow.dismiss(); });
                btnReactLove.setOnClickListener(x -> { selectedReaction = "Love"; updateMetaText(); popupWindow.dismiss(); });
                btnReactHaha.setOnClickListener(x -> { selectedReaction = "Haha"; updateMetaText(); popupWindow.dismiss(); });
                btnReactWow.setOnClickListener(x -> { selectedReaction = "Wow"; updateMetaText(); popupWindow.dismiss(); });
                btnReactSad.setOnClickListener(x -> { selectedReaction = "Sad"; updateMetaText(); popupWindow.dismiss(); });
                btnReactAngry.setOnClickListener(x -> { selectedReaction = "Angry"; updateMetaText(); popupWindow.dismiss(); });

                popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 100, 100);
            });
        }

        if (optTag != null) {
            optTag.setOnClickListener(v -> openTagDialog());
        }

        btnPost.setOnClickListener(v -> {
            String content = edtContent.getText().toString().trim();
            if (content.isEmpty() && selectedImageUris.isEmpty()) {
                Toast.makeText(getContext(), "Hãy nhập nội dung hoặc chọn ảnh nhé!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(getContext(), "Đang đăng bài...", Toast.LENGTH_SHORT).show();

            // Prepare tag ids
            List<String> tagIds = new ArrayList<>();
            for (User u : selectedTags) tagIds.add(u.getId());

            // Đã sửa: truyền groupId khi đăng bài vào nhóm, cùng tags và initialReaction
            if (groupId != null && !groupId.isEmpty()) {
                viewModel.uploadPost(getContext(), content, selectedImageUris, groupId, tagIds, selectedReaction);
            } else {
                viewModel.uploadPost(getContext(), content, selectedImageUris, null, tagIds, selectedReaction);
            }
        });

        return view;
    }

    private void updateMetaText() {
        StringBuilder s = new StringBuilder();
        if (selectedReaction != null) s.append("Cảm xúc: ").append(selectedReaction);
        if (!selectedTags.isEmpty()) {
            if (s.length() > 0) s.append(" · ");
            s.append("Đã gắn: ");
            for (int i = 0; i < selectedTags.size(); i++) {
                s.append(selectedTags.get(i).getUsername());
                if (i < selectedTags.size() - 1) s.append(", ");
            }
        }
        tvSelectedMeta.setText(s.toString());
        tvSelectedMeta.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private void openTagDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_user_search, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        EditText edtSearch = dialogView.findViewById(R.id.edtSearchUser);
        RecyclerView rvResults = dialogView.findViewById(R.id.rvUserResults);
        Button btnDone = dialogView.findViewById(R.id.btnDoneSearch);

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        UserSearchAdapter adapter = new UserSearchAdapter(new ArrayList<>(), selectedTags);
        rvResults.setAdapter(adapter);

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            String q = edtSearch.getText().toString().trim();
            if (!q.isEmpty()) {
                com.example.frontend.data.remote.ApiClient.getApiService(requireContext()).searchUsers(q)
                        .enqueue(new retrofit2.Callback<com.example.frontend.data.model.ApiResponse<java.util.List<User>>>() {
                            @Override
                            public void onResponse(retrofit2.Call<com.example.frontend.data.model.ApiResponse<java.util.List<User>>> call, retrofit2.Response<com.example.frontend.data.model.ApiResponse<java.util.List<User>>> response) {
                                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                    adapter.updateData(response.body().getData());
                                }
                            }

                            @Override
                            public void onFailure(retrofit2.Call<com.example.frontend.data.model.ApiResponse<java.util.List<User>>> call, Throwable t) {
                            }
                        });
            }
            return true;
        });

        btnDone.setOnClickListener(v -> {
            selectedTags = adapter.getSelectedUsers();
            updateMetaText();
            dialog.dismiss();
        });

        dialog.show();
    }

    // =======================================================
    // 5. LẮNG NGHE KẾT QUẢ TỪ VIEWMODEL
    // =======================================================
    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnPost.setEnabled(!isLoading);
        });

        viewModel.getIsSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (Boolean.TRUE.equals(isSuccess)) {
                viewModel.resetSuccess(); // Tránh re-trigger khi quay lại fragment
                Toast.makeText(getContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                // Nếu mở như Fragment (FeedFragment gọi), pop back stack
                // Nếu mở như Activity standalone, finish Activity
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().setResult(android.app.Activity.RESULT_OK);
                    getActivity().finish();
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =======================================================
    // 6. ACTIVITY LAUNCHER CHỌN NHIỀU ẢNH
    // =======================================================
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    selectedImageUris.addAll(uris);

                    // Giới hạn chọn tối đa 10 ảnh
                    if (selectedImageUris.size() > 10) {
                        selectedImageUris = selectedImageUris.subList(0, 10);
                        Toast.makeText(getContext(), "Chỉ được chọn tối đa 10 ảnh", Toast.LENGTH_SHORT).show();
                    }

                    previewAdapter.notifyDataSetChanged();
                    rvImagePreview.setVisibility(View.VISIBLE);
                }
            }
    );

    private void loadProfileFallback() {
        UserRepository repository = new UserRepository(requireContext());
        repository.getProfile().observe(getViewLifecycleOwner(), result -> {
            if (result.status == Result.Status.SUCCESS && result.data != null) {
                User user = result.data;
                String username = user.getUsername();
                String avatar = user.getAvatar();

                SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (username != null && !username.isEmpty()) {
                    editor.putString("USERNAME", username);
                    if (tvUserName != null) {
                        tvUserName.setText(username);
                    }
                }

                if (avatar != null && !avatar.isEmpty()) {
                    editor.putString("USER_AVATAR", avatar);
                    if (imgAvatar != null) {
                        Glide.with(this)
                                .load(avatar)
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .into(imgAvatar);
                    }
                }

                editor.apply();
            }
        });
    }
}