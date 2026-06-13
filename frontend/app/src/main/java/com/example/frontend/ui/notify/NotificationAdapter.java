package com.example.frontend.ui.notify;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Notification;
import com.example.frontend.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications = new ArrayList<>();
    private Context context;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onClick(Notification notification);
    }

    public NotificationAdapter(Context context, OnNotificationClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateData(List<Notification> newList) {
        this.notifications = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification noti = notifications.get(position);

        // 1. Set Ảnh đại diện với Glide và hiệu ứng bo tròn
        if (noti.getSender() != null) {
            Glide.with(context)
                    .load(noti.getSender().getAvatar())
                    .placeholder(R.drawable.ic_user)
                    .circleCrop()
                    .into(holder.imgAvatar);
        }

        // 2. Chế biến nội dung (Tên người dùng + Nội dung từ Backend)
        String userName = (noti.getSender() != null && noti.getSender().getUsername() != null)
                ? noti.getSender().getUsername() : "Người dùng";

        String contentText = (noti.getContent() != null) ? noti.getContent() : "đã tương tác với bạn.";

        // In đậm Tên người dùng bằng HTML
        holder.tvContent.setText(Html.fromHtml("<b>" + userName + "</b> " + contentText, Html.FROM_HTML_MODE_LEGACY));

        // 3. Set Thời gian
        holder.tvTime.setText(TimeUtils.getTimeAgo(noti.getCreatedAt()));

        // 4. Hiệu ứng "Đã đọc" / "Chưa đọc"
        if (!noti.isRead()) {
            holder.layoutContainer.setBackgroundColor(Color.parseColor("#E8F4FA")); // Màu xanh nhạt
            holder.imgUnreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.layoutContainer.setBackgroundColor(Color.WHITE); // Màu trắng
            holder.imgUnreadDot.setVisibility(View.GONE);
        }

        // 5. Sự kiện Click
        holder.itemView.setOnClickListener(v -> {
            if (!noti.isRead()) {
                noti.setRead(true);
                notifyItemChanged(position);
            }
            if (listener != null) listener.onClick(noti);
        });
    }

    @Override
    public int getItemCount() { return notifications.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgUnreadDot;
        TextView tvContent, tvTime;
        LinearLayout layoutContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgUnreadDot = itemView.findViewById(R.id.imgUnreadDot);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutContainer = itemView.findViewById(R.id.layoutNotificationContainer);
        }
    }
}