package com.example.frontend.ui.story;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Story;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.FileUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateStoryActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private TextInputEditText edtCaption;
    private MaterialButton btnPost;
    private Uri selectedUri;
    private ApiService apiService;

    private final ActivityResultLauncher<String> pickLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedUri = uri;
                    Glide.with(this).load(uri).into(imgPreview);
                    imgPreview.setVisibility(View.VISIBLE);
                    btnPost.setEnabled(true);
                }
            });

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> {
                for (Boolean v : r.values()) { if (v) { openPicker(); return; } }
                Toast.makeText(this, "Cần cấp quyền truy cập ảnh", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_story);

        ImageButton btnBack = findViewById(R.id.btnBack);
        imgPreview          = findViewById(R.id.imgPreview);
        edtCaption          = findViewById(R.id.edtCaption);
        btnPost             = findViewById(R.id.btnPostStory);
        MaterialButton btnPickImage = findViewById(R.id.btnPickImage);

        apiService = ApiClient.getApiService(this);

        btnBack.setOnClickListener(v -> finish());
        btnPickImage.setOnClickListener(v -> checkPermAndPick());
        btnPost.setEnabled(false);
        btnPost.setOnClickListener(v -> uploadStory());
    }

    private void checkPermAndPick() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openPicker();
        } else {
            permLauncher.launch(new String[]{perm});
        }
    }

    private void openPicker() { pickLauncher.launch("image/*"); }

    private void uploadStory() {
        if (selectedUri == null) return;
        File file = FileUtils.getFileFromUri(this, selectedUri);
        if (file == null) {
            Toast.makeText(this, "Không đọc được file", Toast.LENGTH_SHORT).show();
            return;
        }
        btnPost.setEnabled(false);
        btnPost.setText("Đang đăng...");

        String captionText = edtCaption.getText() != null ? edtCaption.getText().toString() : "";
        RequestBody captionBody = RequestBody.create(MediaType.parse("text/plain"), captionText);
        RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        apiService.createStory(filePart, captionBody).enqueue(new Callback<ApiResponse<Story>>() {
            @Override
            public void onResponse(Call<ApiResponse<Story>> call,
                                   Response<ApiResponse<Story>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    Toast.makeText(CreateStoryActivity.this,
                            "Đã đăng tin!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    btnPost.setEnabled(true);
                    btnPost.setText("Đăng tin");
                    Toast.makeText(CreateStoryActivity.this, "Đăng thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Story>> call, Throwable t) {
                btnPost.setEnabled(true);
                btnPost.setText("Đăng tin");
                Toast.makeText(CreateStoryActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
