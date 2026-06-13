package com.example.frontend.ui.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Group;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DiscoverGroupAdapter extends RecyclerView.Adapter<DiscoverGroupAdapter.VH> {

    public interface OnJoinClickListener {
        void onJoin(Group group);
    }

    public interface OnGroupClickListener {
        void onOpen(Group group);
    }

    private final List<Group> items = new ArrayList<>();
    private final OnJoinClickListener listener;
    private OnGroupClickListener clickListener;

    public DiscoverGroupAdapter(OnJoinClickListener listener) {
        this.listener = listener;
    }

    public void setOnGroupClickListener(OnGroupClickListener l) {
        this.clickListener = l;
    }

    public void submit(List<Group> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void appendAll(List<Group> data) {
        if (data == null) return;
        int start = items.size();
        items.addAll(data);
        notifyItemRangeInserted(start, data.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discover_group, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Group g = items.get(position);
        h.tvName.setText(g.getGroupName());
        h.tvDescription.setText(
                g.getDescription() != null && !g.getDescription().isEmpty()
                        ? g.getDescription() : "Không có mô tả");
        h.tvMeta.setText(
                ("Public".equals(g.getPrivacy()) ? "🌐 Công khai" : "🔒 Riêng tư")
                        + "  •  " + g.getMemberCount() + " thành viên");

        if (g.getAvatarUrl() != null && !g.getAvatarUrl().isEmpty()) {
            Glide.with(h.imgAvatar.getContext()).load(g.getAvatarUrl())
                    .placeholder(R.drawable.ic_group).into(h.imgAvatar);
        } else {
            h.imgAvatar.setImageResource(R.drawable.ic_group);
        }

        h.btnJoin.setOnClickListener(v -> {
            if (listener != null) listener.onJoin(g);
        });

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onOpen(g);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView imgAvatar;
        TextView tvName, tvDescription, tvMeta;
        Button btnJoin;

        VH(@NonNull View v) {
            super(v);
            imgAvatar     = v.findViewById(R.id.imgGroupAvatar);
            tvName        = v.findViewById(R.id.tvGroupName);
            tvDescription = v.findViewById(R.id.tvGroupDescription);
            tvMeta        = v.findViewById(R.id.tvGroupMeta);
            btnJoin       = v.findViewById(R.id.btnJoin);
        }
    }
}