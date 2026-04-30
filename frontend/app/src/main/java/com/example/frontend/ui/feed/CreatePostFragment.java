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

import java.util.ArrayList;
import java.util.List;

public class CreatePostFragment extends Fragment {

    private EditText edtContent;
    private ImageView btnBack;
    private Button btnPost;
    private LinearLayout btnPickImage;

    // View và List chứa nhiều ảnh
    private RecyclerView rvImagePreview;
    private ImagePreviewAdapter previewAdapter;
    private List<Uri> selectedImageUris = new ArrayList<>();

    // View cho Profile
    private TextView tvUserName;
    private ImageView imgAvatar;

    private CreatePostViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed_create_post, container, false);

        // 1. Ánh xạ các View cơ bản
        edtContent = view.findViewById(R.id.edtContent);
        btnPost = view.findViewById(R.id.btnPost);
        btnPickImage = view.findViewById(R.id.optImage);
        btnBack = view.findViewById(R.id.btnClose);
        tvUserName = view.findViewById(R.id.tvUserName);
        imgAvatar = view.findViewById(R.id.imgAvatar);

        // =======================================================
        // 2. LẤY THÔNG TIN USER TỪ SHAREDPREFERENCES ĐỂ HIỂN THỊ
        // =======================================================
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String myUsername = prefs.getString("USERNAME", "Người dùng");
        String myAvatarUrl = prefs.getString("USER_AVATAR", "");

        if (tvUserName != null) {
            tvUserName.setText(myUsername);
        }

        if (!myAvatarUrl.isEmpty() && imgAvatar != null) {
            Glide.with(this)
                    .load(myAvatarUrl)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(imgAvatar);
        }

        // =======================================================
        // 3. SETUP RECYCLERVIEW HIỂN THỊ ẢNH PREVIEW (VUỐT NGANG)
        // =======================================================
        rvImagePreview = view.findViewById(R.id.rvImagePreview);

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
                // Nơi đây sẽ xử lý mở màn hình Full Screen ở các bước sau
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

        btnPost.setOnClickListener(v -> {
            String content = edtContent.getText().toString().trim();
            if (content.isEmpty() && selectedImageUris.isEmpty()) {
                Toast.makeText(getContext(), "Hãy nhập nội dung hoặc chọn ảnh nhé!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(getContext(), "Đang đăng bài...", Toast.LENGTH_SHORT).show();

            // TẠM THỜI: Chỉ bốc ảnh đầu tiên ra gửi đi để không làm vỡ Backend cũ.
            Uri firstImage = selectedImageUris.isEmpty() ? null : selectedImageUris.get(0);
            viewModel.uploadPost(getContext(), content, firstImage);
        });

        return view;
    }

    // =======================================================
    // 5. LẮNG NGHE KẾT QUẢ TỪ VIEWMODEL
    // =======================================================
    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnPost.setEnabled(!isLoading);
        });

        viewModel.getIsSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess) {
                Toast.makeText(getContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
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
}