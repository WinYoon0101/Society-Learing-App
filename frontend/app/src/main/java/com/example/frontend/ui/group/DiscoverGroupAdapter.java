package com.example.frontend.ui.group;

import android.annotation.SuppressLint;
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

    public interface OnJoinListener {
        void onJoin(Group group, int position);
    }

    private final List<Group> items = new ArrayList<>();
    private OnJoinListener joinListener;

    public void setOnJoinListener(OnJoinListener listener) {
        this.joinListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<Group> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
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
        h.tvGroupName.setText(g.getGroupName());
        h.tvMemberCount.setText(g.getMemberCount() + " thành viên");

        if (g.getDescription() != null && !g.getDescription().isEmpty()) {
            h.tvDescription.setVisibility(View.VISIBLE);
            h.tvDescription.setText(g.getDescription());
        } else {
            h.tvDescription.setVisibility(View.GONE);
        }

        if (g.getAvatarUrl() != null && !g.getAvatarUrl().isEmpty()) {
            Glide.with(h.imgAvatar.getContext())
                    .load(g.getAvatarUrl())
                    .placeholder(R.drawable.ic_group)
                    .error(R.drawable.ic_group)
                    .into(h.imgAvatar);
        } else {
            h.imgAvatar.setImageResource(R.drawable.ic_group);
        }

        h.btnJoin.setOnClickListener(v -> {
            if (joinListener != null) joinListener.onJoin(g, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView imgAvatar;
        TextView tvGroupName, tvMemberCount, tvDescription;
        Button btnJoin;

        VH(@NonNull View v) {
            super(v);
            imgAvatar = v.findViewById(R.id.imgGroupAvatar);
            tvGroupName = v.findViewById(R.id.tvGroupName);
            tvMemberCount = v.findViewById(R.id.tvMemberCount);
            tvDescription = v.findViewById(R.id.tvDescription);
            btnJoin = v.findViewById(R.id.btnJoin);
        }
    }
}
