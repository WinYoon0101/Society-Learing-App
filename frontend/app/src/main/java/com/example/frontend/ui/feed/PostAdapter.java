package com.example.frontend.ui.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Post;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> postList;
    private Context context;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    // --- ĐÂY LÀ HÀM MỚI ĐƯỢC THÊM VÀO CHO MVVM ---
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Post> newPostList) {
        this.postList = newPostList;
        // Ra lệnh cho RecyclerView vẽ lại toàn bộ giao diện với dữ liệu mới
        notifyDataSetChanged();
    }
    // ---------------------------------------------

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp cái khuôn giao diện item_home_posts.xml của bạn
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_posts, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Đổ dữ liệu chữ
        holder.tvContent.setText(post.getContent());

        // Đổ dữ liệu User (Kiểm tra null để tránh văng app)
        if (post.getAuthorId() != null) {
            holder.tvUserName.setText(post.getAuthorId().getUsername());

            // Load Avatar bằng Glide
            Glide.with(context)
                    .load(post.getAuthorId().getAvatar())
                    .placeholder(R.drawable.ic_user) // Ảnh mặc định trong lúc chờ tải
                    .into(holder.imgAvatar);
        } else {
            holder.tvUserName.setText("Người dùng ẩn danh");
        }

        // Đổ dữ liệu ảnh (Nếu bài viết có ảnh thì hiện, không thì giấu đi)
        if (post.getImage() != null && !post.getImage().isEmpty()) {
            holder.imgPost.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImage())
                    .into(holder.imgPost);
        } else {
            holder.imgPost.setVisibility(View.GONE); // Giấu khung ảnh đi nếu không có
        }
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    // Class ánh xạ các thành phần trong item_home_posts.xml
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvContent;
        ImageView imgAvatar, imgPost;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvAuthorName);
            tvContent = itemView.findViewById(R.id.tvContent);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgPost = itemView.findViewById(R.id.imgPost);
        }
    }
}