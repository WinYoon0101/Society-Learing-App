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
                viewModel.acceptRequest(friend.getId());
            }

            @Override
            public void onDeclineClick(Friend friend) {
                viewModel.declineRequest(friend.getId());
                Toast.makeText(getContext(), "Đang xóa lời mời...", Toast.LENGTH_SHORT).show();
            }

            @Override public void onAddFriendClick(Friend friend) {}
            @Override public void onRemoveSuggestClick(Friend friend) {}
        });

        rvRequests.setAdapter(requestAdapter);


        // ===============================================
        // 2. SETUP RECYCLER VIEW GỢI Ý (VÀ LẮNG NGHE NÚT BẤM)
        // ===============================================
        RecyclerView rvSuggestions = view.findViewById(R.id.rvFriendSuggestions);
        suggestionAdapter = new FriendAdapter(true, new FriendAdapter.OnFriendActionListener() {
            @Override
            public void onAddFriendClick(Friend friend) {
                // 1. Cập nhật UI ngay lập tức
                suggestionAdapter.updateItemStatus(friend.getId(), true);

                // 2. Gọi API ngầm
                viewModel.sendFriendRequest(friend.getId());
                Toast.makeText(getContext(), "Đã gửi yêu cầu kết bạn", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeclineClick(Friend friend) {
                // Nếu là list gợi ý và đang hiện "Hủy lời mời"
                // 1. Cập nhật UI ngay lập tức quay về "Thêm bạn bè"
                suggestionAdapter.updateItemStatus(friend.getId(), false);

                // 2. Gọi API hủy (dùng chung hàm decline/xóa lời mời)
                viewModel.declineRequest(friend.getId());
                Toast.makeText(getContext(), "Đã hủy yêu cầu", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveSuggestClick(Friend friend) {
                // Gỡ khỏi danh sách gợi ý
                Toast.makeText(getContext(), "Đã gỡ gợi ý", Toast.LENGTH_SHORT).show();
                viewModel.fetchFriendSuggestions();
            }

            @Override public void onAcceptClick(Friend friend) {}
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
        // Lắng nghe kết quả chung cho các hành động (Chấp nhận, Thêm bạn, Xóa)
        viewModel.getActionResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case SUCCESS:
                    Toast.makeText(getContext(), "Thành công!", Toast.LENGTH_SHORT).show();

                    // Cập nhật lại cả 2 danh sách để người đó biến mất khỏi chỗ cũ
                    // hoặc hiện đúng số lượng mới
                    viewModel.fetchPendingRequests();
                    viewModel.fetchFriendSuggestions();
                    break;

                case ERROR:
                    Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
                    break;

                case LOADING:
                    break;
            }
        });

        // Kích hoạt gọi API ngay khi mở Fragment lên
        viewModel.fetchPendingRequests();
        viewModel.fetchFriendSuggestions();
    }
}