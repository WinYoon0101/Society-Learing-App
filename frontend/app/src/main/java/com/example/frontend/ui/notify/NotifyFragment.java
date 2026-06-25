package com.example.frontend.ui.notify;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Notification;
import com.example.frontend.data.model.NotificationListResponse;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotifyFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private MaterialButton btnMarkAllRead;

    private NotificationAdapter adapter; // Gọi Adapter từ file ngoài
    private ApiService apiService;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh     = view.findViewById(R.id.swipeRefresh);
        rvNotifications  = view.findViewById(R.id.rvNotifications);
        tvEmpty          = view.findViewById(R.id.tvEmpty);
        btnMarkAllRead   = view.findViewById(R.id.btnMarkAllRead);

        apiService = ApiClient.getApiService(requireContext());

        // Khởi tạo Adapter ngoài (Chỉ cần truyền Context)
        adapter = new NotificationAdapter(requireContext());

        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadNotifications);
        btnMarkAllRead.setOnClickListener(v -> markAllRead());

        loadNotifications();
    }

    private void loadNotifications() {
        swipeRefresh.setRefreshing(true);
        apiService.getNotifications(1, 50).enqueue(new Callback<NotificationListResponse>() {
            @Override
            public void onResponse(Call<NotificationListResponse> call,
                                   Response<NotificationListResponse> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Notification> list = response.body().getData();
                    adapter.submit(list);
                    boolean empty = list == null || list.isEmpty();
                    tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
                } else {
                    Toast.makeText(requireContext(), "Không tải được thông báo", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<NotificationListResponse> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllRead() {
        btnMarkAllRead.setEnabled(false); // Khóa nút chống bấm liên tục

        apiService.markAllNotificationsRead().enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                btnMarkAllRead.setEnabled(true);
                if (response.isSuccessful()) {
                    adapter.markAllRead();
                    Toast.makeText(requireContext(), "Đã đánh dấu đọc tất cả", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Lỗi từ Server", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                btnMarkAllRead.setEnabled(true);
                Toast.makeText(requireContext(), "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
                Log.e("NOTI_DEBUG", "Lỗi markAllRead: " + t.getMessage());
            }
        });
    }
}