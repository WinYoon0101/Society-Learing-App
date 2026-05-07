package com.example.frontend.ui.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent; // Thêm thư viện Intent
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
// Nhớ import đúng đường dẫn của PostDetailActivity trong project của bạn
// import com.example.frontend.ui.postdetail.PostDetailActivity;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> postList;
    private Context context;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Post> newPostList) {
        this.postList = newPostList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_posts, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // 1. Đổ dữ liệu chữ
        holder.tvContent.setText(post.getContent());

        // 2. Đổ dữ liệu User
        if (post.getAuthorId() != null) {
            holder.tvUserName.setText(post.getAuthorId().getUsername());
            Glide.with(context)
                    .load(post.getAuthorId().getAvatar())
                    .placeholder(R.drawable.ic_user)
                    .into(holder.imgAvatar);
        } else {
            holder.tvUserName.setText("Người dùng ẩn danh");
        }

        // 3. Đổ dữ liệu ảnh
        if (post.getImage() != null && !post.getImage().isEmpty()) {
            holder.imgPost.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImage())
                    .into(holder.imgPost);
        } else {
            holder.imgPost.setVisibility(View.GONE);
        }

        // =========================================================
        // 4. CHUYỂN HƯỚNG SANG MÀN HÌNH CHI TIẾT KHI BẤM NÚT COMMENT
        // =========================================================
        // (Kiểm tra xem holder.btnComment có null không để tránh crash nếu XML thiếu ID này)
        if (holder.btnComment != null) {
            holder.btnComment.setOnClickListener(v -> {
                // Khởi tạo Intent để chuyển trang
                Intent intent = new Intent(context, PostDetailActivity.class);

                // Gửi dữ liệu qua trang Detail (ID bài viết và Nội dung bài)
                intent.putExtra("POST_ID", post.getId());
                intent.putExtra("POST_CONTENT", post.getContent());

                if (post.getAuthorId() != null) {
                    intent.putExtra("AUTHOR_NAME", post.getAuthorId().getUsername());
                    intent.putExtra("AUTHOR_AVATAR", post.getAuthorId().getAvatar());
                }
                intent.putExtra("POST_IMAGE", post.getImage());

                // Bắt đầu chuyển trang
                context.startActivity(intent);
            });
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
        View btnComment; // Khai báo chung là View để bao trọn cả ImageView hoặc LinearLayout

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvAuthorName);
            tvContent = itemView.findViewById(R.id.tvContent);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgPost = itemView.findViewById(R.id.imgPost);

            // ÁNH XẠ NÚT COMMENT Ở ĐÂY
            // ⚠️ LƯU Ý: Bạn hãy mở file item_home_posts.xml lên và đảm bảo rằng
            // cái icon comment của bạn đang có thuộc tính android:id="@+id/btnComment" nhé!
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }
}