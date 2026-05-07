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
        holder.imgIcon.setImageResource(getIcon(item.getType()));
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    private int getIcon(String type) {
        switch (type != null ? type : "") {
            case "Like": return R.drawable.ic_like_color;
            case "Love": return R.drawable.ic_love;
            case "Haha": return R.drawable.ic_haha;
            case "Wow": return R.drawable.ic_wow;
            case "Sad": return R.drawable.ic_sad;
            case "Angry": return R.drawable.ic_angry;
            default: return R.drawable.ic_like_color;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgIcon;
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgUserAvatar);
            imgIcon = itemView.findViewById(R.id.imgReactionBadge);
            tvName = itemView.findViewById(R.id.tvUserName);
        }
    }
}