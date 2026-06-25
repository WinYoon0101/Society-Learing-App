package com.example.frontend.ui.notify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Notification;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.ui.feed.PostDetailActivity;
import com.example.frontend.ui.group.GroupDetailActivity;
import com.example.frontend.ui.profile.FriendProfileActivity;

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

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    private final List<Notification> items = new ArrayList<>();
    private final Context context;
    private final ApiService apiService;

    // Constructor nhận Context để dùng cho Glide và Intent
    public NotificationAdapter(Context context) {
        this.context = context;
        this.apiService = ApiClient.getApiService(context);
    }

    public void submit(List<Notification> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void markAllRead() {
        for (Notification item : items) {
            item.setRead(true);
        }
        notifyDataSetChanged(); // Cập nhật lại toàn bộ list trong 1 lần
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Notification n = items.get(pos);
        h.tvMessage.setText(n.getContent() != null ? n.getContent() : "Thông báo mới");
        h.tvTime.setText(formatTime(n.getCreatedAt()));

        // Cập nhật màu nền và dấu chấm chưa đọc
        h.dotUnread.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
        h.itemView.setBackgroundColor(n.isRead() ? Color.WHITE : Color.parseColor("#ECFDF5"));

        // Load Avatar
        if (n.getSender() != null && n.getSender().getAvatar() != null && !n.getSender().getAvatar().isEmpty()) {
            Glide.with(context).load(n.getSender().getAvatar())
                    .placeholder(R.drawable.ic_user).into(h.imgAvatar);
        } else {
            h.imgAvatar.setImageResource(R.drawable.ic_user);
        }

        // Xử lý Click điều hướng
        h.itemView.setOnClickListener(v -> {
            Log.d("NOTI_DEBUG", "Type: " + n.getTargetType() + " | Id: " + n.getTargetId());

            // 1. Cập nhật trạng thái đã đọc lên Server và UI
            if (!n.isRead()) {
                n.setRead(true);
                notifyItemChanged(pos);
                apiService.markNotificationRead(n.getId()).enqueue(new Callback<ApiResponse<Object>>() {
                    @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> r) {}
                    @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {}
                });
            }

            // 2. Chuyển trang
            String targetType = n.getTargetType();
            String targetId = n.getTargetId();

            if (targetType == null || targetId == null) {
                Toast.makeText(context, "Không thể mở nội dung này", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                if (targetType.equalsIgnoreCase("Post") || targetType.equalsIgnoreCase("Comment")) {
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("POST_ID", targetId);
                    context.startActivity(intent);

                } else if (targetType.equalsIgnoreCase("Friend") || targetType.equalsIgnoreCase("User")) {
                    if (n.getSender() != null) {
                        Intent intent = new Intent(context, FriendProfileActivity.class);
                        intent.putExtra("FRIEND_ID", n.getSender().getId());
                        intent.putExtra("FRIEND_NAME", n.getSender().getUsername());
                        intent.putExtra("FRIEND_AVATAR", n.getSender().getAvatar());
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                    }

                } else if (targetType.equalsIgnoreCase("Group")) {
                    Intent intent = new Intent(context, GroupDetailActivity.class);
                    intent.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, targetId);
                    context.startActivity(intent);

                } else {
                    Toast.makeText(context, "Loại thông báo không được hỗ trợ", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Đã xảy ra lỗi khi mở màn hình", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

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

    static class VH extends RecyclerView.ViewHolder {
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