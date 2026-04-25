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

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private String currentUserId;

    // 1. KHAI BÁO GIAO TIẾP (INTERFACES)
    private OnReplyClickListener replyClickListener;
    private OnDeleteClickListener deleteClickListener;

    public interface OnReplyClickListener {
        void onReplyClick(String commentId, String userName);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(String commentId, int position);
    }

    // Các hàm Setter để Activity đăng ký lắng nghe
    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
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
                // Ưu tiên reply vào comment gốc để tránh cây quá sâu,
                // nhưng nếu muốn sâu tận cùng thì cứ truyền comment.getId()
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

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnOptions = itemView.findViewById(R.id.btnOptions);
        }
    }
}