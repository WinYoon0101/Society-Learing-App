package com.example.frontend.ui.scanner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.regex.Pattern;

import com.amrdeveloper.codeview.CodeView;
import com.example.frontend.R;

public class ScanResultActivity extends AppCompatActivity {

    private EditText etPlainText;
    private CodeView codeView;
    private String ocrText;
    private boolean isCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);

        ocrText = getIntent().getStringExtra("OCR_TEXT");
        isCode = getIntent().getBooleanExtra("IS_CODE", false);

        etPlainText = findViewById(R.id.et_plain_text);
        codeView = findViewById(R.id.code_view);
        MaterialButton btnCopy = findViewById(R.id.btn_copy);
        MaterialButton btnCreatePost = findViewById(R.id.btn_create_post);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_result);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(isCode ? "Nhận diện Code" : "Nhận diện Văn bản");

        if (isCode) {
            setupCodeEditor();
        } else {
            setupPlainEditor();
        }

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            String finalTxt = isCode ? codeView.getText().toString() : etPlainText.getText().toString();
            clipboard.setPrimaryClip(ClipData.newPlainText("Code", finalTxt));
            Toast.makeText(this, "Đã sao chép vào khay nhớ tạm!", Toast.LENGTH_SHORT).show();
        });

        btnCreatePost.setOnClickListener(v -> {
            // 1. Lấy text mới nhất đang hiển thị
            String finalTxt = isCode ? codeView.getText().toString() : etPlainText.getText().toString();

            // 2. Tạo Intent gọi đến màn hình CreatePostActivity
            Intent intent = new Intent(ScanResultActivity.this, com.example.frontend.ui.feed.CreatePostActivity.class);

            // 3. Đóng gói đoạn chữ vừa quét được để gửi đi theo
            intent.putExtra("SCANNED_CONTENT", finalTxt);

            startActivity(intent);
        });
    }

    private void setupPlainEditor() {
        etPlainText.setVisibility(View.VISIBLE);
        codeView.setVisibility(View.GONE);
        etPlainText.setText(ocrText);
    }

    private void setupCodeEditor() {
        codeView.setVisibility(View.VISIBLE);
        etPlainText.setVisibility(View.GONE);

        // --- BẢNG MÀU  ---
        int colorKeyword    = 0xFFFF79C6; // Hồng: public, return, if...
        int colorType       = 0xFF8BE9FD; // Xanh lơ: int, String, boolean...
        int colorMethod     = 0xFF50FA7B; // Xanh lá: tên hàm (ví dụ: onCreate)
        int colorAnnotation = 0xFFFFB86C; // Cam: @Override, @Nullable...
        int colorNumber     = 0xFFBD93F9; // Tím: Số 0-9
        int colorString     = 0xFFF1FA8C; // Vàng: Chuỗi "Hello"
        int colorComment    = 0xFF6272A4; // Xám: // Chú thích

        // 1. Từ khóa điều khiển (Control & Modifiers)
        String[] keywords = {
                "def", "import", "class", "return", "if", "else", "for", "while",
                "public", "private", "protected", "void", "static", "final", "new",
                "let", "const", "var", "function", "extends", "implements", "this", "super"
        };

        // 2. Kiểu dữ liệu cơ bản (Data Types)
        String[] types = {
                "int", "float", "double", "boolean", "char", "String", "Object", "List", "Map", "HashMap"
        };

        // --- ADD CÁC PATTERN TÔ MÀU ---

        // 1. Chú thích (Comment) - Ưu tiên cao nhất để không bị đè màu
        codeView.addSyntaxPattern(Pattern.compile("//.*"), colorComment);
        codeView.addSyntaxPattern(Pattern.compile("#.*"), colorComment);
        codeView.addSyntaxPattern(Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL), colorComment);

        // 2. Chuỗi (String)
        codeView.addSyntaxPattern(Pattern.compile("\"[^\"]*\""), colorString);
        codeView.addSyntaxPattern(Pattern.compile("'[^']*'"), colorString);

        // 3. Annotation (@Override, @NonNull...)
        codeView.addSyntaxPattern(Pattern.compile("@\\w+"), colorAnnotation);

        // 4. Tên hàm (Nhận diện các từ đứng ngay trước dấu ngoặc đơn mở "(" )
        codeView.addSyntaxPattern(Pattern.compile("\\b\\w+(?=\\()"), colorMethod);

        // 5. Từ khóa & Kiểu dữ liệu
        codeView.addSyntaxPattern(Pattern.compile("\\b(" + String.join("|", keywords) + ")\\b"), colorKeyword);
        codeView.addSyntaxPattern(Pattern.compile("\\b(" + String.join("|", types) + ")\\b"), colorType);

        // 6. Chữ số (Numbers)
        codeView.addSyntaxPattern(Pattern.compile("\\b\\d+\\b"), colorNumber);


        codeView.setText(ocrText);
    }
}