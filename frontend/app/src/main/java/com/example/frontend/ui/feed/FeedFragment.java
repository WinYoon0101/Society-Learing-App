package com.example.frontend.ui.feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        // 1. Kết nối ViewModel (Nên gọi trước để adapter có thể dùng được ngay)
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        viewModel.init(getContext());

        // 2. Setup RecyclerView
        RecyclerView rcv = view.findViewById(R.id.rvPosts);
        rcv.setLayoutManager(new LinearLayoutManager(getContext()));

        // =======================================================
        // ĐÃ SỬA: Khởi tạo Adapter kèm Interface Lắng nghe Reaction
        // =======================================================
        adapter = new PostAdapter(getContext(), new ArrayList<>(), (targetId, type) -> {
            // Khi Adapter báo có người vừa thả cảm xúc, ta nhờ ViewModel đẩy lên Backend
            // (Theo logic Backend của bạn: Nếu truyền đúng type cũ lên, nó sẽ tự Unlike.
            // Do đó nếu type ở local bị null (do user hủy Like), ta vẫn gửi chữ "Like" lên để Backend xóa)
            String reactionToSend = (type == null) ? "Like" : type;

            if (viewModel != null) {
                viewModel.toggleReaction(targetId, "Posts", reactionToSend);
            }
        });

        rcv.setAdapter(adapter);

        // 3. Quan sát dữ liệu
        viewModel.getPosts().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                adapter.updateData(list);
            } else {
                Toast.makeText(getContext(), "Không có bài viết nào hoặc lỗi tải tin", Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Nút mở màn hình tạo bài viết
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