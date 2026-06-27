package com.example.admin.ui.users;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // IMPORT GLIDE VÀO ĐÂY
import com.example.admin.R;
import com.example.admin.data.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.UserViewHolder> {

    private List<User> userList = new ArrayList<>();
    private List<User> userListFull = new ArrayList<>();
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user, int position);
    }

    public void setListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<User> list) {
        this.userList = new ArrayList<>(list);
        this.userListFull = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    public void updateUserAt(int position, User updatedUser) {
        userList.set(position, updatedUser);
        for (int i = 0; i < userListFull.size(); i++) {
            if (userListFull.get(i)._id.equals(updatedUser._id)) {
                userListFull.set(i, updatedUser);
                break;
            }
        }
        notifyItemChanged(position);
    }

    public void filter(String text) {
        userList.clear();
        if (text.isEmpty()) {
            userList.addAll(userListFull);
        } else {
            text = text.toLowerCase();
            for (User user : userListFull) {
                if ((user.username != null && user.username.toLowerCase().contains(text)) ||
                        (user.email != null && user.email.toLowerCase().contains(text))) {
                    userList.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_admin, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvUsername.setText(user.username != null ? user.username : "Không xác định");
        holder.tvEmail.setText(user.email != null ? user.email : "Không có email");

        if (user.isActive) {
            holder.imgStatus.setColorFilter(Color.parseColor("#10B981"));
        } else {
            holder.imgStatus.setColorFilter(Color.parseColor("#EF4444"));
        }

        // SỬ DỤNG GLIDE ĐỂ LOAD AVATAR THẬT TỪ URL
        if (user.avatar != null && !user.avatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_user) // Ảnh tạm khi đang load
                    .error(R.drawable.ic_user)       // Ảnh lỗi nếu URL hỏng
                    .circleCrop()                    // Bo tròn ảnh
                    .into(holder.imgAvatar);
        } else {
            // Nếu không có avatar thì set ảnh mặc định
            holder.imgAvatar.setImageResource(R.drawable.ic_user);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgStatus;
        TextView tvUsername, tvEmail;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgStatus = itemView.findViewById(R.id.imgStatus);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
        }
    }
}