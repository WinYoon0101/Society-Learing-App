package com.example.frontend.ui.docs;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;

public class DocsActivity extends AppCompatActivity {

    private RecyclerView rvMyDocs;
    private DocsAdapter adapter; // ĐÃ ĐỔI SANG DocsAdapter
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

        // 2. Setup RecyclerView với Listener cho DocsAdapter
        rvMyDocs.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DocsAdapter(new DocsAdapter.OnDocActionListener() {
            @Override
            public void onDeleteClick(Document doc) {
                // Xử lý xóa tài liệu: Hiển thị Dialog xác nhận rồi gọi API xóa
                Toast.makeText(DocsActivity.this, "Xóa: " + doc.getTitle(), Toast.LENGTH_SHORT).show();
                // viewModel.deleteDocument(doc.getId());
            }

            @Override
            public void onEditClick(Document doc) {
                // Chuyển sang màn hình Edit tài liệu
                Toast.makeText(DocsActivity.this, "Sửa: " + doc.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemClick(Document doc) {
                // Mở file tài liệu
                Toast.makeText(DocsActivity.this, "Mở: " + doc.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        rvMyDocs.setAdapter(adapter);

        // 3. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(DocsViewModel.class);

        // 4. Quan sát dữ liệu
        viewModel.getMyDocsResult().observe(this, result -> {
            if (result == null) return;

            switch (result.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    break;

                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    if (result.data != null && result.data.getDocuments() != null) {
                        // FIX: Đưa vào hàng đợi để đảm bảo RecyclerView đã sẵn sàng
                        rvMyDocs.post(() -> {
                            adapter.submitList(result.data.getDocuments());
                            if (result.data.getDocuments().isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                            }
                        });
                    }
                    break;

                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        // 5. Gọi API
        viewModel.fetchMyDocuments(1, 20);
    }
}