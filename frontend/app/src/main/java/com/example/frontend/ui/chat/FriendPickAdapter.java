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
import com.example.frontend.data.model.Friend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendPickAdapter extends RecyclerView.Adapter<FriendPickAdapter.FriendPickViewHolder> {

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }

    private List<Friend> friends = new ArrayList<>();
    private final OnFriendClickListener listener;

    // Chế độ chọn nhiều (tạo nhóm) — mặc định tắt để giữ nguyên luồng single-select
    private boolean selectable = false;
    private final Set<String> selectedIds = new HashSet<>();

    public FriendPickAdapter(OnFriendClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Friend> list) {
        this.friends = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        notifyDataSetChanged();
    }

    /** Bật/tắt chọn 1 friend (chỉ có tác dụng ở chế độ selectable). */
    public void toggleSelection(String friendId) {
        if (friendId == null) return;
        if (selectedIds.contains(friendId)) {
            selectedIds.remove(friendId);
        } else {
            selectedIds.add(friendId);
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    @NonNull
    @Override
    public FriendPickViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_pick, parent, false);
        return new FriendPickViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendPickViewHolder holder, int position) {
        holder.bind(friends.get(position));
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    class FriendPickViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgAvatar;
        final TextView tvName;
        final ImageView imgCheck;

        FriendPickViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgFriendPickAvatar);
            tvName = itemView.findViewById(R.id.tvFriendPickName);
            imgCheck = itemView.findViewById(R.id.imgFriendPickCheck);
        }

        void bind(Friend friend) {
            tvName.setText(friend.getUsername());

            Glide.with(itemView.getContext())
                    .load(friend.getAvatar())
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(imgAvatar);

            if (selectable) {
                boolean checked = friend.getId() != null && selectedIds.contains(friend.getId());
                imgCheck.setVisibility(checked ? View.VISIBLE : View.GONE);
            } else {
                imgCheck.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (selectable) {
                    toggleSelection(friend.getId());
                }
                if (listener != null) {
                    listener.onFriendClick(friend);
                }
            });
        }
    }
}
