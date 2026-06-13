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

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

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


        // Khởi tạo Select Box cho Môn học
        AutoCompleteTextView etSubject = findViewById(R.id.etSubject);
        String[] subjects = {"CNTT/IT", "Kinh tế", "Khoa học", "Luật"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                subjects
        );
        etSubject.setAdapter(adapter);
        etSubject.setText(subjects[0], false);


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

        AutoCompleteTextView etSubject = findViewById(R.id.etSubject);
        String selectedSubject = etSubject.getText().toString().trim();


        if (selectedFileUri == null || title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề và chọn file!", Toast.LENGTH_SHORT).show();
            return;
        }


        if (selectedSubject.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn môn học!", Toast.LENGTH_SHORT).show();
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

            Toast.makeText(this, "Lỗi: Không đọc được file!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xác định MIME type an toàn
        String mimeType = getContentResolver().getType(selectedFileUri);
        if (mimeType == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(selectedFileUri.toString());
            if (extension != null && !extension.isEmpty()) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        RequestBody requestFile = RequestBody.create(
                MediaType.parse(mimeType),

                file
        );
        MultipartBody.Part body = MultipartBody.Part.createFormData("media", file.getName(), requestFile);
        RequestBody sourceType = RequestBody.create(MediaType.parse("text/plain"), "document");
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

        AutoCompleteTextView etSubject = findViewById(R.id.etSubject);
        String selectedSubject = etSubject.getText().toString().trim();

        // Map "CNTT/IT" sang "IT" để khớp với chip/tab IT của LibraryFragment
        String subjectValue = selectedSubject;
        if ("CNTT/IT".equals(selectedSubject)) {
            subjectValue = "IT";
        }


        Map<String, Object> docData = new HashMap<>();
        docData.put("mediaId", mediaId);
        docData.put("title", title);

        docData.put("subject", subjectValue);

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

                    Toast.makeText(UploadDocumentActivity.this, "Lỗi tạo tài liệu: " + response.code(), Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                btnUpload.setEnabled(true);
                Log.e("UPLOAD_FLOW", "Thất bại bước 2: " + t.getMessage());

                Toast.makeText(UploadDocumentActivity.this, "Thất bại khi lưu tài liệu: " + t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private File uriToFile(Uri uri) {
        try {

            // Lấy đuôi file thật từ Uri
            String extension = null;
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            }
            if (extension == null) {
                // Thử lấy extension từ uri path
                String path = uri.getPath();
                if (path != null) {
                    int lastDot = path.lastIndexOf('.');
                    if (lastDot != -1) {
                        extension = path.substring(lastDot + 1);
                    }
                }
            }
            if (extension == null || extension.isEmpty()) {
                extension = "pdf"; // Mặc định là pdf nếu không nhận diện được
            }

            // Tạo file tạm CÓ ĐUÔI FILE (Rất quan trọng)
            File file = new File(getCacheDir(), "upload_file_" + System.currentTimeMillis() + "." + extension);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();

            outputStream.close();
            inputStream.close();
            return file;
        } catch (Exception e) {
            Log.e("UPLOAD_FLOW", "Lỗi chuyển đổi URI sang File: " + e.getMessage());

            return null;
        }
    }
}