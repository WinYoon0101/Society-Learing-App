package com.example.frontend.ui.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Post;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    // =========================================================
    // 1. INTERFACE: Lắng nghe sự kiện thả cảm xúc
    // =========================================================
    public interface OnReactionListener {
        void onReactClick(String targetId, String type);
    }

    private List<Post> postList;
    private Context context;
    private OnReactionListener reactionListener;

    // 2. CẬP NHẬT CONSTRUCTOR: Truyền thêm listener vào
    public PostAdapter(Context context, List<Post> postList, OnReactionListener listener) {
        this.context = context;
        this.postList = postList;
        this.reactionListener = listener;
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

        // 3.5. HIỂN THỊ SỐ LƯỢNG COMMENT
        if (holder.tvCommentCount != null) {
            holder.tvCommentCount.setText(String.valueOf(post.getcountComment()));
            holder.tvCommentCount.setVisibility(View.VISIBLE);
        }

        // 4. CHUYỂN HƯỚNG SANG MÀN HÌNH CHI TIẾT
        if (holder.btnComment != null) {
            holder.btnComment.setOnClickListener(v -> {
                Intent intent = new Intent(context, PostDetailActivity.class);

                intent.putExtra("POST_ID", post.getId());
                intent.putExtra("POST_CONTENT", post.getContent());

                if (post.getAuthorId() != null) {
                    intent.putExtra("AUTHOR_NAME", post.getAuthorId().getUsername());
                    intent.putExtra("AUTHOR_AVATAR", post.getAuthorId().getAvatar());
                }
                intent.putExtra("POST_IMAGE", post.getImage());

                context.startActivity(intent);
            });
        }

        // =========================================================
        // 5. XỬ LÝ REACTION (THẢ CẢM XÚC)
        // =========================================================
        if (holder.btnLike != null) {
            // Khởi tạo Icon hiện tại (Load từ dữ liệu cũ)
            // LƯU Ý: Đảm bảo model Post có hàm getMyReaction()
            String currentReaction = post.getMyReaction();
            holder.btnLike.setImageResource(getIconForReaction(currentReaction));

            // 5.1. SỰ KIỆN NHẤN THƯỜNG (Chỉ Like hoặc Hủy Like)
            holder.btnLike.setOnClickListener(v -> {
                String newReaction = (post.getMyReaction() != null) ? null : "Like";
                handleReactionUpdate(holder, post, newReaction);
            });

            // 5.2. SỰ KIỆN NHẤN GIỮ HIỆN POPUP CẢM XÚC
            holder.btnLike.setOnLongClickListener(v -> {
                View popupView = LayoutInflater.from(context).inflate(R.layout.item_feed_reaction_popup, null);
                PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                // Ánh xạ 6 nút
                ImageView btnReactLike = popupView.findViewById(R.id.btnReactLike);
                ImageView btnReactLove = popupView.findViewById(R.id.btnReactLove);
                ImageView btnReactHaha = popupView.findViewById(R.id.btnReactHaha);
                ImageView btnReactWow = popupView.findViewById(R.id.btnReactWow);
                ImageView btnReactSad = popupView.findViewById(R.id.btnReactSad);
                ImageView btnReactAngry = popupView.findViewById(R.id.btnReactAngry);

                // Bắt sự kiện chọn icon
                btnReactLike.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Like"); popupWindow.dismiss(); });
                btnReactLove.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Love"); popupWindow.dismiss(); });
                btnReactHaha.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Haha"); popupWindow.dismiss(); });
                btnReactWow.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Wow"); popupWindow.dismiss(); });
                btnReactSad.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Sad"); popupWindow.dismiss(); });
                btnReactAngry.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Angry"); popupWindow.dismiss(); });

                // Hiển thị popup lơ lửng trên đầu nút Like
                popupWindow.showAsDropDown(v, 0, -v.getHeight() - 140);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    // =========================================================
    // HÀM HỖ TRỢ XỬ LÝ REACTION
    // =========================================================
    private void handleReactionUpdate(PostViewHolder holder, Post post, String type) {
        // 1. Cập nhật local data (Đảm bảo model Post có hàm setMyReaction)
        post.setMyReaction(type);

        // 2. Đổi icon tức thì cho user nhìn thấy (Optimistic UI)
        holder.btnLike.setImageResource(getIconForReaction(type));

        // 3. Truyền tin cho Activity/Fragment đẩy API lên Backend
        if (reactionListener != null) {
            reactionListener.onReactClick(post.getId(), type);
        }
    }

    private int getIconForReaction(String type) {
        if (type == null) return R.drawable.ic_like; // Icon khi chưa thả cảm xúc (Bạn nhớ chuẩn bị icon này)
        switch (type) {
            case "Like": return R.drawable.ic_like_color;
            case "Love": return R.drawable.ic_love;
            case "Haha": return R.drawable.ic_haha;
            case "Wow":  return R.drawable.ic_wow;
            case "Sad":  return R.drawable.ic_sad;
            case "Angry":return R.drawable.ic_angry;
            default: return R.drawable.ic_like;
        }
    }

    // Class ánh xạ các thành phần trong item_home_posts.xml
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvContent, tvCommentCount;
        ImageView imgAvatar, imgPost, btnLike;
        View btnComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvAuthorName);
            tvContent = itemView.findViewById(R.id.tvContent);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgPost = itemView.findViewById(R.id.imgPost);
            btnComment = itemView.findViewById(R.id.btnComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            btnLike = itemView.findViewById(R.id.imgLike);
        }
    }
}