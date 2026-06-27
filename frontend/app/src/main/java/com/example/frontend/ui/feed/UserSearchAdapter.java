package com.example.frontend.ui.feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.VH> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    private List<User> data = new ArrayList<>();
    private final Map<String, User> selectedUsers = new LinkedHashMap<>();
    private OnSelectionChangedListener selectionListener;

    public UserSearchAdapter(List<User> initial, List<User> preselected) {
        if (initial != null) this.data = initial;
        if (preselected != null) {
            for (User u : preselected) {
                if (u != null && u.getId() != null) {
                    selectedUsers.put(u.getId(), u);
                }
            }
        }
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public void updateData(List<User> items) {
        this.data = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        User u = data.get(position);
        if (u == null) return;

        holder.tvName.setText(u.getUsername() != null ? u.getUsername() : "Người dùng");

        // Cải thiện Glide: Thêm error và circleCrop để ảnh đẹp hơn
        Glide.with(holder.itemView.getContext())
                .load(u.getAvatar())
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(holder.imgAvatar);

        String uid = u.getId();

        // Gỡ listener của Checkbox trước khi set (tránh lỗi khi tái sử dụng View)
        holder.chk.setOnCheckedChangeListener(null);
        holder.chk.setChecked(uid != null && selectedUsers.containsKey(uid));

        // Gom chung sự kiện Click cho cả Hàng (itemView) và Checkbox
        View.OnClickListener clickAction = v -> {
            if (uid == null) return;
            if (selectedUsers.containsKey(uid)) {
                selectedUsers.remove(uid);
                holder.chk.setChecked(false);
            } else {
                selectedUsers.put(uid, u);
                holder.chk.setChecked(true);
            }
            if (selectionListener != null) {
                selectionListener.onSelectionChanged(selectedUsers.size());
            }
        };

        // Gắn sự kiện click
        holder.itemView.setOnClickListener(clickAction);
        holder.chk.setOnClickListener(clickAction); // 👉 FIX LỖI 2: Đảm bảo bấm Checkbox cũng chạy logic
    }

    @Override
    public int getItemCount() { return data != null ? data.size() : 0; }

    public List<User> getSelectedUsers() {
        return new ArrayList<>(selectedUsers.values());
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName;
        CheckBox chk;

        VH(@NonNull View itemView) {
            super(itemView);
            // 👉 FIX LỖI 1: Cập nhật lại chuẩn ID theo file XML
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvUserName);
            chk = itemView.findViewById(R.id.cbSelect);
        }
    }
}