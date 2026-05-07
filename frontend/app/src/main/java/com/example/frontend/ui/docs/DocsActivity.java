package com.example.frontend.ui.docs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;

public class DocsActivity extends AppCompatActivity {

    private RecyclerView rvMyDocs;
    private DocsAdapter adapter;
    private DocsViewModel viewModel;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docs);

        // 1. Ánh xạ View
        rvMyDocs = findViewById(R.id.rvMyDocs);
        progressBar = findViewById(R.id.loading);
        tvEmpty = findViewById(R.id.tvEmpty);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // 2. Setup RecyclerView
        rvMyDocs.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DocsAdapter(new DocsAdapter.OnDocActionListener() {
            @Override
            public void onDeleteClick(Document doc) {
                // HIỂN THỊ DIALOG XÁC NHẬN TRƯỚC KHI XÓA
                new AlertDialog.Builder(DocsActivity.this)
                        .setTitle("Xóa tài liệu")
                        .setMessage("Bạn có chắc chắn muốn xóa '" + doc.getTitle() + "'? Hành động này không thể hoàn tác.")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            viewModel.deleteDocument(doc.getId());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onEditClick(Document doc) {
                Intent intent = new Intent(DocsActivity.this, EditDocActivity.class);
                // Truyền toàn bộ object sang để hiển thị thông tin cũ
                intent.putExtra("DOC_DATA", doc);
                startActivity(intent);
            }

            @Override
            public void onItemClick(Document doc) {
                Toast.makeText(DocsActivity.this, "Xem tài liệu: " + doc.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        rvMyDocs.setAdapter(adapter);

        // 3. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(DocsViewModel.class);

        // 4. QUAN SÁT KẾT QUẢ DANH SÁCH (Fetch)
        viewModel.getMyDocsResult().observe(this, result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    if (result.data != null) {
                        adapter.submitList(result.data.getDocuments());
                        tvEmpty.setVisibility(result.data.getDocuments().isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        // 5. QUAN SÁT KẾT QUẢ XÓA (Delete)
        viewModel.getDeleteResult().observe(this, result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã xóa tài liệu!", Toast.LENGTH_SHORT).show();
                    // Load lại danh sách trang 1 để cập nhật UI
                    viewModel.fetchMyDocuments(1, 20);
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Xóa thất bại: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        // Gọi API lần đầu
        viewModel.fetchMyDocuments(1, 20);
    }
}