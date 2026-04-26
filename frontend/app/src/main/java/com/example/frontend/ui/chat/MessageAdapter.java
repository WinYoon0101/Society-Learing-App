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
import com.example.frontend.data.model.Message;
import com.example.frontend.data.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages = new ArrayList<>();
    private String currentUserId;

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submitList(List<Message> list) {
        this.messages = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (msg.getSender() != null && msg.getSender().getId() != null) {
            String senderId = msg.getSender().getId();
            String logMsg = "SenderId: " + senderId + " | CurrentId: " + currentUserId + " | Match: " + senderId.equals(currentUserId);
            android.util.Log.d("MessageAdapter", logMsg);

            if (senderId.equals(currentUserId)) {
                return VIEW_TYPE_SENT;
            }
        }
        return VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
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
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(msg);
        } else {
            ((ReceivedViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvSentMessage);
            tvTime = itemView.findViewById(R.id.tvSentTime);
        }

        void bind(Message message) {
            tvMessage.setText(message.getText());
            if (message.getCreatedAt() != null) {
                tvTime.setText(formatTime(message.getCreatedAt()));
            }
        }
    }

    class ReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvMessage, tvTime;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgReceivedAvatar);
            tvMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvTime = itemView.findViewById(R.id.tvReceivedTime);
        }

        void bind(Message message) {
            tvMessage.setText(message.getText());
            if (message.getCreatedAt() != null) {
                tvTime.setText(formatTime(message.getCreatedAt()));
            }

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
        }
    }

    private String formatTime(java.util.Date date) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
    }
}
