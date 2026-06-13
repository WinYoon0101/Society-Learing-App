package com.example.frontend.ui.notify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Notification;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.ui.feed.PostDetailActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotifyFragment extends Fragment {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Ánh xạ layout
        View view = inflater.inflate(R.layout.fragment_notify, container, false);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. Khởi tạo Adapter và gắn sự kiện khi Click vào 1 thông báo
        adapter = new NotificationAdapter(getContext(), notification -> {

            // Gọi API báo cho Backend là "Tui đã đọc cái này rồi!"
            markAsRead(notification.getId());

            // Chuyển sang màn hình xem bài viết
            // Kiểm tra xem đích đến là Bài viết hay Comment để gắn ID cho chuẩn
            if (notification.getTargetId() != null) {
                Intent intent = new Intent(getContext(), PostDetailActivity.class);
                intent.putExtra("POST_ID", notification.getTargetId());
                startActivity(intent);
            }
        });

        rvNotifications.setAdapter(adapter);

        // 3. Tải dữ liệu lần đầu tiên
        loadNotifications();

        return view;
    }

    // Mỗi khi vuốt qua vuốt lại tab Thông Báo, tự động làm mới danh sách
    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }

    // Hàm gọi API lấy danh sách thông báo về
    private void loadNotifications() {
        // ĐÃ SỬA: Truyền getContext() vào getApiService()
        ApiClient.getApiService(getContext()).getNotifications().enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Notification>>> call, Response<ApiResponse<List<Notification>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Notification> notiList = response.body().getData();
                    if (notiList != null) {
                        adapter.updateData(notiList); // Đổ dữ liệu vào giao diện
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi tải thông báo!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Notification>>> call, Throwable t) {
                Log.e("NotifyFragment", "Lỗi mạng: " + t.getMessage());
            }
        });
    }

    // Hàm gọi API đánh dấu đã đọc
    private void markAsRead(String notiId) {
        // ĐÃ SỬA: Truyền getContext() vào getApiService()
        ApiClient.getApiService(getContext()).markNotificationAsRead(notiId).enqueue(new Callback<ApiResponse<Notification>>() {
            @Override
            public void onResponse(Call<ApiResponse<Notification>> call, Response<ApiResponse<Notification>> response) {
                // Thành công: không cần làm gì vì bên Adapter lúc click nó đã đổi màu chữ/mất chấm đỏ rồi
            }

            @Override
            public void onFailure(Call<ApiResponse<Notification>> call, Throwable t) {
                Log.e("NotifyFragment", "Lỗi đánh dấu đã đọc: " + t.getMessage());
            }
        });
    }
}