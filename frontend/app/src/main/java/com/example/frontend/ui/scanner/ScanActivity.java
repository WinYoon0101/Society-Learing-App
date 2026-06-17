package com.example.frontend.ui.scanner;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.frontend.R;

public class ScanActivity extends AppCompatActivity {

    private PhotoView imagePreview;
    private LinearLayout layoutPlaceholder;
    private MaterialButton btnGallery, btnCamera, btnScan;
    private LinearProgressIndicator progressBar;

    private Uri currentImageUri;
    private Uri highResImageUri; // Biến hứng ảnh sắc nét
    private TextRecognizer recognizer;

    // 1. Trình xử lý chọn ảnh từ Thư viện
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    startCrop(selectedUri);
                }
            }
    );

    // 2. Trình xử lý chụp ảnh từ Camera (BẢN SẮC NÉT)
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Ứng dụng Camera đã chụp xong và lưu ảnh gốc sắc nét vào highResImageUri
                    if (highResImageUri != null) {
                        startCrop(highResImageUri);
                    }
                } else {
                    Toast.makeText(this, "Đã hủy chụp ảnh", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // 3. Trình xử lý xin quyền Camera
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Bạn cần cấp quyền để sử dụng Camera!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        imagePreview = findViewById(R.id.image_preview);
        layoutPlaceholder = findViewById(R.id.layout_placeholder);
        btnGallery = findViewById(R.id.btn_gallery);
        btnCamera = findViewById(R.id.btn_camera);
        btnScan = findViewById(R.id.btn_scan);
        progressBar = findViewById(R.id.progress_bar);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Mở Thư viện
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // Mở Camera (Có kiểm tra quyền)
        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnScan.setOnClickListener(v -> processImage());
    }

    // Hàm mở Camera và ép lưu ảnh gốc sắc nét
    private void openCamera() {
        try {
            // Tạo thông tin file ảnh mới
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "SmartScan_" + System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DESCRIPTION, "Ảnh chụp chất lượng cao cho OCR");

            // Yêu cầu hệ thống cấp một đường dẫn Uri hợp lệ để lưu ảnh gốc
            highResImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // LỆNH QUAN TRỌNG NHẤT: Ép Camera lưu ảnh full HD vào đường dẫn vừa tạo
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, highResImageUri);

            cameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: Không thể mở Camera trên máy này!", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCrop(Uri uri) {
        String destFileName = "cropped_scan_" + System.currentTimeMillis() + ".jpg";
        UCrop.Options options = new UCrop.Options();
        options.setFreeStyleCropEnabled(true);
        options.setToolbarColor(android.graphics.Color.parseColor("#10B981"));
        options.setStatusBarColor(android.graphics.Color.parseColor("#059669"));

        UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destFileName)))
                .withOptions(options)
                .start(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            currentImageUri = UCrop.getOutput(data);
            imagePreview.setImageURI(currentImageUri);

            layoutPlaceholder.setVisibility(View.GONE);
            btnScan.setVisibility(View.VISIBLE);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(this, "Lỗi cắt ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void processImage() {
        if (currentImageUri == null) return;

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentImageUri);
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            progressBar.setVisibility(View.VISIBLE);
            btnScan.setEnabled(false);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        progressBar.setVisibility(View.GONE);
                        btnScan.setEnabled(true);

                        String resultText = formatAndKeepIndentation(visionText);
                        boolean isCode = detectCode(resultText);

                        Intent intent = new Intent(ScanActivity.this, ScanResultActivity.class);
                        intent.putExtra("OCR_TEXT", resultText);
                        intent.putExtra("IS_CODE", isCode);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnScan.setEnabled(true);
                        Toast.makeText(this, "Không thể nhận diện chữ", Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatAndKeepIndentation(Text visionText) {
        StringBuilder formattedText = new StringBuilder();
        List<Integer> leftCoords = new ArrayList<>();

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect rect = line.getBoundingBox();
                if (rect != null) leftCoords.add(rect.left);
            }
        }
        if (leftCoords.isEmpty()) return "";

        int minX = Collections.min(leftCoords);

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect rect = line.getBoundingBox();
                if (rect != null) {
                    int diffX = rect.left - minX;
                    int spaceCount = Math.max(0, diffX / 25);
                    String spaces = new String(new char[spaceCount]).replace("\0", " ");
                    formattedText.append(spaces).append(line.getText()).append("\n");
                }
            }
            formattedText.append("\n");
        }
        return formattedText.toString().trim();
    }

    private boolean detectCode(String text) {
        String codeChars = "{}();=<>[]";
        int specialCount = 0;
        for (char ch : text.toCharArray()) {
            if (codeChars.indexOf(ch) >= 0) specialCount++;
        }

        // 2. Danh sách từ khóa
        String[] keywords = {
                "public", "class", "def", "import", "void", "function",
                "var", "let", "const", "return", "if", "else", "for", "while", "print"
        };

        String regex = "\\b(" + String.join("|", keywords) + ")\\b";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);

        int keywordCount = 0;
        while (matcher.find()) {
            keywordCount++;
        }

        // 3. Logic đánh giá
        return (keywordCount >= 1 && specialCount >= 2) || (specialCount >= 5);
    }
}