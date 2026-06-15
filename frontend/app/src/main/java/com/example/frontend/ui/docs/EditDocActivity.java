package com.example.frontend.ui.docs;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;

public class EditDocActivity extends AppCompatActivity {

    private EditText etTitle;

    private AutoCompleteTextView actSubject;
    private ProgressBar progressBar;
    private DocsViewModel viewModel;
    private String docId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_doc);

        // 1. Ánh xạ
        etTitle = findViewById(R.id.etTitle);
        actSubject = findViewById(R.id.actSubject);
        progressBar = findViewById(R.id.loading);
        TextView btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 2. Thiết lập dữ liệu cho Select Box (Dropdown Menu)
        String[] subjects = new String[]{"IT", "Kinh tế", "Khoa học", "Luật", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                subjects
        );
        actSubject.setAdapter(adapter);

        // 3. Nhận dữ liệu truyền từ màn hình danh sách sang
        Document doc = (Document) getIntent().getSerializableExtra("DOC_DATA");
        if (doc != null) {
            docId = doc.getId();
            etTitle.setText(doc.getTitle());

            // Set text môn học cũ. Tham số 'false' để tránh việc menu tự động xổ xuống khi vừa gán giá trị
            actSubject.setText(doc.getSubject(), false);
        }

        viewModel = new ViewModelProvider(this).get(DocsViewModel.class);

        // 4. Xử lý nút Lưu
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String subject = actSubject.getText().toString().trim();

            if (title.isEmpty() || subject.isEmpty()) {
                Toast.makeText(this, "Vui lòng không để trống thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.updateDocument(docId, title, subject);
        });

        // 5. Quan sát kết quả cập nhật
        viewModel.getUpdateResult().observe(this, result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
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