package com.example.frontend.ui.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Media;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadDocumentActivity extends AppCompatActivity {
    private Uri selectedFileUri;
    private TextView tvFileName;
    private ApiService apiService;
    private View btnUpload; // Ánh xạ nút để điều khiển enable/disable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_document);

        View btnClose = findViewById(R.id.btnClose);
        btnUpload = findViewById(R.id.btnUpload);
        tvFileName = findViewById(R.id.tvFileName);

        apiService = ApiClient.getApiService(this);

        btnClose.setOnClickListener(v -> finish());

        // 1. Bộ chọn File
        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        // Hiển thị tên file cho user an tâm
                        tvFileName.setText("Đã chọn: " + selectedFileUri.getPath());
                    }
                }
        );

        findViewById(R.id.btnSelectFile).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

        btnUpload.setOnClickListener(v -> startUploadFlow());
    }

    private void startUploadFlow() {
        // 1. Kiểm tra đầu vào
        String title = ((EditText)findViewById(R.id.etTitle)).getText().toString().trim();
        if (selectedFileUri == null || title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề và chọn file!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Lấy USER_ID thật từ máy (Tránh dùng ID dummy làm server từ chối)
        SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String userId = sharedPref.getString("USER_ID", "");

        if (userId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Bạn cần đăng nhập lại!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Vô hiệu hóa nút để tránh bấm 2 lần gây loạn
        btnUpload.setEnabled(false);
        Toast.makeText(this, "Đang xử lý, vui lòng đợi...", Toast.LENGTH_SHORT).show();

        // Bước 1: Upload Media lên Cloudinary qua Backend
        File file = uriToFile(selectedFileUri);
        if (file == null) {
            btnUpload.setEnabled(true);
            return;
        }

        RequestBody requestFile = RequestBody.create(
                MediaType.parse(getContentResolver().getType(selectedFileUri)),
                file
        );
        MultipartBody.Part body = MultipartBody.Part.createFormData("media", file.getName(), requestFile);
        RequestBody sourceType = RequestBody.create(MediaType.parse("text/plain"), "post");
        RequestBody targetIdBody = RequestBody.create(MediaType.parse("text/plain"), userId);

        Log.d("UPLOAD_FLOW", "Bắt đầu bước 1: Upload Media...");

        apiService.uploadSingleFile(body, sourceType, targetIdBody).enqueue(new Callback<ApiResponse<Media>>() {
            @Override
            public void onResponse(Call<ApiResponse<Media>> call, Response<ApiResponse<Media>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String mediaId = response.body().getData().get_id();
                    Log.d("UPLOAD_FLOW", "Thành công bước 1! mediaId: " + mediaId);
                    createDocumentRecord(mediaId); // Sang bước 2
                } else {
                    btnUpload.setEnabled(true);
                    Log.e("UPLOAD_FLOW", "Lỗi bước 1: " + response.code());
                    Toast.makeText(UploadDocumentActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Media>> call, Throwable t) {
                btnUpload.setEnabled(true);
                Log.e("UPLOAD_FLOW", "Thất bại bước 1: " + t.getMessage());
                Toast.makeText(UploadDocumentActivity.this, "Mất kết nối server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createDocumentRecord(String mediaId) {
        String title = ((EditText)findViewById(R.id.etTitle)).getText().toString().trim();
        String subject = ((EditText)findViewById(R.id.etSubject)).getText().toString().trim();

        Map<String, Object> docData = new HashMap<>();
        docData.put("mediaId", mediaId);
        docData.put("title", title);
        docData.put("subject", subject);
        docData.put("visibility", "public");

        Log.d("UPLOAD_FLOW", "Bắt đầu bước 2: Tạo Document...");

        apiService.createDocument(docData).enqueue(new Callback<ApiResponse<com.example.frontend.data.model.Document>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.example.frontend.data.model.Document>> call, Response<ApiResponse<com.example.frontend.data.model.Document>> response) {
                btnUpload.setEnabled(true);
                if (response.isSuccessful()) {
                    Log.d("UPLOAD_FLOW", "Hoàn tất! Đã tạo Document.");
                    Toast.makeText(UploadDocumentActivity.this, "Đăng tài liệu thành công!", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    Log.e("UPLOAD_FLOW", "Lỗi bước 2: " + response.code());
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                btnUpload.setEnabled(true);
                Log.e("UPLOAD_FLOW", "Thất bại bước 2: " + t.getMessage());
            }
        });
    }

    private File uriToFile(Uri uri) {
        try {
            // 1. Lấy đuôi file thật từ Uri (ví dụ: pdf, docx)
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            if (extension == null) extension = "pdf"; // Mặc định là pdf nếu không nhận diện được

            // 2. Tạo file tạm CÓ ĐUÔI FILE (Rất quan trọng)
            File file = new File(getCacheDir(), "upload_file_" + System.currentTimeMillis() + "." + extension);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            return file; // Bây giờ file gửi lên server sẽ có tên như: upload_file_123.pdf
        } catch (Exception e) {
            return null;
        }
    }
}