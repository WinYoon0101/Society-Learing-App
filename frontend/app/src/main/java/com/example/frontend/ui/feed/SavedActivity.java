package com.example.frontend.ui.feed;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;

// Nếu báo đỏ SavedPostAdapter và SavedViewModel, bạn click chuột vào chữ đó rồi ấn Alt + Enter -> Import class nhé!
import com.example.frontend.ui.feed.SavedPostAdapter;
import com.example.frontend.ui.feed.SavedViewModel;

import java.util.ArrayList;

public class SavedActivity extends AppCompatActivity {

    private RecyclerView rvSavedPosts;
    private SavedPostAdapter adapter;
    private TextView tvEmptySaved;

    private SavedViewModel viewModel;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_saved);

        // Căn chỉnh viền màn hình (Code cũ của bạn)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Ánh xạ View
        rvSavedPosts = findViewById(R.id.rvSavedPosts);
        tvEmptySaved = findViewById(R.id.tvEmptySaved);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Lấy Token người dùng
        token = "Bearer " + getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).getString("JWT_TOKEN", "");

        // 2. Khởi tạo ViewModel (Bộ não gọi API)
        viewModel = new ViewModelProvider(this).get(SavedViewModel.class);

        // 3. Khởi tạo RecyclerView & Adapter (Giao diện danh sách)
        rvSavedPosts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavedPostAdapter(this, new ArrayList<>(), postId -> {
            Toast.makeText(this, "Đang xử lý...", Toast.LENGTH_SHORT).show();
            viewModel.unsavePost(token, postId); // Khi bấm Bỏ lưu -> Gọi API
        });
        rvSavedPosts.setAdapter(adapter);

        // 4. Lắng nghe dữ liệu đổ về từ Backend
        viewModel.getSavedPosts().observe(this, list -> {
            if (list != null && !list.isEmpty()) {
                adapter.updateData(list);
                showEmptyState(false); // Hiện danh sách
            } else {
                showEmptyState(true);  // Hiện chữ "Bạn chưa lưu bài viết nào"
            }
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mỗi lần mở lại màn hình này -> Tự động gọi API lấy bài lưu mới nhất
        if (viewModel != null) {
            viewModel.fetchSavedPosts(token);
        }
    }

    // Hàm ẩn/hiện danh sách hoặc dòng chữ trống
    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvSavedPosts.setVisibility(View.GONE);
            tvEmptySaved.setVisibility(View.VISIBLE);
        } else {
            rvSavedPosts.setVisibility(View.VISIBLE);
            tvEmptySaved.setVisibility(View.GONE);
        }
    }
}