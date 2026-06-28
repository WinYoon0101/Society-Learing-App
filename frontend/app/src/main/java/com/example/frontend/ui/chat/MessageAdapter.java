package com.example.frontend.ui.chat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Message;
import com.example.frontend.data.model.Reaction;
import com.example.frontend.data.model.User;
import com.example.frontend.ui.feed.PostDetailActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SYSTEM = 3;

    public interface OnReactionClickListener {
        void onLongPress(Message message, View anchor);
        void onReactionChipClick(Message message, String emoji);
        void onReplyClick(Message message);
        void onQuoteClick(String replyToMessageId);
        void onMoreClick(Message message, View anchor);
    }

    private List<Message> messages = new ArrayList<>();
    private String currentUserId;
    private OnReactionClickListener reactionClickListener;
    // Action bar đang hiện (chỉ cho phép 1 message hiện action bar tại một thời điểm)
    private View visibleActionBar;
    // Highlight tạm thời khi scroll tới message gốc (bấm quote)
    private static final int HIGHLIGHT_COLOR = 0x334A7C59;
    private int highlightPosition = -1;
    private final Handler highlightHandler = new Handler(Looper.getMainLooper());

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnReactionClickListener(OnReactionClickListener listener) {
        this.reactionClickListener = listener;
    }

    public void submitList(List<Message> list) {
        this.messages = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateReactions(String messageId, List<Reaction> reactions) {
        if (messageId == null) return;
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (messageId.equals(m.getId())) {
                Message updated = new Message(
                        m.getId(),
                        m.getConversationId(),
                        m.getSender(),
                        m.getText(),
                        m.getReplyTo(),
                        reactions != null ? reactions : new ArrayList<>(),
                        m.isDeleted(),
                        m.isSystem(),
                        m.getMediaUrl(),
                        m.getMediaType(),
                        m.getCreatedAt(),
                        m.getUpdatedAt()
                );
                messages.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (msg.isSystem()) {
            return VIEW_TYPE_SYSTEM;
        }
        if (msg.getSender() != null && msg.getSender().getId() != null) {
            String senderId = msg.getSender().getId().trim();
            String myId = currentUserId != null ? currentUserId.trim() : "";
            android.util.Log.d("MessageAdapter", "SenderId: [" + senderId + "] | CurrentId: [" + myId + "] | Match: " + senderId.equals(myId));

            if (senderId.equals(myId)) {
                return VIEW_TYPE_SENT;
            }
        }
        return VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SYSTEM) {
            View view = inflater.inflate(R.layout.item_message_system, parent, false);
            return new SystemViewHolder(view);
        } else if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).bind(msg);
            return;
        } else if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(msg);
        } else {
            ((ReceivedViewHolder) holder).bind(msg);
        }
        holder.itemView.setBackgroundColor(
                position == highlightPosition ? HIGHLIGHT_COLOR : Color.TRANSPARENT);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /** Vị trí message theo id trong trang đang load; -1 nếu không có. */
    public int indexOfMessage(String messageId) {
        if (messageId == null) return -1;
        String target = messageId.trim();
        for (int i = 0; i < messages.size(); i++) {
            String id = messages.get(i).getId();
            if (id != null && target.equals(id.trim())) return i;
        }
        return -1;
    }

    /** Nhấp nháy highlight message ở vị trí cho trước (~1.2s) — dùng khi bấm quote. */
    public void flashMessage(int position) {
        if (position < 0 || position >= messages.size()) return;
        int old = highlightPosition;
        highlightPosition = position;
        if (old >= 0 && old != position) notifyItemChanged(old);
        notifyItemChanged(position);

        highlightHandler.removeCallbacksAndMessages(null);
        highlightHandler.postDelayed(() -> {
            int p = highlightPosition;
            highlightPosition = -1;
            if (p >= 0) notifyItemChanged(p);
        }, 1200);
    }

    private void bindReactions(Message message, View scrollContainer, LinearLayout chipContainer) {
        List<Reaction> reactions = message.getReactions();
        if (reactions == null || reactions.isEmpty()) {
            scrollContainer.setVisibility(View.GONE);
            chipContainer.removeAllViews();
            return;
        }

        // Group emoji → list of userIds (preserve insertion order)
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (Reaction r : reactions) {
            if (r.getEmoji() == null) continue;
            List<String> users = grouped.get(r.getEmoji());
            if (users == null) {
                users = new ArrayList<>();
                grouped.put(r.getEmoji(), users);
            }
            users.add(r.getUserId() != null ? r.getUserId().trim() : "");
        }

        chipContainer.removeAllViews();
        String myId = currentUserId != null ? currentUserId.trim() : "";

        for (Map.Entry<String, List<String>> e : grouped.entrySet()) {
            String emoji = e.getKey();
            List<String> userIds = e.getValue();
            boolean mine = userIds.contains(myId);

            TextView chip = new TextView(chipContainer.getContext());
            chip.setText(emoji + " " + userIds.size());
            chip.setTextSize(12f);
            chip.setTextColor(0xFF333333);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            int padH = dp(chipContainer, 8);
            int padV = dp(chipContainer, 4);
            chip.setPadding(padH, padV, padH, padV);
            chip.setBackgroundResource(mine
                    ? R.drawable.bg_reaction_chip_mine
                    : R.drawable.bg_reaction_chip);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(chipContainer, 4));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                if (reactionClickListener != null) {
                    reactionClickListener.onReactionChipClick(message, emoji);
                }
            });

            chipContainer.addView(chip);
        }

        scrollContainer.setVisibility(View.VISIBLE);
    }

    private boolean bindSharedPost(Message message, View card, TextView tvMessage,
                                   TextView tvAuthor, TextView tvContent, ImageView imgPreview) {
        SharedPostMessage sharedPost = SharedPostMessage.parse(message.getText());
        if (sharedPost == null) {
            card.setVisibility(View.GONE);
            card.setOnClickListener(null);
            return false;
        }

        tvMessage.setVisibility(View.GONE);
        card.setVisibility(View.VISIBLE);
        String author = sharedPost.getAuthorName();
        tvAuthor.setText(!TextUtils.isEmpty(author) ? author : "Bài viết");

        String content = sharedPost.getContent();
        tvContent.setText(!TextUtils.isEmpty(content) ? content : "Nhấn để xem bài viết");

        String imageUrl = sharedPost.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            imgPreview.setVisibility(View.VISIBLE);
            Glide.with(imgPreview.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.bg_rounded_image_placeholder)
                    .error(R.drawable.bg_rounded_image_placeholder)
                    .into(imgPreview);
        } else {
            imgPreview.setVisibility(View.GONE);
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PostDetailActivity.class);
            intent.putExtra("POST_ID", sharedPost.getPostId());
            v.getContext().startActivity(intent);
        });
        return true;
    }

    private int dp(View v, int dp) {
        return Math.round(v.getResources().getDisplayMetrics().density * dp);
    }

    /** Hiển thị block quote tin nhắn gốc khi message là reply. */
    private void bindReplyQuote(Message message, View quoteRoot, TextView sender, TextView text) {
        Message replyTo = message.getReplyTo();
        if (quoteRoot == null) return;
        if (replyTo == null) {
            quoteRoot.setVisibility(View.GONE);
            return;
        }
        String name = (replyTo.getSender() != null && replyTo.getSender().getUsername() != null)
                ? replyTo.getSender().getUsername() : "";
        sender.setText(name);

        final String replyToId = replyTo.getId();
        quoteRoot.setOnClickListener(v -> {
            if (reactionClickListener != null && replyToId != null) {
                reactionClickListener.onQuoteClick(replyToId);
            }
        });

        String snippet = SharedPostMessage.previewText(replyTo.getText());
        if (snippet == null || snippet.isEmpty()) {
            if (replyTo.getMediaUrl() != null && !replyTo.getMediaUrl().isEmpty()) {
                String mt = replyTo.getMediaType();
                if ("image".equals(mt)) snippet = "[Ảnh]";
                else if ("video".equals(mt)) snippet = "[Video]";
                else snippet = "[Tệp]";
            } else {
                snippet = "";
            }
        }
        text.setText(snippet);
        quoteRoot.setVisibility(View.VISIBLE);
    }

    /** Hiện 1 action bar, ẩn cái đang hiện trước đó (chỉ 1 message hiện tại 1 thời điểm). */
    private void showActionBar(View actionBar) {
        if (visibleActionBar != null && visibleActionBar != actionBar) {
            visibleActionBar.setVisibility(View.GONE);
        }
        visibleActionBar = actionBar;
        actionBar.setVisibility(View.VISIBLE);
    }

    private void hideActionBar(View actionBar) {
        actionBar.setVisibility(View.GONE);
        if (visibleActionBar == actionBar) {
            visibleActionBar = null;
        }
    }

    /** Action bar (reply + react + more) ẩn mặc định; hiện khi long-press (mobile) / hover (chuột). */
    private void wireActionBar(Message message, View bubble, View itemRoot,
                               View actionBar, ImageButton btnReact, ImageButton btnReply,
                               ImageButton btnMore) {
        actionBar.setVisibility(View.GONE);

        bubble.setOnLongClickListener(v -> {
            showActionBar(actionBar);
            return true;
        });

        itemRoot.setOnHoverListener((v, e) -> {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_HOVER_ENTER) {
                showActionBar(actionBar);
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                hideActionBar(actionBar);
            }
            return false;
        });

        btnReact.setOnClickListener(v -> {
            if (reactionClickListener != null) {
                reactionClickListener.onLongPress(message, v);
            }
        });
        btnReply.setOnClickListener(v -> {
            if (reactionClickListener != null) {
                reactionClickListener.onReplyClick(message);
            }
        });
        btnMore.setOnClickListener(v -> {
            if (reactionClickListener != null) {
                reactionClickListener.onMoreClick(message, v);
            }
        });
    }

    /** Nếu message đã thu hồi → render placeholder, ẩn mọi tương tác. Trả true nếu đã xử lý. */
    private boolean bindRecalled(Message message, View itemRoot, TextView tvMessage, TextView tvTime,
                                 View actionBar, View reactionScroll, View replyQuote,
                                 View imgMedia, View fileLayout) {
        if (!message.isDeleted()) {
            tvMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            return false;
        }
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText("Tin nhắn đã bị thu hồi");
        tvMessage.setTypeface(null, android.graphics.Typeface.ITALIC);
        if (message.getCreatedAt() != null) tvTime.setText(formatTime(message.getCreatedAt()));
        actionBar.setVisibility(View.GONE);
        reactionScroll.setVisibility(View.GONE);
        if (replyQuote != null) replyQuote.setVisibility(View.GONE);
        imgMedia.setVisibility(View.GONE);
        fileLayout.setVisibility(View.GONE);
        tvMessage.setOnLongClickListener(null);
        itemRoot.setOnHoverListener(null);
        return true;
    }

    /** Xóa 1 message khỏi list (xóa-phía-mình). */
    public void removeMessage(String messageId) {
        if (messageId == null) return;
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getId())) {
                messages.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    /** Đánh dấu 1 message đã thu hồi (cả 2) → hiển thị placeholder. */
    public void markRecalled(String messageId) {
        if (messageId == null) return;
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (messageId.equals(m.getId())) {
                Message updated = new Message(
                        m.getId(), m.getConversationId(), m.getSender(),
                        "Tin nhắn đã bị thu hồi", m.getReplyTo(), new ArrayList<>(),
                        true, m.isSystem(), null, null, m.getCreatedAt(), m.getUpdatedAt());
                messages.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    private void bindMedia(Message message, ImageView imgPreview,
                           LinearLayout layoutFile, TextView tvIcon, TextView tvName) {
        if (message.getMediaUrl() == null || message.getMediaUrl().isEmpty()) {
            imgPreview.setVisibility(View.GONE);
            layoutFile.setVisibility(View.GONE);
            return;
        }

        String mediaType = message.getMediaType();
        if ("image".equals(mediaType)) {
            imgPreview.setVisibility(View.VISIBLE);
            layoutFile.setVisibility(View.GONE);
            Glide.with(imgPreview.getContext())
                    .load(message.getMediaUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_user)
                    .into(imgPreview);
        } else {
            imgPreview.setVisibility(View.GONE);
            layoutFile.setVisibility(View.VISIBLE);
            String url = message.getMediaUrl();
            String filename = url.contains("/") ? url.substring(url.lastIndexOf("/") + 1) : url;
            tvName.setText(filename);
            tvIcon.setText("video".equals(mediaType) ? "🎬" : "📄");
        }
    }

    class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        View reactionScroll;
        LinearLayout reactionContainer;
        View messageActionBar;
        ImageButton btnReact, btnReply, btnMore;
        View replyQuote;
        TextView tvReplyQuoteSender, tvReplyQuoteText;
        View sharedPostCard;
        TextView tvSharedPostAuthor, tvSharedPostContent;
        ImageView imgSharedPostImage;
        ImageView imgMediaPreview;
        LinearLayout layoutFilePreview;
        TextView tvFileIcon, tvFileName;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvSentMessage);
            tvTime = itemView.findViewById(R.id.tvSentTime);
            reactionScroll = itemView.findViewById(R.id.reactionBarScroll);
            reactionContainer = itemView.findViewById(R.id.reactionBarContainer);
            messageActionBar = itemView.findViewById(R.id.messageActionBar);
            btnReact = itemView.findViewById(R.id.btnReact);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnMore = itemView.findViewById(R.id.btnMore);
            replyQuote = itemView.findViewById(R.id.replyQuote);
            tvReplyQuoteSender = itemView.findViewById(R.id.tvReplyQuoteSender);
            tvReplyQuoteText = itemView.findViewById(R.id.tvReplyQuoteText);
            sharedPostCard = itemView.findViewById(R.id.sharedPostCard);
            tvSharedPostAuthor = itemView.findViewById(R.id.tvSharedPostAuthor);
            tvSharedPostContent = itemView.findViewById(R.id.tvSharedPostContent);
            imgSharedPostImage = itemView.findViewById(R.id.imgSharedPostImage);
            imgMediaPreview = itemView.findViewById(R.id.imgMediaPreview);
            layoutFilePreview = itemView.findViewById(R.id.layoutFilePreview);
            tvFileIcon = itemView.findViewById(R.id.tvFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
        }

        void bind(Message message) {
            sharedPostCard.setVisibility(View.GONE);
            if (bindRecalled(message, itemView, tvMessage, tvTime, messageActionBar,
                    reactionScroll, replyQuote, imgMediaPreview, layoutFilePreview)) {
                return;
            }
            String text = message.getText();
            boolean isSharedPost = bindSharedPost(
                    message,
                    sharedPostCard,
                    tvMessage,
                    tvSharedPostAuthor,
                    tvSharedPostContent,
                    imgSharedPostImage
            );
            if (isSharedPost) {
                // Card handles its own visible content.
            } else if (text == null || text.isEmpty()) {
                tvMessage.setVisibility(View.GONE);
            } else {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(text);
            }
            if (message.getCreatedAt() != null) {
                tvTime.setText(formatTime(message.getCreatedAt()));
            }
            wireActionBar(message, isSharedPost ? sharedPostCard : tvMessage,
                    itemView, messageActionBar, btnReact, btnReply, btnMore);
            bindReplyQuote(message, replyQuote, tvReplyQuoteSender, tvReplyQuoteText);
            bindMedia(message, imgMediaPreview, layoutFilePreview, tvFileIcon, tvFileName);
            bindReactions(message, reactionScroll, reactionContainer);
        }
    }

    class ReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvMessage, tvTime;
        View reactionScroll;
        LinearLayout reactionContainer;
        View messageActionBar;
        ImageButton btnReact, btnReply, btnMore;
        View replyQuote;
        TextView tvReplyQuoteSender, tvReplyQuoteText;
        View sharedPostCard;
        TextView tvSharedPostAuthor, tvSharedPostContent;
        ImageView imgSharedPostImage;
        ImageView imgMediaPreview;
        LinearLayout layoutFilePreview;
        TextView tvFileIcon, tvFileName;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgReceivedAvatar);
            tvMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvTime = itemView.findViewById(R.id.tvReceivedTime);
            reactionScroll = itemView.findViewById(R.id.reactionBarScroll);
            reactionContainer = itemView.findViewById(R.id.reactionBarContainer);
            messageActionBar = itemView.findViewById(R.id.messageActionBar);
            btnReact = itemView.findViewById(R.id.btnReact);
            btnReply = itemView.findViewById(R.id.btnReply);
            btnMore = itemView.findViewById(R.id.btnMore);
            replyQuote = itemView.findViewById(R.id.replyQuote);
            tvReplyQuoteSender = itemView.findViewById(R.id.tvReplyQuoteSender);
            tvReplyQuoteText = itemView.findViewById(R.id.tvReplyQuoteText);
            sharedPostCard = itemView.findViewById(R.id.sharedPostCard);
            tvSharedPostAuthor = itemView.findViewById(R.id.tvSharedPostAuthor);
            tvSharedPostContent = itemView.findViewById(R.id.tvSharedPostContent);
            imgSharedPostImage = itemView.findViewById(R.id.imgSharedPostImage);
            imgMediaPreview = itemView.findViewById(R.id.imgMediaPreview);
            layoutFilePreview = itemView.findViewById(R.id.layoutFilePreview);
            tvFileIcon = itemView.findViewById(R.id.tvFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
        }

        void bind(Message message) {
            User sender = message.getSender();
            if (sender != null && sender.getAvatar() != null) {
                Glide.with(itemView.getContext())
                        .load(sender.getAvatar())
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_user);
            }

            sharedPostCard.setVisibility(View.GONE);
            if (bindRecalled(message, itemView, tvMessage, tvTime, messageActionBar,
                    reactionScroll, replyQuote, imgMediaPreview, layoutFilePreview)) {
                return;
            }
            String text = message.getText();
            boolean isSharedPost = bindSharedPost(
                    message,
                    sharedPostCard,
                    tvMessage,
                    tvSharedPostAuthor,
                    tvSharedPostContent,
                    imgSharedPostImage
            );
            if (isSharedPost) {
                // Card handles its own visible content.
            } else if (text == null || text.isEmpty()) {
                tvMessage.setVisibility(View.GONE);
            } else {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(text);
            }
            if (message.getCreatedAt() != null) {
                tvTime.setText(formatTime(message.getCreatedAt()));
            }

            wireActionBar(message, isSharedPost ? sharedPostCard : tvMessage,
                    itemView, messageActionBar, btnReact, btnReply, btnMore);
            bindReplyQuote(message, replyQuote, tvReplyQuoteSender, tvReplyQuoteText);
            bindMedia(message, imgMediaPreview, layoutFilePreview, tvFileIcon, tvFileName);
            bindReactions(message, reactionScroll, reactionContainer);
        }
    }

    /** Tin nhắn hệ thống: chữ xám nhỏ căn giữa, không tương tác. */
    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvSystem;

        SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystem = itemView.findViewById(R.id.tvSystemMessage);
        }

        void bind(Message message) {
            tvSystem.setText(message.getText());
        }
    }

    private String formatTime(java.util.Date date) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
    }
}
