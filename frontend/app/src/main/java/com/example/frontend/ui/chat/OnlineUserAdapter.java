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

public class OnlineUserAdapter extends RecyclerView.Adapter<OnlineUserAdapter.OnlineUserViewHolder> {

    private List<User> users = new ArrayList<>();
    private String currentUserId;
    private String currentUserAvatar;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private OnUserClickListener listener;

    public OnlineUserAdapter(String currentUserId, String currentUserAvatar, OnUserClickListener listener) {
        this.currentUserId = currentUserId;
        this.currentUserAvatar = currentUserAvatar;
        this.listener = listener;
    }

    public void submitList(List<User> list) {
        this.users = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OnlineUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_online_user, parent, false);
        return new OnlineUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnlineUserViewHolder holder, int position) {
        if (position == 0) {
            holder.bind(null, "Bạn", currentUserAvatar, true);
        } else {
            User user = users.get(position - 1);
            holder.bind(user, user.getUsername(), user.getAvatar(), true);
        }
    }

    @Override
    public int getItemCount() {
        // +1 for "You" slot at position 0
        return users.size() + 1;
    }

    class OnlineUserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName;
        View viewDot;

        OnlineUserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgOnlineAvatar);
            tvName = itemView.findViewById(R.id.tvOnlineName);
            viewDot = itemView.findViewById(R.id.viewOnlineDot);
        }

        void bind(User user, String name, String avatarUrl, boolean online) {
            tvName.setText(name);
            viewDot.setVisibility(online ? View.VISIBLE : View.GONE);

            Glide.with(itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(imgAvatar);

            itemView.setOnClickListener(v -> {
                if (listener != null && user != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}
