package com.example.frontend.ui.docs;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;

public class EditDocActivity extends AppCompatActivity {

    private EditText etTitle, etSubject;
    private ProgressBar progressBar;
    private DocsViewModel viewModel;
    private String docId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_doc);

        // 1. Ánh xạ
        etTitle = findViewById(R.id.etTitle);
        etSubject = findViewById(R.id.etSubject);
        progressBar = findViewById(R.id.loading);
        TextView btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 2. Nhận dữ liệu truyền từ màn hình danh sách sang
        Document doc = (Document) getIntent().getSerializableExtra("DOC_DATA");
        if (doc != null) {
            docId = doc.getId();
            etTitle.setText(doc.getTitle());
            etSubject.setText(doc.getSubject());
        }

        viewModel = new ViewModelProvider(this).get(DocsViewModel.class);

        // 3. Xử lý nút Lưu
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String subject = etSubject.getText().toString().trim();

            if (title.isEmpty() || subject.isEmpty()) {
                Toast.makeText(this, "Vui lòng không để trống thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.updateDocument(docId, title, subject);
        });

        // 4. Quan sát kết quả cập nhật
        viewModel.getUpdateResult().observe(this, result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Báo về cho DocsActivity biết để refresh
                    finish();
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}