package com.example.frontend.ui.feed;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.frontend.R;

public class CreatePostFragment extends Fragment {

    private EditText edtContent;
    private ImageView imgPreview;
    private ImageView btnBack;
    private Button btnPost;
    private LinearLayout btnPickImage;
    private Uri selectedImageUri;

    // 1. Khai báo ViewModel
    private CreatePostViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed_create_post, container, false);

        edtContent = view.findViewById(R.id.edtContent);
        imgPreview = view.findViewById(R.id.imgPreview);
        btnPost = view.findViewById(R.id.btnPost);
        btnPickImage = view.findViewById(R.id.optImage);
        btnBack = view.findViewById(R.id.btnClose);

        // 2. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(CreatePostViewModel.class);

        // 3. Quan sát các trạng thái từ ViewModel (Observer Pattern)
        observeViewModel();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnPost.setOnClickListener(v -> {
            String content = edtContent.getText().toString().trim();
            if (content.isEmpty() && selectedImageUri == null) {
                Toast.makeText(getContext(), "Hãy nhập nội dung hoặc chọn ảnh nhé!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 4. Giao việc cho ViewModel xử lý API thay vì tự làm
            Toast.makeText(getContext(), "Đang đăng bài...", Toast.LENGTH_SHORT).show();
            viewModel.uploadPost(getContext(), content, selectedImageUri);
        });

        return view;
    }

    // Hàm quan sát: Lắng nghe tín hiệu trả về từ ViewModel
    private void observeViewModel() {
        // Nghe tín hiệu Loading (Để khóa/mở nút bấm)
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnPost.setEnabled(!isLoading);
        });

        // Nghe tín hiệu Thành công
        viewModel.getIsSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess) {
                Toast.makeText(getContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack(); // Quay về trang chủ
            }
        });

        // Nghe tín hiệu Lỗi
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imgPreview.setImageURI(selectedImageUri);
                    imgPreview.setVisibility(View.VISIBLE);
                }
            }
    );
}