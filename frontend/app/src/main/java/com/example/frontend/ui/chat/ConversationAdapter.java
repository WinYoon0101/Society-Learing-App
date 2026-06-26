package com.example.frontend.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<Conversation> conversations = new ArrayList<>();
    private Set<String> onlineIds = new HashSet<>();
    private String currentUserId;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation, User otherMember);
    }

    private OnConversationClickListener listener;

    public ConversationAdapter(String currentUserId, OnConversationClickListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void submitList(List<Conversation> list) {
        this.conversations = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnlineIds(Set<String> ids) {
        this.onlineIds = ids != null ? ids : new HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(conversations.get(position));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgConvMuted;
        TextView tvName, tvLastMessage, tvTime;
        View viewOnlineDot, viewUnreadDot;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgConvAvatar);
            tvName = itemView.findViewById(R.id.tvConvName);
            tvLastMessage = itemView.findViewById(R.id.tvConvLastMessage);
            tvTime = itemView.findViewById(R.id.tvConvTime);
            viewOnlineDot = itemView.findViewById(R.id.viewConvOnlineDot);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            imgConvMuted = itemView.findViewById(R.id.imgConvMuted);
        }

        void bind(Conversation conversation) {
            User otherMember = getOtherMember(conversation);

            if (conversation.isGroup()) {
                // Group: tên ghép thành viên (hoặc tên đặt), không có chấm online, avatar nhóm
                tvName.setText(ChatNameUtils.groupDisplayName(
                        conversation.getName(), conversation.getMembers(), currentUserId));
                viewOnlineDot.setVisibility(View.GONE);
                imgAvatar.setImageResource(R.drawable.ic_group);
            } else if (otherMember != null) {
                tvName.setText(resolveDisplayName(conversation, otherMember));
                boolean online = otherMember.getId() != null && onlineIds.contains(otherMember.getId());
                viewOnlineDot.setVisibility(online ? View.VISIBLE : View.GONE);

                Glide.with(itemView.getContext())
                        .load(otherMember.getAvatar())
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(imgAvatar);
            } else {
                tvName.setText("Conversation");
                viewOnlineDot.setVisibility(View.GONE);
                imgAvatar.setImageResource(R.drawable.ic_user);
            }

            // Last message
            if (conversation.getLastMessage() != null) {
                String text = conversation.getLastMessage().getText();
                boolean isSystem = conversation.getLastMessage().isSystem();
                String senderName = "";
                User sender = conversation.getLastMessage().getSender();
                if (!isSystem && sender != null && sender.getId() != null
                        && sender.getId().equals(currentUserId)) {
                    senderName = "Bạn: ";
                }
                tvLastMessage.setText(senderName + (text != null ? text : ""));
            } else {
                tvLastMessage.setText("Bắt đầu cuộc trò chuyện...");
            }

            // Đã tắt thông báo (tin nhắn HOẶC cuộc gọi) → hiện icon chuông gạch
            boolean muted = conversation.getMuted() || conversation.getMutedCall();
            imgConvMuted.setVisibility(muted ? View.VISIBLE : View.GONE);

            // Chấm xanh dương "chưa đọc" (kiểu Messenger) — BE tính sẵn cờ unread (đã loại muted)
            boolean unread = conversation.getUnread();
            viewUnreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
            // Chưa đọc → preview tin nhắn đậm + tối hơn (giống Messenger); tên giữ nguyên
            tvLastMessage.setTypeface(null, unread ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            tvLastMessage.setTextColor(unread ? 0xFF1A1A1A : 0xFF888888);

            // Time
            Date timeDate = conversation.getUpdatedAt();
            if (timeDate != null) {
                tvTime.setText(formatTime(timeDate));
                // Green color for recent (< 1 hour)
                long diffMs = System.currentTimeMillis() - timeDate.getTime();
                if (diffMs < 60 * 60 * 1000L) {
                    tvTime.setTextColor(0xFF5C8B70);
                } else {
                    tvTime.setTextColor(0xFF888888);
                }
            } else {
                tvTime.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(conversation, otherMember);
                }
            });
        }

        /** Ưu tiên biệt danh (conversation.nicknames[otherId]) nếu có, else username gốc. */
        private String resolveDisplayName(Conversation conversation, User otherMember) {
            Map<String, String> nicknames = conversation.getNicknames();
            if (nicknames != null && otherMember.getId() != null) {
                String nn = nicknames.get(otherMember.getId());
                if (nn != null && !nn.trim().isEmpty()) {
                    return nn;
                }
            }
            return otherMember.getUsername();
        }

        private User getOtherMember(Conversation conversation) {
            if (conversation.getMembers() == null) return null;
            for (User member : conversation.getMembers()) {
                if (member.getId() != null && !member.getId().equals(currentUserId)) {
                    return member;
                }
            }
            // Fallback: nếu chỉ có 1 member hoặc chat với chính mình
            if (!conversation.getMembers().isEmpty()) {
                return conversation.getMembers().get(0);
            }
            return null;
        }

        private String formatTime(Date date) {
            long diffMs = System.currentTimeMillis() - date.getTime();
            long minutes = diffMs / (60 * 1000);
            long hours = diffMs / (60 * 60 * 1000);

            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút";
            if (hours < 24) return hours + " giờ";

            Calendar today = Calendar.getInstance();
            Calendar msgDay = Calendar.getInstance();
            msgDay.setTime(date);

            int dayDiff = today.get(Calendar.DAY_OF_YEAR) - msgDay.get(Calendar.DAY_OF_YEAR);
            if (today.get(Calendar.YEAR) == msgDay.get(Calendar.YEAR) && dayDiff == 1) {
                return "Hôm qua";
            }

            return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(date);
        }
    }
}
