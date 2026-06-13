package com.example.frontend.ui.notify;

import android.os.Bundle;
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

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Notification;
import com.example.frontend.data.model.NotificationListResponse;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotifyFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private MaterialButton btnMarkAllRead;

    private NotificationAdapter adapter;
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
        adapter    = new NotificationAdapter();
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
        apiService.markAllNotificationsRead().enqueue(new Callback<com.example.frontend.data.model.ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<com.example.frontend.data.model.ApiResponse<Object>> call,
                                   Response<com.example.frontend.data.model.ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    adapter.markAllRead();
                    Toast.makeText(requireContext(), "Đã đánh dấu đọc tất cả", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<com.example.frontend.data.model.ApiResponse<Object>> call, Throwable t) {}
        });
    }

    // ─── Adapter ──────────────────────────────────────────────────────────────
    class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
        private final List<Notification> items = new ArrayList<>();

        void submit(List<Notification> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        void markAllRead() {
            for (int i = 0; i < items.size(); i++) notifyItemChanged(i);
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Notification n = items.get(pos);
            h.tvMessage.setText(n.getMessage());
            h.tvTime.setText(formatTime(n.getCreatedAt()));
            h.dotUnread.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
            h.itemView.setBackgroundColor(n.isRead() ? 0xFFFFFFFF : 0xFFECFDF5);

            if (n.getSender() != null && n.getSender().getAvatar() != null
                    && !n.getSender().getAvatar().isEmpty()) {
                Glide.with(h.imgAvatar).load(n.getSender().getAvatar())
                        .placeholder(R.drawable.ic_user).into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_user);
            }

            h.itemView.setOnClickListener(v -> {
                if (!n.isRead()) {
                    apiService.markNotificationRead(n.getId()).enqueue(new Callback<com.example.frontend.data.model.ApiResponse<Object>>() {
                        @Override public void onResponse(Call<com.example.frontend.data.model.ApiResponse<Object>> call, Response<com.example.frontend.data.model.ApiResponse<Object>> r) {}
                        @Override public void onFailure(Call<com.example.frontend.data.model.ApiResponse<Object>> call, Throwable t) {}
                    });
                    h.dotUnread.setVisibility(View.GONE);
                    h.itemView.setBackgroundColor(0xFFFFFFFF);
                }
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView imgAvatar;
            TextView tvMessage, tvTime;
            View dotUnread;
            VH(@NonNull View v) {
                super(v);
                imgAvatar  = v.findViewById(R.id.imgSenderAvatar);
                tvMessage  = v.findViewById(R.id.tvMessage);
                tvTime     = v.findViewById(R.id.tvTime);
                dotUnread  = v.findViewById(R.id.dotUnread);
            }
        }
    }

    private String formatTime(String iso) {
        if (iso == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(iso);
            long diff = (System.currentTimeMillis() - date.getTime()) / 1000;
            if (diff < 60)   return diff + "s trước";
            if (diff < 3600) return (diff / 60) + " phút trước";
            if (diff < 86400)return (diff / 3600) + " giờ trước";
            return (diff / 86400) + " ngày trước";
        } catch (ParseException e) {
            return "";
        }
    }
}
