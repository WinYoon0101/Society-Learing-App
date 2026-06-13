package com.example.frontend.ui.live;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.LiveModel;
import java.util.List;

public class LiveAdapter extends RecyclerView.Adapter<LiveAdapter.ViewHolder> {
    private List<LiveModel> lives;
    private OnLiveClickListener listener;

    public interface OnLiveClickListener {
        void onLiveClick(LiveModel live);
    }

    public LiveAdapter(List<LiveModel> lives, OnLiveClickListener listener) {
        this.lives = lives;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_live, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LiveModel live = lives.get(position);

        // Hiển thị tên và tiêu đề
        holder.tvHostName.setText(live.getHost().getUsername());
        holder.tvLiveTitle.setText(live.getTitle());



        // Load Avatar Host bằng Glide
        if (live.getHost().getAvatar() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(live.getHost().getAvatar())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(holder.imgAvatar);
        }

        holder.itemView.setOnClickListener(v -> listener.onLiveClick(live));
    }

    @Override
    public int getItemCount() { return lives != null ? lives.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHostName, tvLiveTitle, tvViewerCount;
        ImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHostName = itemView.findViewById(R.id.tvHostName);
            tvLiveTitle = itemView.findViewById(R.id.tvLiveTitle);

            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}