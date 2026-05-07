package com.example.frontend.ui.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Post;
import com.example.frontend.ui.feed.PostDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class SavedPostAdapter extends RecyclerView.Adapter<SavedPostAdapter.SavedViewHolder> {

    // Interface để báo ra ngoài khi bấm Bỏ lưu
    public interface OnUnsaveClickListener {
        void onUnsavePost(String postId);
    }

    private Context context;
    private List<Post> savedPostList;
    private OnUnsaveClickListener unsaveListener;

    public SavedPostAdapter(Context context, List<Post> savedPostList, OnUnsaveClickListener listener) {
        this.context = context;
        this.savedPostList = savedPostList;
        this.unsaveListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Post> newList) {
        this.savedPostList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_post, parent, false);
        return new SavedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedViewHolder holder, int position) {
        Post post = savedPostList.get(position);

        // 1. Gán nội dung thu gọn
        holder.tvPostContent.setText(post.getContent());

        // 2. Gán Tác giả & Avatar
        if (post.getAuthorId() != null) {
            holder.tvAuthorName.setText(post.getAuthorId().getUsername() + " • Đã lưu");
            Glide.with(context).load(post.getAuthorId().getAvatar())
                    .placeholder(R.drawable.ic_user).into(holder.imgAuthorAvatar);
        } else {
            holder.tvAuthorName.setText("Ẩn danh • Đã lưu");
        }

        // ==========================================
        // 3. XỬ LÝ THUMBNAIL (ĐÃ SỬA THEO Ý BẠN)
        // ==========================================
        // Luôn luôn hiện cái khung chứa ảnh (không bao giờ ẩn)
        holder.cardThumbnail.setVisibility(View.VISIBLE);

        if (post.getImages() != null && !post.getImages().isEmpty()) {
            // Nếu bài viết có ảnh -> Lấy ảnh đầu tiên (.get(0)) đắp vào
            Glide.with(context).load(post.getImages().get(0))
                    .placeholder(R.drawable.ic_image)
                    .into(holder.imgPostThumbnail);
        } else {
            // Nếu bài viết KHÔNG CÓ ẢNH -> Đắp một cái ảnh mặc định vào cho khỏi trống
            // Bạn có thể đổi R.drawable.ic_image thành R.drawable.logo nếu muốn hiện logo app
            holder.imgPostThumbnail.setImageResource(R.drawable.ic_image);
        }

        // 4. Sự kiện bấm nút 3 chấm -> Bỏ lưu
        holder.btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.btnMoreOptions);
            popupMenu.getMenu().add(Menu.NONE, 1, 1, "Bỏ lưu bài viết");

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    if (unsaveListener != null) {
                        unsaveListener.onUnsavePost(post.getId());
                    }
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        // 5. Sự kiện bấm vào Bài viết -> Mở trang Chi tiết
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("POST_ID", post.getId());
            intent.putExtra("POST_CONTENT", post.getContent());
            if (post.getAuthorId() != null) {
                intent.putExtra("AUTHOR_NAME", post.getAuthorId().getUsername());
                intent.putExtra("AUTHOR_AVATAR", post.getAuthorId().getAvatar());
            }
            if (post.getImages() != null) {
                intent.putStringArrayListExtra("POST_IMAGES", new ArrayList<>(post.getImages()));
            }
            intent.putExtra("COMMENT_COUNT", post.getcountComment());
            intent.putExtra("REACTION_COUNT", post.getcountReaction());
            intent.putExtra("MY_REACTION", post.getMyReaction());
            if (post.getTopReactions() != null) {
                intent.putStringArrayListExtra("TOP_REACTIONS", new ArrayList<>(post.getTopReactions()));
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return savedPostList != null ? savedPostList.size() : 0;
    }

    public static class SavedViewHolder extends RecyclerView.ViewHolder {
        CardView cardThumbnail;
        ImageView imgPostThumbnail, imgAuthorAvatar, btnMoreOptions;
        TextView tvPostContent, tvAuthorName;

        public SavedViewHolder(@NonNull View itemView) {
            super(itemView);
            cardThumbnail = itemView.findViewById(R.id.cardThumbnail);
            imgPostThumbnail = itemView.findViewById(R.id.imgPostThumbnail);
            imgAuthorAvatar = itemView.findViewById(R.id.imgAuthorAvatar);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
        }
    }
}