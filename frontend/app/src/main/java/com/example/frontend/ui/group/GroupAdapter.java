package com.example.frontend.ui.group;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Group;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.VH> {

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    private final List<Group> items = new ArrayList<>();
    private final OnGroupClickListener listener;

    public GroupAdapter(OnGroupClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Group> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Group g = items.get(position);
        h.tvName.setText(g.getGroupName());

        // Subtitle: priority -> "X bài viết mới" if newPostCount > 0, else "Cập nhật ..."
        if (g.getNewPostCount() > 0) {
            String txt = g.getNewPostCount() > 10
                    ? "• Hơn 10 bài viết mới"
                    : "• " + g.getNewPostCount() + " bài viết mới";
            h.tvSubtitle.setText(txt);
            h.tvSubtitle.setTextColor(0xFF1F8B4C); // green
        } else {
            h.tvSubtitle.setText(formatLastUpdated(g.getLastUpdated()));
            h.tvSubtitle.setTextColor(0xFF888888); // gray
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

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(g);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatLastUpdated(String iso) {
        if (iso == null || iso.isEmpty()) return "Chưa có hoạt động";
        try {
            SimpleDateFormat fmt = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            long ts = fmt.parse(iso).getTime();
            CharSequence rel = DateUtils.getRelativeTimeSpanString(
                    ts, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            return "Cập nhật " + rel;
        } catch (Exception e) {
            return "Chưa có hoạt động";
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView imgAvatar;
        TextView tvName, tvSubtitle;

        VH(@NonNull View v) {
            super(v);
            imgAvatar = v.findViewById(R.id.imgGroupAvatar);
            tvName = v.findViewById(R.id.tvGroupName);
            tvSubtitle = v.findViewById(R.id.tvGroupSubtitle);
        }
    }
}
