package com.example.frontend.ui.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Post;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnReactionListener {
        void onReactClick(String targetId, String type);
    }

    private List<Post> postList;
    private Context context;
    private OnReactionListener reactionListener;

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

        holder.tvContent.setText(post.getContent());
        if (post.getAuthorId() != null) {
            holder.tvUserName.setText(post.getAuthorId().getUsername());
            Glide.with(context).load(post.getAuthorId().getAvatar()).placeholder(R.drawable.ic_user).into(holder.imgAvatar);
        } else {
            holder.tvUserName.setText("Người dùng ẩn danh");
        }

        if (post.getImage() != null && !post.getImage().isEmpty()) {
            holder.imgPost.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getImage()).into(holder.imgPost);
        } else {
            holder.imgPost.setVisibility(View.GONE);
        }

        if (holder.tvCommentCount != null) {
            holder.tvCommentCount.setText(String.valueOf(post.getcountComment()));
            holder.tvCommentCount.setVisibility(View.VISIBLE);
        }

        // ==========================================
        // KHU VỰC TOP REACTION (Hiển thị ban đầu)
        // ==========================================
        int reactCount = post.getcountReaction();
        List<String> topReactions = post.getTopReactions();

        if (reactCount > 0) {
            holder.layoutTopReactions.setVisibility(View.VISIBLE);
            holder.tvReactionCount.setText(String.valueOf(reactCount));

            holder.imgReact1.setVisibility(View.GONE);
            holder.imgReact2.setVisibility(View.GONE);

            if (topReactions != null && !topReactions.isEmpty()) {
                holder.imgReact1.setVisibility(View.VISIBLE);
                holder.imgReact1.setImageResource(getIconForReaction(topReactions.get(0)));

                if (topReactions.size() > 1) {
                    holder.imgReact2.setVisibility(View.VISIBLE);
                    holder.imgReact2.setImageResource(getIconForReaction(topReactions.get(1)));
                }
            }
        } else {
            holder.layoutTopReactions.setVisibility(View.GONE);
        }

        // Mở BottomSheet khi nhấn vào số lượng reaction
        holder.layoutTopReactions.setOnClickListener(v -> {
            if (context instanceof AppCompatActivity) {
                ReactionListBottomSheet bottomSheet = ReactionListBottomSheet.newInstance(post.getId());
                bottomSheet.show(((AppCompatActivity) context).getSupportFragmentManager(), "ReactionBottomSheet");
            }
        });

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

        // ==========================================
        // KHU VỰC NÚT LIKE (Thả cảm xúc)
        // ==========================================
        if (holder.btnLikeContainer != null) {
            String currentReaction = post.getMyReaction();
            holder.imgLikeIcon.setImageResource(getIconForReaction(currentReaction));

            if (currentReaction != null) {
                holder.tvLikeLabel.setText(currentReaction);
            } else {
                holder.tvLikeLabel.setText("Thích");
            }

            holder.btnLikeContainer.setOnClickListener(v -> {
                String newReaction = (post.getMyReaction() != null) ? null : "Like";
                handleReactionUpdate(holder, post, newReaction);
            });

            holder.btnLikeContainer.setOnLongClickListener(v -> {
                View popupView = LayoutInflater.from(context).inflate(R.layout.item_feed_reaction_popup, null);
                PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                ImageView btnReactLike = popupView.findViewById(R.id.btnReactLike);
                ImageView btnReactLove = popupView.findViewById(R.id.btnReactLove);
                ImageView btnReactHaha = popupView.findViewById(R.id.btnReactHaha);
                ImageView btnReactWow = popupView.findViewById(R.id.btnReactWow);
                ImageView btnReactSad = popupView.findViewById(R.id.btnReactSad);
                ImageView btnReactAngry = popupView.findViewById(R.id.btnReactAngry);

                btnReactLike.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Like"); popupWindow.dismiss(); });
                btnReactLove.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Love"); popupWindow.dismiss(); });
                btnReactHaha.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Haha"); popupWindow.dismiss(); });
                btnReactWow.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Wow"); popupWindow.dismiss(); });
                btnReactSad.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Sad"); popupWindow.dismiss(); });
                btnReactAngry.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Angry"); popupWindow.dismiss(); });

                popupWindow.showAsDropDown(v, 0, -v.getHeight() - 140);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() { return postList != null ? postList.size() : 0; }

    // ==========================================
    // HÀM XỬ LÝ REAL-TIME KHI BẤM THẢ TIM
    // (Đã fix lỗi kẹt 2 icon khi đổi cảm xúc)
    // ==========================================
    private void handleReactionUpdate(PostViewHolder holder, Post post, String newReactionType) {
        String oldReaction = post.getMyReaction();
        int currentCount = post.getcountReaction();
        List<String> topReactions = post.getTopReactions();

        if (topReactions == null) topReactions = new ArrayList<>();

        // Logic tăng/giảm số lượng và thay đổi Icon
        if (oldReaction == null && newReactionType != null) {
            // Trường hợp 1: Thả mới hoàn toàn
            currentCount++;
            if (!topReactions.contains(newReactionType)) topReactions.add(0, newReactionType);
        } else if (oldReaction != null && newReactionType == null) {
            // Trường hợp 2: Hủy thả cảm xúc
            currentCount--;
            if (currentCount <= 0) {
                topReactions.clear();
            } else {
                topReactions.remove(oldReaction);
            }
        } else if (oldReaction != null && newReactionType != null && !oldReaction.equals(newReactionType)) {
            // Trường hợp 3: Đổi từ cảm xúc này sang cảm xúc khác
            topReactions.remove(oldReaction); // Bắt buộc xóa cái cũ đi
            if (!topReactions.contains(newReactionType)) {
                topReactions.add(0, newReactionType); // Đẩy cái mới lên đầu
            }
        }

        // Đảm bảo không hiển thị quá 2 icon gây tràn layout
        if (topReactions.size() > 2) {
            topReactions = new ArrayList<>(topReactions.subList(0, 2));
        }

        // Lưu ngược lại vào Bài viết
        post.setMyReaction(newReactionType);
        post.setcountReaction(currentCount);
        post.setTopReactions(topReactions);

        // Đổi màu và chữ nút Like
        holder.imgLikeIcon.setImageResource(getIconForReaction(newReactionType));
        if (newReactionType != null) holder.tvLikeLabel.setText(newReactionType);
        else holder.tvLikeLabel.setText("Thích");

        // Cập nhật Vùng hiển thị số lượng ở phía trên (Tránh bị tàng hình)
        if (currentCount > 0) {
            holder.layoutTopReactions.setVisibility(View.VISIBLE);
            holder.tvReactionCount.setText(String.valueOf(currentCount));

            holder.imgReact1.setVisibility(View.GONE);
            holder.imgReact2.setVisibility(View.GONE);

            if (!topReactions.isEmpty()) {
                holder.imgReact1.setVisibility(View.VISIBLE);
                holder.imgReact1.setImageResource(getIconForReaction(topReactions.get(0)));
                if (topReactions.size() > 1) {
                    holder.imgReact2.setVisibility(View.VISIBLE);
                    holder.imgReact2.setImageResource(getIconForReaction(topReactions.get(1)));
                }
            }
        } else {
            holder.layoutTopReactions.setVisibility(View.GONE);
        }

        // Gọi API Backend
        if (reactionListener != null) {
            String typeToSend = newReactionType != null ? newReactionType : oldReaction;
            reactionListener.onReactClick(post.getId(), typeToSend);
        }
    }

    private int getIconForReaction(String type) {
        if (type == null) return R.drawable.ic_like;
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

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvContent, tvCommentCount;
        ImageView imgAvatar, imgPost;
        View btnComment;

        LinearLayout layoutTopReactions;
        TextView tvReactionCount;
        ImageView imgReact1, imgReact2;

        LinearLayout btnLikeContainer;
        ImageView imgLikeIcon;
        TextView tvLikeLabel;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvAuthorName);
            tvContent = itemView.findViewById(R.id.tvContent);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgPost = itemView.findViewById(R.id.imgPost);
            btnComment = itemView.findViewById(R.id.btnComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);

            layoutTopReactions = itemView.findViewById(R.id.layoutTopReactions);
            tvReactionCount = itemView.findViewById(R.id.tvReactionCount);
            imgReact1 = itemView.findViewById(R.id.imgReact1);
            imgReact2 = itemView.findViewById(R.id.imgReact2);

            btnLikeContainer = itemView.findViewById(R.id.btnLike);
            imgLikeIcon = itemView.findViewById(R.id.imgLike);
            tvLikeLabel = itemView.findViewById(R.id.tvLikeCount);
        }
    }
}