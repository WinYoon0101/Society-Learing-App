package com.example.frontend.ui.friend;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Friend; // Nhớ import model Friend

public class FriendFragment extends Fragment {

    private FriendViewModel viewModel;

    // 1. Khai báo 2 biến Adapter
    private FriendAdapter requestAdapter;
    private FriendAdapter suggestionAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(FriendViewModel.class);

        // ===============================================
        // 2. SETUP RECYCLER VIEW LỜI MỜI (VÀ LẮNG NGHE NÚT BẤM)
        // ===============================================
        RecyclerView rvRequests = view.findViewById(R.id.rvFriendRequests);
        requestAdapter = new FriendAdapter(false, new FriendAdapter.OnFriendActionListener() {
            @Override
            public void onAcceptClick(Friend friend) {
                // Gọi API chấp nhận lời mời
                viewModel.acceptRequest(friend.getId());
            }

            @Override
            public void onDeclineClick(Friend friend) {
                // Tạm thời hiển thị Toast, bạn có thể gọi ViewModel để xóa nếu có API
                Toast.makeText(getContext(), "Đã xóa lời mời của " + friend.getUsername(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddFriendClick(Friend friend) {} // Bỏ qua

            @Override
            public void onRemoveSuggestClick(Friend friend) {} // Bỏ qua
        });
        rvRequests.setAdapter(requestAdapter);


        // ===============================================
        // 2. SETUP RECYCLER VIEW GỢI Ý (VÀ LẮNG NGHE NÚT BẤM)
        // ===============================================
        RecyclerView rvSuggestions = view.findViewById(R.id.rvFriendSuggestions);
        suggestionAdapter = new FriendAdapter(true, new FriendAdapter.OnFriendActionListener() {
            @Override
            public void onAcceptClick(Friend friend) {} // Bỏ qua

            @Override
            public void onDeclineClick(Friend friend) {} // Bỏ qua

            @Override
            public void onAddFriendClick(Friend friend) {
                // Gọi API gửi lời mời kết bạn (Bạn cần tạo hàm sendRequest trong ViewModel nếu chưa có)
                Toast.makeText(getContext(), "Đã gửi lời mời đến " + friend.getUsername(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveSuggestClick(Friend friend) {
                // Logic gỡ người này khỏi giao diện tạm thời
                Toast.makeText(getContext(), "Đã gỡ " + friend.getUsername() + " khỏi gợi ý", Toast.LENGTH_SHORT).show();
            }
        });
        rvSuggestions.setAdapter(suggestionAdapter);

        // Ánh xạ các View khác
        View layoutEmpty = view.findViewById(R.id.layoutEmptyRequests);
        TextView tvRequestCount = view.findViewById(R.id.tvRequestCount);

        // ===============================================
        // 3. Lắng nghe dữ liệu LỜI MỜI và bơm vào Adapter
        // ===============================================
        viewModel.getPendingRequestsResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING: break;
                case SUCCESS:
                    if (result.data != null && !result.data.isEmpty()) {
                        layoutEmpty.setVisibility(View.GONE);
                        rvRequests.setVisibility(View.VISIBLE);

                        tvRequestCount.setText(String.valueOf(result.data.size()));
                        tvRequestCount.setVisibility(View.VISIBLE);

                        requestAdapter.submitList(result.data);
                    } else {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvRequests.setVisibility(View.GONE);
                        tvRequestCount.setVisibility(View.GONE);
                    }
                    break;
                case ERROR:
                    Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        // ===============================================
        // 4. Lắng nghe dữ liệu GỢI Ý và bơm vào Adapter
        // ===============================================
        viewModel.getSuggestionsResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING: break;
                case SUCCESS:
                    if (result.data != null && !result.data.isEmpty()) {
                        suggestionAdapter.submitList(result.data);
                    } else {
                        view.findViewById(R.id.layoutSuggestions).setVisibility(View.GONE);
                    }
                    break;
                case ERROR:
                    Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        // Lắng nghe kết quả bấm nút Chấp nhận
        viewModel.getActionResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case SUCCESS:
                    Toast.makeText(getContext(), "Chấp nhận thành công!", Toast.LENGTH_SHORT).show();
                    // Gọi lại API để làm mới cả danh sách lời mời và gợi ý
                    viewModel.fetchPendingRequests();
                    viewModel.fetchFriendSuggestions();
                    break;
                case ERROR:
                    Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        // Kích hoạt gọi API ngay khi mở Fragment lên
        viewModel.fetchPendingRequests();
        viewModel.fetchFriendSuggestions();
    }
}