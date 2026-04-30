package com.example.frontend.ui.feed;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import java.util.ArrayList;

public class FeedFragment extends Fragment {
    private FeedViewModel viewModel;
    private PostAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        // =======================================================
        // BỔ SUNG: CẬP NHẬT GIAO DIỆN THANH ĐĂNG BÀI (AVATAR VÀ TÊN USER)
        // =======================================================
        ImageView imgMyAvatarInFeed = view.findViewById(R.id.imgMyAvatarInFeed);
        TextView tvCreatePostHint = view.findViewById(R.id.tvCreatePostHint);

        // Lấy thông tin từ SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String myAvatarUrl = prefs.getString("USER_AVATAR", "");
        String myUsername = prefs.getString("USERNAME", "Bạn");

        // 1. Cài đặt chữ (Cắt lấy tên cuối cho giống Facebook)
        if (tvCreatePostHint != null) {
            String shortName = myUsername;
            if (myUsername.contains(" ")) {
                shortName = myUsername.substring(myUsername.lastIndexOf(" ") + 1); // Lấy chữ cuối cùng
            }
            tvCreatePostHint.setText(shortName + " ơi, bạn muốn chia sẻ kiến thức gì?");
        }

        // 2. Cài đặt Avatar
        if (!myAvatarUrl.isEmpty() && imgMyAvatarInFeed != null) {
            Glide.with(this)
                    .load(myAvatarUrl)
                    .placeholder(R.drawable.ic_user)
                    .into(imgMyAvatarInFeed);
        }
        // =======================================================

        // 1. Kết nối ViewModel
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        viewModel.init(getContext());

        // 2. Setup RecyclerView
        RecyclerView rcv = view.findViewById(R.id.rvPosts);
        rcv.setLayoutManager(new LinearLayoutManager(getContext()));

        // =======================================================
        // Khởi tạo Adapter kèm Interface Lắng nghe Reaction
        // =======================================================
        adapter = new PostAdapter(getContext(), new ArrayList<>(), (targetId, type) -> {
            // Fix lỗi truyền Null cho Backend khi người dùng ấn Hủy Like
            String reactionToSend = (type == null) ? "Like" : type;

            if (viewModel != null) {
                // CHÚ Ý: Truyền đúng chữ "Post" (không có s) để Backend Node.js nhận diện đúng
                Log.d("DEBUG_REACT", "👉 Đang gửi API thả tim lên Server: " + reactionToSend);
                viewModel.toggleReaction(targetId, "Post", reactionToSend);
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
        if (viewModel != null) {
            viewModel.loadPosts(); // Load lại data mới nhất khi quay lại màn hình
        }
    }
}