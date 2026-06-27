package com.example.admin.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.admin.R;
import com.example.admin.data.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserCheckboxAdapter extends RecyclerView.Adapter<UserCheckboxAdapter.ViewHolder> {

    private List<User> userList = new ArrayList<>();
    private List<User> userListFull = new ArrayList<>(); // Bản sao để phục vụ tìm kiếm
    private Set<String> selectedUserIds = new HashSet<>(); // Lưu ID những người được tick

    public void setUsers(List<User> users) {
        this.userList.clear();
        this.userListFull.clear();
        if (users != null) {
            this.userList.addAll(users);
            this.userListFull.addAll(users);
        }
        notifyDataSetChanged();
    }

    // Trả về danh sách ID đang được chọn
    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
    }

    // Hàm lọc tìm kiếm
    public void filter(String text) {
        userList.clear();
        if (text.isEmpty()) {
            userList.addAll(userListFull);
        } else {
            text = text.toLowerCase().trim();
            for (User item : userListFull) {
                if ((item.getUsername() != null && item.getUsername().toLowerCase().contains(text)) ||
                        (item.getEmail() != null && item.getEmail().toLowerCase().contains(text))) {
                    userList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getUsername());
        holder.tvUserEmail.setText(user.getEmail());

        // Bỏ listener cũ để tránh lỗi khi scroll RecyclerView
        holder.cbSelectUser.setOnCheckedChangeListener(null);
        holder.cbSelectUser.setChecked(selectedUserIds.contains(user.getId()));

        holder.cbSelectUser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedUserIds.add(user.getId());
            } else {
                selectedUserIds.remove(user.getId());
            }
        });

        // Bấm vào cả dòng (Item) cũng giống như bấm vào CheckBox
        holder.itemView.setOnClickListener(v -> holder.cbSelectUser.setChecked(!holder.cbSelectUser.isChecked()));
    }

    @Override
    public int getItemCount() { return userList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelectUser;
        TextView tvUserName, tvUserEmail;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectUser = itemView.findViewById(R.id.cbSelectUser);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
        }
    }
}