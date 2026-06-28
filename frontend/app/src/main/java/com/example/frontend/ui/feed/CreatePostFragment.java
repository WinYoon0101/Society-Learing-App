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
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.User;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.utils.Result;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class CreatePostFragment extends Fragment {
    private EditText edtContent;
    private ImageView btnBack;
    private Button btnPost;
    private LinearLayout btnPickImage, optFeeling, optTag;
    private LinearLayout btnPrivacy;
    private TextView tvPrivacyText;
    private ImageView imgPrivacyIcon;
    private String selectedPrivacy = "Public";
    private RecyclerView rvImagePreview;
    private ImagePreviewAdapter previewAdapter;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private TextView tvUserName, tvSelectedMeta;
    private ImageView imgAvatar;
    private CreatePostViewModel viewModel;
    private String groupId;
    private List<User> selectedTags = new ArrayList<>();
    private String selectedFeeling = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed_create_post, container, false);
        if (getArguments() != null) groupId = getArguments().getString("groupId");

        edtContent = view.findViewById(R.id.edtContent);
        btnPost = view.findViewById(R.id.btnPost);
        btnPickImage = view.findViewById(R.id.optImage);
        optFeeling = view.findViewById(R.id.optFeeling);
        optTag = view.findViewById(R.id.optTag);
        tvSelectedMeta = view.findViewById(R.id.tvSelectedMeta);
        btnBack = view.findViewById(R.id.btnClose);
        tvUserName = view.findViewById(R.id.tvUserName);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        btnPrivacy = view.findViewById(R.id.btnPrivacy);
        tvPrivacyText = view.findViewById(R.id.tvPrivacyText);
        imgPrivacyIcon = view.findViewById(R.id.imgPrivacyIcon);

        if (btnPrivacy != null) btnPrivacy.setOnClickListener(v -> showPrivacyDialog());

        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String myUsername = prefs.getString("USERNAME", "Người dùng");
        String myAvatarUrl = prefs.getString("USER_AVATAR", "");
        if (tvUserName != null) tvUserName.setText(myUsername);
        if (imgAvatar != null && !myAvatarUrl.isEmpty()) {
            Glide.with(this).load(myAvatarUrl).placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(imgAvatar);
        } else loadProfileFallback();

        rvImagePreview = view.findViewById(R.id.rvImagePreview);
        rvImagePreview.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        previewAdapter = new ImagePreviewAdapter(getContext(), selectedMediaUris, new ImagePreviewAdapter.OnImageClickListener() {
            @Override
            public void onRemove(int position) {
                selectedMediaUris.remove(position);
                previewAdapter.notifyItemRemoved(position);
                previewAdapter.notifyItemRangeChanged(position, selectedMediaUris.size());
                if (selectedMediaUris.isEmpty()) rvImagePreview.setVisibility(View.GONE);
            }
            @Override
            public void onImageClick(int position) {}
        });
        rvImagePreview.setAdapter(previewAdapter);

        viewModel = new ViewModelProvider(this).get(CreatePostViewModel.class);
        observeViewModel();

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
            else if (getActivity() != null) getActivity().finish();
        });

        btnPickImage.setOnClickListener(v -> mediaPickerLauncher.launch(new String[]{"image/*", "video/*"}));

        if (optFeeling != null) {
            optFeeling.setOnClickListener(v -> {
                FeelingBottomSheet bottomSheet = FeelingBottomSheet.newInstance();
                bottomSheet.setReactionListener(reactionType -> {
                    selectedFeeling = reactionType;
                    updateMetaText();
                });
                bottomSheet.show(getParentFragmentManager(), "FeelingBottomSheet");
            });
        }

        if (optTag != null) optTag.setOnClickListener(v -> openTagDialog());

        btnPost.setOnClickListener(v -> {
            String content = edtContent.getText().toString().trim();
            if (content.isEmpty() && selectedMediaUris.isEmpty()) {
                Toast.makeText(getContext(), "Hãy nhập nội dung nhé!", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(getContext(), "Đang đăng bài...", Toast.LENGTH_SHORT).show();
            List<String> tagIds = new ArrayList<>();
            for (User u : selectedTags) tagIds.add(u.getId());
            viewModel.uploadPost(getContext(), content, selectedPrivacy, selectedFeeling, selectedMediaUris, groupId, tagIds, null);
        });

        return view;
    }

    private void showPrivacyDialog() {
        String[] options = {"Công khai", "Bạn bè", "Chỉ mình tôi"};
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Ai có thể xem bài viết này?")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { selectedPrivacy = "Public"; if (tvPrivacyText != null) tvPrivacyText.setText("Công khai"); if (imgPrivacyIcon != null) imgPrivacyIcon.setImageResource(R.drawable.ic_public); }
                    else if (which == 1) { selectedPrivacy = "Friends"; if (tvPrivacyText != null) tvPrivacyText.setText("Bạn bè"); if (imgPrivacyIcon != null) imgPrivacyIcon.setImageResource(R.drawable.ic_friend); }
                    else { selectedPrivacy = "Private"; if (tvPrivacyText != null) tvPrivacyText.setText("Chỉ mình tôi"); if (imgPrivacyIcon != null) imgPrivacyIcon.setImageResource(R.drawable.ic_private); }
                }).show();
    }

    private void updateMetaText() {
        StringBuilder s = new StringBuilder();
        if (selectedFeeling != null) {
            s.append("Đang cảm thấy ").append(getFeelingText(selectedFeeling));
        }
        if (!selectedTags.isEmpty()) {
            if (s.length() > 0) s.append("\n");
            s.append("— Cùng với ").append(selectedTags.get(0).getUsername());
            if (selectedTags.size() > 1) s.append(" và ").append(selectedTags.size() - 1).append(" người khác");
        }
        tvSelectedMeta.setText(s.toString());
        tvSelectedMeta.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private String getFeelingText(String type) {
        switch (type) {
            case "Like": return "tuyệt vời 👍";
            case "Love": return "được yêu ❤️";
            case "Haha": return "vui vẻ 😆";
            case "Wow": return "ngạc nhiên 😮";
            case "Sad": return "buồn 😢";
            case "Angry": return "tức giận 😡";
            case "Lucky": return "may mắn 🍀";
            case "Loved": return "đong đầy tình yêu 🥰";
            case "Sick": return "mệt mỏi 🤒";
            case "Question": return "tò mò 🤔";
            case "Cool": return "rất ngầu 😎";
            case "Smart": return "thông minh 🧠";
            default: return type;
        }
    }

    private void loadFriends(UserSearchAdapter adapter) {
        ApiClient.getApiService(requireContext()).getFriends()
                .enqueue(new retrofit2.Callback<ApiResponse<List<Friend>>>() {
                    @Override
                    public void onResponse(retrofit2.Call<ApiResponse<List<Friend>>> call,
                                           retrofit2.Response<ApiResponse<List<Friend>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<Friend> friends = response.body().getData();
                            List<User> friendUsers = new ArrayList<>();
                            if (friends != null) {
                                for (Friend f : friends) {
                                    friendUsers.add(new User(f.getId(), f.getUsername(), f.getAvatar()));
                                }
                            }
                            adapter.updateData(friendUsers);
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<ApiResponse<List<Friend>>> call, Throwable t) {}
                });
    }

    // 👉 ĐÃ FIX: Sử dụng BottomSheetDialog chuẩn thay cho AlertDialog
    private void openTagDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_user_search, null);
        bottomSheetDialog.setContentView(dialogView);

        EditText edtSearch = dialogView.findViewById(R.id.edtSearchUser);
        RecyclerView rvResults = dialogView.findViewById(R.id.rvUserResults);
        Button btnDone = dialogView.findViewById(R.id.btnDoneSearch);
        TextView tvTagCount = dialogView.findViewById(R.id.tvTagCount);

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        UserSearchAdapter adapter = new UserSearchAdapter(new ArrayList<>(), selectedTags);
        rvResults.setAdapter(adapter);

        if (tvTagCount != null) tvTagCount.setText(selectedTags.size() + " đã chọn");

        adapter.setOnSelectionChangedListener(count -> {
            if (tvTagCount != null) tvTagCount.setText(count + " đã chọn");
        });

        loadFriends(adapter);

        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim();
                if (!q.isEmpty()) {
                    ApiClient.getApiService(requireContext()).searchUsers(q)
                            .enqueue(new retrofit2.Callback<ApiResponse<List<User>>>() {
                                @Override public void onResponse(retrofit2.Call<ApiResponse<List<User>>> call,
                                                                 retrofit2.Response<ApiResponse<List<User>>> response) {
                                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                                        adapter.updateData(response.body().getData());
                                }
                                @Override public void onFailure(retrofit2.Call<ApiResponse<List<User>>> call, Throwable t) {}
                            });
                } else {
                    loadFriends(adapter);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnDone.setOnClickListener(v -> {
            selectedTags.clear();
            selectedTags.addAll(adapter.getSelectedUsers());
            updateMetaText();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> btnPost.setEnabled(!isLoading));
        viewModel.getIsSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (Boolean.TRUE.equals(isSuccess)) {
                viewModel.resetSuccess();
                String successMsg = viewModel.getSuccessMessage().getValue();
                Toast.makeText(getContext(), successMsg != null && !successMsg.isEmpty() ? successMsg : "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
                else if (getActivity() != null) { getActivity().setResult(android.app.Activity.RESULT_OK); getActivity().finish(); }
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private final ActivityResultLauncher<String[]> mediaPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (isSupportedMedia(uri)) selectedMediaUris.add(uri);
                    }
                    while (selectedMediaUris.size() > 10) {
                        selectedMediaUris.remove(selectedMediaUris.size() - 1);
                    }
                    previewAdapter.notifyDataSetChanged();
                    rvImagePreview.setVisibility(View.VISIBLE);
                }
            }
    );

    private boolean isSupportedMedia(Uri uri) {
        String mimeType = requireContext().getContentResolver().getType(uri);
        return mimeType != null && (mimeType.startsWith("image/") || mimeType.startsWith("video/"));
    }

    private void loadProfileFallback() {
        UserRepository repository = new UserRepository(requireContext());
        repository.getProfile().observe(getViewLifecycleOwner(), result -> {
            if (result.status == Result.Status.SUCCESS && result.data != null) {
                User user = result.data;
                SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                    editor.putString("USERNAME", user.getUsername());
                    if (tvUserName != null) tvUserName.setText(user.getUsername());
                }
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    editor.putString("USER_AVATAR", user.getAvatar());
                    if (imgAvatar != null) Glide.with(this).load(user.getAvatar()).into(imgAvatar);
                }
                editor.apply();
            }
        });
    }
}
