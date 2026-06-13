package com.example.frontend.ui.feed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Comment;
import com.example.frontend.data.repository.PostRepository;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.utils.Result;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Toast;

import java.util.List;
import android.view.LayoutInflater;
import android.widget.PopupWindow;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.ArrayList;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private String currentUserId;

    // 1. KHAI BÁO GIAO TIẾP (INTERFACES)
    private OnReplyClickListener replyClickListener;
    private OnDeleteClickListener deleteClickListener;
    private OnReactionChangedListener reactionChangedListener;

    public interface OnReplyClickListener {
        void onReplyClick(String commentId, String userName);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(String commentId, int position);
    }

    public interface OnReactionChangedListener {
        void onReactionChanged(); // Gọi khi reaction thay đổi để reload lại danh sách
    }

    // Các hàm Setter để Activity đăng ký lắng nghe
    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setOnReactionChangedListener(OnReactionChangedListener listener) {
        this.reactionChangedListener = listener;
    }

    // 2. CONSTRUCTOR
    public CommentAdapter(List<Comment> commentList, String currentUserId) {
        this.commentList = commentList;
        this.currentUserId = currentUserId;
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

        // repository to call reaction API
        PostRepository postRepo = new PostRepository(context);

        // 3. ĐỔ DỮ LIỆU CƠ BẢN LÊN GIAO DIỆN
        // Giả sử comment.getUserId() trả về object User chứa username và avatar
        String userName = comment.getUserId().getUsername();
        holder.tvUserName.setText(userName);
        holder.tvContent.setText(comment.getContent());

        // Load Avatar bằng Glide
        Glide.with(context)
                .load(comment.getUserId().getAvatar())
                .placeholder(R.drawable.ic_launcher_background) // Ảnh mặc định nếu lỗi
                .into(holder.imgAvatar);

        // --- Reaction button state ---
        String myReact = comment.getMyReaction();
        holder.btnLike.setText(myReact != null ? myReact : "Thích");

        // --- Top reactions preview ---
        if (comment.getCountReaction() > 0) {
            holder.imgReact1.setVisibility(View.GONE);
            holder.imgReact2.setVisibility(View.GONE);
            holder.tvReactionCount.setVisibility(View.VISIBLE);
            holder.tvReactionCount.setText(String.valueOf(comment.getCountReaction()));
            if (comment.getTopReactions() != null && !comment.getTopReactions().isEmpty()) {
                holder.imgReact1.setVisibility(View.VISIBLE);
                holder.imgReact1.setText(getEmojiForReaction(comment.getTopReactions().get(0)));
                if (comment.getTopReactions().size() > 1) {
                    holder.imgReact2.setVisibility(View.VISIBLE);
                    holder.imgReact2.setText(getEmojiForReaction(comment.getTopReactions().get(1)));
                }
            }
        } else {
            holder.imgReact1.setVisibility(View.GONE);
            holder.imgReact2.setVisibility(View.GONE);
            holder.tvReactionCount.setVisibility(View.GONE);
        }

        holder.btnLike.setOnClickListener(v -> {
            String newType = (comment.getMyReaction() != null) ? null : "Like";

            // optimistic UI
            String old = comment.getMyReaction();
            int oldCount = comment.getCountReaction();

            if (old == null && newType != null) {
                comment.setMyReaction(newType);
                comment.setCountReaction(oldCount + 1);
            } else if (old != null && newType == null) {
                comment.setMyReaction(null);
                comment.setCountReaction(Math.max(0, oldCount - 1));
            }
            holder.btnLike.setText(comment.getMyReaction() != null ? comment.getMyReaction() : "Thích");

            // call API
            postRepo.toggleReaction(comment.getId(), "Comment", newType, new Callback<ApiResponse<Object>>() {
                @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    // server accepted — reload để đảm bảo dữ liệu đúng
                    if (response.isSuccessful() && reactionChangedListener != null) {
                        reactionChangedListener.onReactionChanged();
                    }
                }
                @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    // revert optimistic update
                    comment.setMyReaction(old);
                    comment.setCountReaction(oldCount);
                    holder.btnLike.setText(old != null ? old : "Thích");
                    Toast.makeText(context, "Lỗi kết nối, thử lại", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Long-press to open reaction picker
        holder.btnLike.setOnLongClickListener(v -> {
            View popupView = LayoutInflater.from(context).inflate(R.layout.item_feed_reaction_popup, null);
            PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            TextView btnReactLike = popupView.findViewById(R.id.btnReactLike);
            TextView btnReactLove = popupView.findViewById(R.id.btnReactLove);
            TextView btnReactHaha = popupView.findViewById(R.id.btnReactHaha);
            TextView btnReactWow = popupView.findViewById(R.id.btnReactWow);
            TextView btnReactSad = popupView.findViewById(R.id.btnReactSad);
            TextView btnReactAngry = popupView.findViewById(R.id.btnReactAngry);

            btnReactLike.setOnClickListener(x -> { updateCommentReaction(comment, holder, "Like", postRepo, context); popupWindow.dismiss(); });
            btnReactLove.setOnClickListener(x -> { updateCommentReaction(comment, holder, "Love", postRepo, context); popupWindow.dismiss(); });
            btnReactHaha.setOnClickListener(x -> { updateCommentReaction(comment, holder, "Haha", postRepo, context); popupWindow.dismiss(); });
            btnReactWow.setOnClickListener(x -> { updateCommentReaction(comment, holder, "Wow", postRepo, context); popupWindow.dismiss(); });
            btnReactSad.setOnClickListener(x -> { updateCommentReaction(comment, holder, "Sad", postRepo, context); popupWindow.dismiss(); });
            btnReactAngry.setOnClickListener(x -> { updateCommentReaction(comment, holder, "Angry", postRepo, context); popupWindow.dismiss(); });

            popupWindow.showAsDropDown(v, 0, -v.getHeight() - 140);
            return true;
        });

        // 4. LOGIC THỤT LỀ CHO "PHẢN HỒI" (NESTED COMMENTS)
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (comment.getParentId() != null && !comment.getParentId().isEmpty()) {
            // Nếu là Reply -> Thụt lề trái 48dp
            int marginInPx = (int) (48 * context.getResources().getDisplayMetrics().density);
            params.setMarginStart(marginInPx);

            // Bóp nhỏ Avatar lại (30dp)
            holder.imgAvatar.getLayoutParams().width = (int) (30 * context.getResources().getDisplayMetrics().density);
            holder.imgAvatar.getLayoutParams().height = (int) (30 * context.getResources().getDisplayMetrics().density);
        } else {
            // Nếu là Bình luận gốc -> Sát lề (0dp)
            params.setMarginStart(0);

            // Avatar to bình thường (40dp)
            holder.imgAvatar.getLayoutParams().width = (int) (40 * context.getResources().getDisplayMetrics().density);
            holder.imgAvatar.getLayoutParams().height = (int) (40 * context.getResources().getDisplayMetrics().density);
        }
        holder.itemView.setLayoutParams(params); // Áp dụng thay đổi

        // 5. HIỆN/ẨN NÚT TÙY CHỌN (XÓA)
        // Chỉ hiện nếu ID người comment TRÙNG VỚI ID tài khoản đang đăng nhập
        if (comment.getUserId() != null
                && comment.getUserId().getId() != null
                && currentUserId.equals(comment.getUserId().getId())) {

            holder.btnOptions.setVisibility(View.VISIBLE);
        } else {
            holder.btnOptions.setVisibility(View.GONE);
        }

        // 6. XỬ LÝ SỰ KIỆN BẤM NÚT
        // Bấm "Phản hồi"
        holder.btnReply.setOnClickListener(v -> {
            if (replyClickListener != null) {
                String targetId = (comment.getParentId() != null) ? comment.getParentId() : comment.getId();
                replyClickListener.onReplyClick(targetId, userName);
            }
        });

        // Bấm nút "3 chấm" (Tùy chọn)
        holder.btnOptions.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.btnOptions);
            popupMenu.getMenu().add(0, 1, 0, "Xóa bình luận");

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    if (deleteClickListener != null) {
                        deleteClickListener.onDeleteClick(comment.getId(), position);
                    }
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return commentList == null ? 0 : commentList.size();
    }

    // 7. VIEWHOLDER
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, btnOptions;
        TextView tvUserName, tvContent, tvTime, btnReply;
        TextView btnLike;
        TextView imgReact1, imgReact2;
        TextView tvReactionCount;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnOptions = itemView.findViewById(R.id.btnOptions);
            btnLike = itemView.findViewById(R.id.btnLike);
            imgReact1 = itemView.findViewById(R.id.imgReact1_comment);
            imgReact2 = itemView.findViewById(R.id.imgReact2_comment);
            tvReactionCount = itemView.findViewById(R.id.tvCommentReactionCount);
        }
    }

    private void updateCommentReaction(Comment comment, CommentViewHolder holder, String newType, PostRepository postRepo, Context context) {
        String old = comment.getMyReaction();
        int oldCount = comment.getCountReaction();

        // optimistic update
        if (old == null && newType != null) {
            comment.setMyReaction(newType);
            comment.setCountReaction(oldCount + 1);
        } else if (old != null && newType == null) {
            comment.setMyReaction(null);
            comment.setCountReaction(Math.max(0, oldCount - 1));
        } else if (old != null && newType != null && !old.equals(newType)) {
            comment.setMyReaction(newType);
        }

        // update UI
        holder.btnLike.setText(comment.getMyReaction() != null ? comment.getMyReaction() : "Thích");
        holder.tvReactionCount.setVisibility(comment.getCountReaction() > 0 ? View.VISIBLE : View.GONE);
        holder.tvReactionCount.setText(String.valueOf(comment.getCountReaction()));

        // update top icons simplistically
        List<String> top = comment.getTopReactions() != null ? comment.getTopReactions() : new ArrayList<>();
        if (!top.contains(newType) && newType != null) top.add(0, newType);
        if (top.size() > 2) top = new ArrayList<>(top.subList(0,2));
        comment.setTopReactions(top);
        if (top.size() > 0) {
            holder.imgReact1.setVisibility(View.VISIBLE);
            holder.imgReact1.setText(getEmojiForReaction(top.get(0)));
            if (top.size() > 1) {
                holder.imgReact2.setVisibility(View.VISIBLE);
                holder.imgReact2.setText(getEmojiForReaction(top.get(1)));
            } else holder.imgReact2.setVisibility(View.GONE);
        }

        // call backend
        postRepo.toggleReaction(comment.getId(), "Comment", newType, new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                // accepted — reload để đảm bảo dữ liệu đồng bộ với server
                if (response.isSuccessful() && reactionChangedListener != null) {
                    reactionChangedListener.onReactionChanged();
                }
            }
            @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                // revert
                comment.setMyReaction(old);
                comment.setCountReaction(oldCount);
                holder.btnLike.setText(old != null ? old : "Thích");
                holder.tvReactionCount.setText(String.valueOf(oldCount));
            }
        });
    }

    private String getEmojiForReaction(String type) {
        if (type == null) return "👍";
        switch (type) {
            case "Like": return "👍";
            case "Love": return "❤️";
            case "Haha": return "😆";
            case "Wow":  return "😮";
            case "Sad":  return "😢";
            case "Angry":return "😡";
            default: return "👍";
        }
    }
}