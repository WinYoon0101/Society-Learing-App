package com.example.frontend.ui.feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.frontend.R;
import java.util.ArrayList;

public class FeedFragment extends Fragment {
    private FeedViewModel viewModel;
    private PostAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        // Setup RecyclerView
        RecyclerView rcv = view.findViewById(R.id.rvPosts);
        rcv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter(getContext(), new ArrayList<>());
        rcv.setAdapter(adapter);

        // Kết nối ViewModel
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        viewModel.init(getContext());

        // Quan sát dữ liệu
        viewModel.getPosts().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                adapter.updateData(list);
            } else {
                Toast.makeText(getContext(), "Không có bài viết nào hoặc lỗi tải tin", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút mở màn hình tạo bài viết
        view.findViewById(R.id.btnOpenCreatePost).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CreatePostFragment())
                    .addToBackStack(null)
                    .commit();
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hàm này tự động chạy mỗi khi màn hình Feed hiển thị lại trước mặt người dùng
        if (viewModel != null) {
            // Ra lệnh cho ViewModel đi lấy danh sách mới ngay lập tức
            viewModel.loadPosts();
        }
    }
}