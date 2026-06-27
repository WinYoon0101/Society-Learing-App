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
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

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

        // Load Avatar
        if (post.getAuthor() != null && post.getAuthor().getAvatar() != null) {
            Glide.with(holder.itemView.getContext()).load(post.getAuthor().getAvatar()).circleCrop().into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round); // Avatar mặc định
        }

        // Load Ảnh đính kèm
        if (post.getMediaFiles() != null && !post.getMediaFiles().isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(post.getMediaFiles().get(0)).into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Cảnh báo AI
        if (post.isScanned() && post.isToxicLocally()) {
            holder.tvAiTag.setVisibility(View.VISIBLE);
            holder.tvAiTag.setText(post.getToxicLabel());
        } else {
            holder.tvAiTag.setVisibility(View.GONE);
        }

        // Action Buttons
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(post, position));
        holder.btnBan.setOnClickListener(v -> listener.onBanUserClick(post, position));

    }

    @Override
    public int getItemCount() { return postList.size(); }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthorName, tvContent, tvAiTag;
        ImageView ivAvatar, ivPostImage;
        Button btnDelete, btnBan;


        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvAiTag = itemView.findViewById(R.id.tvAiTag);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnBan = itemView.findViewById(R.id.btnBan);

        }
    }
}