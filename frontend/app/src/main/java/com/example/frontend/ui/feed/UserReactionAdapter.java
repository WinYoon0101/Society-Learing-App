package com.example.frontend.ui.feed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ReactionItem;
import java.util.List;

public class UserReactionAdapter extends RecyclerView.Adapter<UserReactionAdapter.ViewHolder> {
    private Context context;
    private List<ReactionItem> list;

    public UserReactionAdapter(Context context, List<ReactionItem> list) {
        this.context = context;
        this.list = list;
    }

    public void updateData(List<ReactionItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feed_user_reaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReactionItem item = list.get(position);
        if (item.getUserId() != null) {
            holder.tvName.setText(item.getUserId().getUsername());
            Glide.with(context).load(item.getUserId().getAvatar()).placeholder(R.drawable.ic_user).into(holder.imgAvatar);
        }
        holder.imgIcon.setText(getEmoji(item.getType()));
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    private String getEmoji(String type) {
        switch (type != null ? type : "") {
            case "Like": return "👍";
            case "Love": return "❤️";
            case "Haha": return "😆";
            case "Wow": return "😮";
            case "Sad": return "😢";
            case "Angry": return "😡";
            default: return "👍";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView imgIcon;
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgUserAvatar);
            imgIcon = itemView.findViewById(R.id.imgReactionBadge);
            tvName = itemView.findViewById(R.id.tvUserName);
        }
    }
}