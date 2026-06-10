package com.example.frontend.ui.group;

import android.text.format.DateUtils;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import de.hdodenhof.circleimageview.CircleImageView;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.VH> {

    public interface OnRespondListener {
        void onAccept(GroupInvitation invitation);
        void onDecline(GroupInvitation invitation);
    }

    private final List<GroupInvitation> items = new ArrayList<>();
    private final OnRespondListener listener;

    public InvitationAdapter(OnRespondListener listener) {
        this.listener = listener;
    }

    public void submit(List<GroupInvitation> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_invitation, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        GroupInvitation inv = items.get(position);

        // Tên nhóm
        if (inv.getGroup() != null) {
            h.tvGroupName.setText(inv.getGroup().getGroupName());
            if (inv.getGroup().getAvatarUrl() != null && !inv.getGroup().getAvatarUrl().isEmpty()) {
                Glide.with(h.imgGroupAvatar.getContext())
                        .load(inv.getGroup().getAvatarUrl())
                        .placeholder(R.drawable.ic_group)
                        .into(h.imgGroupAvatar);
            } else {
                h.imgGroupAvatar.setImageResource(R.drawable.ic_group);
            }
        }

        // Người mời
        if (inv.getInviter() != null) {
            h.tvInviter.setText("Được mời bởi " + inv.getInviter().getUsername());
        }

        // Thời gian
        h.tvTime.setText(formatTime(inv.getCreatedAt()));

        h.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(inv);
        });

        h.btnDecline.setOnClickListener(v -> {
            if (listener != null) listener.onDecline(inv);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            long ts = fmt.parse(iso).getTime();
            return DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        } catch (Exception e) {
            return "";
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView imgGroupAvatar;
        TextView tvGroupName, tvInviter, tvTime;
        Button btnAccept, btnDecline;

        VH(@NonNull View v) {
            super(v);
            imgGroupAvatar = v.findViewById(R.id.imgGroupAvatar);
            tvGroupName    = v.findViewById(R.id.tvGroupName);
            tvInviter      = v.findViewById(R.id.tvInviter);
            tvTime         = v.findViewById(R.id.tvTime);
            btnAccept      = v.findViewById(R.id.btnAccept);
            btnDecline     = v.findViewById(R.id.btnDecline);
        }
    }
}