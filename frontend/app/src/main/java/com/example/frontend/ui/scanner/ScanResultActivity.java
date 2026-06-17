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
        codeView.setText(ocrText);

        int colorKeyword = 0xFFFF79C6;
        int colorBuiltin = 0xFF8BE9FD;
        int colorString = 0xFFF1FA8C;
        int colorComment = 0xFF6272A4;

        String[] keywords = {"def", "import", "class", "return", "if", "else", "for", "while", "public", "private", "void", "static"};

        codeView.addSyntaxPattern(Pattern.compile("\\b(" + String.join("|", keywords) + ")\\b"), colorKeyword);
        codeView.addSyntaxPattern(Pattern.compile("\"[^\"]*\""), colorString);
        codeView.addSyntaxPattern(Pattern.compile("'[^']*'"), colorString);
        codeView.addSyntaxPattern(Pattern.compile("//.*"), colorComment);
        codeView.addSyntaxPattern(Pattern.compile("#.*"), colorComment);
    }
}