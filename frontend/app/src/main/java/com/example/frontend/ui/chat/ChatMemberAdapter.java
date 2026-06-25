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
import com.example.frontend.data.model.User;

import java.util.ArrayList;
import java.util.List;

/** List thành viên group: tên (ưu tiên biệt danh) + tag "Bạn"; click → callback. */
public class ChatMemberAdapter extends RecyclerView.Adapter<ChatMemberAdapter.MemberViewHolder> {

    public interface MemberCallback {
        String displayName(User member);
        void onMemberClick(User member);
    }

    private final List<User> members;
    private final String currentUserId;
    private final MemberCallback callback;

    public ChatMemberAdapter(List<User> members, String currentUserId, MemberCallback callback) {
        this.members = members != null ? members : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.callback = callback;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(members.get(position));
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgAvatar;
        final TextView tvName;
        final TextView tvTag;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgChatMemberAvatar);
            tvName = itemView.findViewById(R.id.tvChatMemberName);
            tvTag = itemView.findViewById(R.id.tvChatMemberTag);
        }

        void bind(User member) {
            tvName.setText(callback != null ? callback.displayName(member) : member.getUsername());

            boolean isMe = member.getId() != null && member.getId().equals(currentUserId);
            tvTag.setVisibility(isMe ? View.VISIBLE : View.GONE);
            tvTag.setText("Bạn");

            Glide.with(itemView.getContext())
                    .load(member.getAvatar())
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(imgAvatar);

            itemView.setOnClickListener(v -> {
                if (callback != null) callback.onMemberClick(member);
            });
        }
    }
}
