package com.example.admin.ui.posts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.admin.R;
import com.example.admin.data.model.Post;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> postList = new ArrayList<>();
    private final OnPostActionListener listener;

    public interface OnPostActionListener {
        void onDeleteClick(Post post, int position);
        void onBanUserClick(Post post, int position);
        void onApproveClick(Post post, int position);
    }

    public PostAdapter(OnPostActionListener listener) { this.listener = listener; }

    public void setPosts(List<Post> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvAuthorName.setText(post.getAuthor() != null ? post.getAuthor().getUsername() : "Ẩn danh");
        holder.tvContent.setText(post.getContent());

        // Cập nhật thời gian
        holder.tvTime.setText(formatTime(post.getCreatedAt()));

        if (post.getAuthor() != null && post.getAuthor().getAvatar() != null) {
            Glide.with(holder.itemView.getContext()).load(post.getAuthor().getAvatar()).circleCrop().into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        if (post.getMediaFiles() != null && !post.getMediaFiles().isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(post.getMediaFiles().get(0)).into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Đánh dấu trực quan bài vi phạm
        if (post.isScanned() && post.isToxicLocally()) {
            holder.cvAiTag.setVisibility(View.VISIBLE);
            holder.tvAiTag.setText(post.getToxicLabel());
            holder.viewToxicIndicator.setVisibility(View.VISIBLE);
            holder.btnApprove.setVisibility(View.VISIBLE);
        } else {
            holder.cvAiTag.setVisibility(View.GONE);
            holder.viewToxicIndicator.setVisibility(View.GONE);
            holder.btnApprove.setVisibility(View.GONE);
        }

        // ---- LOGIC ĐỔI TRẠNG THÁI NÚT KHÓA / MỞ KHÓA ----
        if (post.getAuthor() != null) {
            if (post.getAuthor().isActive()) {
                // Trạng thái bình thường -> Bấm vào để Khóa
                holder.btnBan.setText("Khóa User");
                holder.btnBan.setTextColor(android.graphics.Color.parseColor("#F59E0B")); // Chữ màu cam

                // Đổi icon thành ổ khóa đóng
                holder.btnBan.setIconResource(android.R.drawable.ic_secure);
                holder.btnBan.setIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F59E0B"))); // Icon màu cam
            } else {
                // Trạng thái đang bị khóa -> Bấm vào để Mở khóa
                holder.btnBan.setText("Mở khóa");
                holder.btnBan.setTextColor(android.graphics.Color.parseColor("#10B981")); // Chữ màu xanh lá

                // Đổi icon thành ổ khóa mở
                holder.btnBan.setIconResource(android.R.drawable.ic_partial_secure);
                holder.btnBan.setIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981"))); // Icon màu xanh lá
            }
        }

        // Xử lý Click Events
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(post, position));
        holder.btnBan.setOnClickListener(v -> listener.onBanUserClick(post, position));
        holder.btnApprove.setOnClickListener(v -> listener.onApproveClick(post, position));
    }

    @Override
    public int getItemCount() { return postList.size(); }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthorName, tvTime, tvContent, tvAiTag;
        ImageView ivAvatar, ivPostImage;
        com.google.android.material.button.MaterialButton btnDelete, btnBan, btnApprove;
        MaterialCardView cvAiTag;
        View viewToxicIndicator;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvAiTag = itemView.findViewById(R.id.tvAiTag);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnBan = itemView.findViewById(R.id.btnBan);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            cvAiTag = itemView.findViewById(R.id.cvAiTag);
            viewToxicIndicator = itemView.findViewById(R.id.viewToxicIndicator);
        }
    }

    private String formatTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) return "Không rõ";
        try {
            // Giả sử API trả về định dạng chuẩn ISO 8601 (VD: 2026-06-28T10:30:00.000Z)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ép về giờ UTC để tính toán chuẩn
            Date date = sdf.parse(timeString);

            long diff = (System.currentTimeMillis() - date.getTime()) / 1000; // Tính ra số giây chênh lệch

            if (diff < 60) return "Vừa xong";
            if (diff < 3600) return (diff / 60) + " phút trước";
            if (diff < 86400) return (diff / 3600) + " giờ trước";
            if (diff < 2592000) return (diff / 86400) + " ngày trước";

            // Nếu lâu hơn 1 tháng thì hiển thị ngày tháng năm
            SimpleDateFormat outFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return outFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return timeString; // Nếu lỗi parse thì in nguyên bản String từ server
        }
    }
}