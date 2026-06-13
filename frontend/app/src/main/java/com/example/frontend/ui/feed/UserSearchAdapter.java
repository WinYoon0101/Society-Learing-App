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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.VH> {

    private List<User> data = new ArrayList<>();
    private Set<String> selectedIds = new HashSet<>();

    public UserSearchAdapter(List<User> initial, List<User> preselected) {
        if (initial != null) this.data = initial;
        if (preselected != null) {
            for (User u : preselected) selectedIds.add(u.getId());
        }
    }

    public void updateData(List<User> items) {
        this.data = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        User u = data.get(position);
        holder.tvName.setText(u.getUsername());
        Glide.with(holder.imgAvatar.getContext()).load(u.getAvatar()).placeholder(R.drawable.ic_user).into(holder.imgAvatar);
        holder.chk.setChecked(selectedIds.contains(u.getId()));
        holder.itemView.setOnClickListener(v -> {
            boolean now = !selectedIds.contains(u.getId());
            if (now) selectedIds.add(u.getId()); else selectedIds.remove(u.getId());
            holder.chk.setChecked(now);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    public List<User> getSelectedUsers() {
        List<User> res = new ArrayList<>();
        for (User u : data) {
            if (selectedIds.contains(u.getId())) res.add(u);
        }
        return res;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName;
        CheckBox chk;
        VH(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgUserAvatar);
            tvName = itemView.findViewById(R.id.tvUserName);
            chk = itemView.findViewById(R.id.chkSelectUser);
        }
    }
}
