package com.example.frontend.ui.feed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Comment;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private String currentUserId;

    private OnReplyClickListener replyClickListener;
    private OnDeleteClickListener deleteClickListener;
    private OnReactionClickListener reactionClickListener;
    private OnReactionChangedListener reactionChangedListener;

    public interface OnReplyClickListener {
        void onReplyClick(String commentId, String userName);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(String commentId, int position);
    }

    public interface OnReactionClickListener {
        void onReactClick(String commentId, String reactionType);
    }

    public interface OnReactionChangedListener {
        void onReactionChanged();
    }

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setOnReactionClickListener(OnReactionClickListener listener) {
        this.reactionClickListener = listener;
    }

    public void setOnReactionChangedListener(OnReactionChangedListener listener) {
        this.reactionChangedListener = listener;
    }

    public CommentAdapter(List<Comment> commentList, String currentUserId) {
        this.commentList = commentList;
        this.currentUserId = currentUserId;
    }

    public void updateData(List<Comment> newCommentList) {
        this.commentList = newCommentList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        Context context = holder.itemView.getContext();

        if (comment.getContent() != null) {
            holder.tvContent.setText(comment.getContent());
        }

        String userName = "Người dùng";
        if (comment.getUserId() != null) {
            userName = comment.getUserId().getUsername();
            holder.tvUserName.setText(userName);
            Glide.with(context)
                    .load(comment.getUserId().getAvatar())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgAvatar);
        }

        // 👉 LOGIC THỤT LỀ BẬC THANG THEO ĐỘ SÂU (DEPTH)
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        int depth = comment.getDepth();

        // Giới hạn thụt tối đa 4 cấp để màn hình không bị đẩy tràn chữ
        int maxVisualDepth = Math.min(depth, 4);
        int marginInPx = (int) (32 * maxVisualDepth * context.getResources().getDisplayMetrics().density);
        params.setMarginStart(marginInPx);

        // Resize Avatar: Root (40dp), Reply con cháu (28dp)
        if (depth > 0) {
            holder.imgAvatar.getLayoutParams().width = (int) (28 * context.getResources().getDisplayMetrics().density);
            holder.imgAvatar.getLayoutParams().height = (int) (28 * context.getResources().getDisplayMetrics().density);
        } else {
            holder.imgAvatar.getLayoutParams().width = (int) (40 * context.getResources().getDisplayMetrics().density);
            holder.imgAvatar.getLayoutParams().height = (int) (40 * context.getResources().getDisplayMetrics().density);
        }
        holder.itemView.setLayoutParams(params);

        // NÚT PHẢN HỒI
        if (holder.btnReply != null) {
            String finalUserName = userName;
            holder.btnReply.setOnClickListener(v -> {
                if (replyClickListener != null) {
                    // Truyền ID của comment được click để làm parentId cho bình luận mới
                    replyClickListener.onReplyClick(comment.getId(), finalUserName);
                }
            });
        }

        // TÙY CHỌN XÓA
        if (comment.getUserId() != null && comment.getUserId().getId() != null && currentUserId.equals(comment.getUserId().getId())) {
            holder.btnOptions.setVisibility(View.VISIBLE);
            holder.btnOptions.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, holder.btnOptions);
                popupMenu.getMenu().add(0, 1, 0, "Xóa bình luận");
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1 && deleteClickListener != null) {
                        deleteClickListener.onDeleteClick(comment.getId(), position);
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        } else {
            holder.btnOptions.setVisibility(View.GONE);
        }

        // NÚT THẢ CẢM XÚC
        updateReactionUI(holder, comment);

        if (holder.btnLike != null) {
            holder.btnLike.setOnClickListener(v -> {
                String newType = (comment.getMyReaction() != null) ? null : "Like";
                handleReactionUpdate(holder, comment, newType);
            });

            holder.btnLike.setOnLongClickListener(v -> {
                View popupView = LayoutInflater.from(context).inflate(R.layout.item_feed_reaction_popup, null);
                PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                popupView.findViewById(R.id.btnReactLike).setOnClickListener(x -> { handleReactionUpdate(holder, comment, "Like"); popupWindow.dismiss(); });
                popupView.findViewById(R.id.btnReactLove).setOnClickListener(x -> { handleReactionUpdate(holder, comment, "Love"); popupWindow.dismiss(); });
                popupView.findViewById(R.id.btnReactHaha).setOnClickListener(x -> { handleReactionUpdate(holder, comment, "Haha"); popupWindow.dismiss(); });
                popupView.findViewById(R.id.btnReactWow).setOnClickListener(x -> { handleReactionUpdate(holder, comment, "Wow"); popupWindow.dismiss(); });
                popupView.findViewById(R.id.btnReactSad).setOnClickListener(x -> { handleReactionUpdate(holder, comment, "Sad"); popupWindow.dismiss(); });
                popupView.findViewById(R.id.btnReactAngry).setOnClickListener(x -> { handleReactionUpdate(holder, comment, "Angry"); popupWindow.dismiss(); });

                popupWindow.showAsDropDown(v, 0, -v.getHeight() - 140);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return commentList == null ? 0 : commentList.size();
    }

    private void handleReactionUpdate(CommentViewHolder holder, Comment comment, String newReactionType) {
        if (holder.btnLike != null) {
            holder.btnLike.setEnabled(false);
            holder.btnLike.postDelayed(() -> holder.btnLike.setEnabled(true), 1000);
        }

        String oldReaction = comment.getMyReaction();
        if (oldReaction != null && oldReaction.equals(newReactionType)) return;

        int currentCount = comment.getCountReaction();
        if (oldReaction == null && newReactionType != null) {
            currentCount++;
        } else if (oldReaction != null && newReactionType == null) {
            currentCount--;
        }

        comment.setMyReaction(newReactionType);
        comment.setCountReaction(Math.max(0, currentCount));

        updateReactionUI(holder, comment);

        if (reactionClickListener != null) {
            String typeToSend = (newReactionType != null) ? newReactionType : oldReaction;
            reactionClickListener.onReactClick(comment.getId(), typeToSend);
        }
    }

    private void updateReactionUI(CommentViewHolder holder, Comment comment) {
        if (holder.btnLike != null) {
            if (comment.getMyReaction() != null) {
                holder.btnLike.setText(comment.getMyReaction());
                holder.btnLike.setTextColor(android.graphics.Color.parseColor("#1877F2"));
            } else {
                holder.btnLike.setText("Thích");
                holder.btnLike.setTextColor(android.graphics.Color.parseColor("#65676B"));
            }
        }

        if (comment.getCountReaction() > 0) {
            View.OnClickListener openReactionList = v -> showReactionList(v.getContext(), comment.getId());
            if (holder.tvReactionCount != null) {
                holder.tvReactionCount.setVisibility(View.VISIBLE);
                holder.tvReactionCount.setText(String.valueOf(comment.getCountReaction()));
                holder.tvReactionCount.setOnClickListener(openReactionList);
            }
            if (holder.imgReact1 != null) {
                holder.imgReact1.setVisibility(View.VISIBLE);
                holder.imgReact1.setText(getEmojiForReaction(comment.getMyReaction() != null ? comment.getMyReaction() : "Like"));
                holder.imgReact1.setOnClickListener(openReactionList);
            }
            if (holder.imgReact2 != null) holder.imgReact2.setOnClickListener(openReactionList);
        } else {
            if (holder.tvReactionCount != null) {
                holder.tvReactionCount.setVisibility(View.GONE);
                holder.tvReactionCount.setOnClickListener(null);
            }
            if (holder.imgReact1 != null) {
                holder.imgReact1.setVisibility(View.GONE);
                holder.imgReact1.setOnClickListener(null);
            }
            if (holder.imgReact2 != null) {
                holder.imgReact2.setVisibility(View.GONE);
                holder.imgReact2.setOnClickListener(null);
            }
        }
    }

    private void showReactionList(Context context, String commentId) {
        if (commentId == null || !(context instanceof AppCompatActivity)) return;
        ReactionListBottomSheet bottomSheet = ReactionListBottomSheet.newInstance(commentId);
        bottomSheet.show(((AppCompatActivity) context).getSupportFragmentManager(), "CommentReactionBottomSheet");
    }

    private String getEmojiForReaction(String type) {
        if (type == null) return "👍";
        switch (type) {
            case "Love": return "❤️";
            case "Haha": return "😆";
            case "Wow":  return "😮";
            case "Sad":  return "😢";
            case "Angry":return "😡";
            default: return "👍";
        }
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, btnOptions;
        TextView tvUserName, tvContent, tvTime, btnReply, btnLike;
        TextView imgReact1, imgReact2, tvReactionCount;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnOptions = itemView.findViewById(R.id.btnOptions);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnLike = itemView.findViewById(R.id.btnLike);
            imgReact1 = itemView.findViewById(R.id.imgReact1_comment);
            imgReact2 = itemView.findViewById(R.id.imgReact2_comment);
            tvReactionCount = itemView.findViewById(R.id.tvCommentReactionCount);
        }
    }
}
