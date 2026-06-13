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
import com.example.frontend.data.model.GroupInvitation;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.VH> {

    public interface OnRespondListener {
        void onAccept(GroupInvitation invitation, int position);
        void onDecline(GroupInvitation invitation, int position);
    }

    private final List<GroupInvitation> items = new ArrayList<>();
    private OnRespondListener respondListener;

    public void setOnRespondListener(OnRespondListener listener) {
        this.respondListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<GroupInvitation> data) {
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
                .inflate(R.layout.item_invitation, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        GroupInvitation inv = items.get(position);

        if (inv.getGroup() != null) {
            h.tvGroupName.setText(inv.getGroup().getGroupName());
            Glide.with(h.imgGroupAvatar.getContext())
                    .load(inv.getGroup().getAvatarUrl())
                    .placeholder(R.drawable.ic_group)
                    .error(R.drawable.ic_group)
                    .into(h.imgGroupAvatar);
        }

        if (inv.getInviter() != null) {
            h.tvInviterName.setText(inv.getInviter().getUsername() + " đã mời bạn tham gia");
        }

        h.btnAccept.setOnClickListener(v -> {
            if (respondListener != null) respondListener.onAccept(inv, h.getAdapterPosition());
        });
        h.btnDecline.setOnClickListener(v -> {
            if (respondListener != null) respondListener.onDecline(inv, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView imgGroupAvatar;
        TextView tvGroupName, tvInviterName;
        Button btnAccept, btnDecline;

        VH(@NonNull View v) {
            super(v);
            imgGroupAvatar = v.findViewById(R.id.imgGroupAvatar);
            tvGroupName = v.findViewById(R.id.tvGroupName);
            tvInviterName = v.findViewById(R.id.tvInviterName);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnDecline = v.findViewById(R.id.btnDecline);
        }
    }
}
