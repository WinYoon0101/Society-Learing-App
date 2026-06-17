package com.example.frontend.ui.feed;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.StoryGroup;
import com.example.frontend.ui.story.CreateStoryActivity;
import com.example.frontend.ui.story.StoryViewActivity;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CREATE = 0;
    private static final int TYPE_STORY  = 1;

    private final Context context;
    private final List<StoryGroup> items = new ArrayList<>();

    public StoryAdapter(Context context) {
        this.context = context;
    }

    public void submit(List<StoryGroup> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int pos) { return pos == 0 ? TYPE_CREATE : TYPE_STORY; }

    @Override public int getItemCount() { return items.size() + 1; } // +1 dành cho thẻ "Tạo tin" đầu tiên

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        if (viewType == TYPE_CREATE) {
            View v = inf.inflate(R.layout.item_feed_create_story, parent, false);
            return new CreateVH(v);
        }
        View v = inf.inflate(R.layout.item_home_story, parent, false);
        return new StoryVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        if (holder instanceof CreateVH) {
            // Lấy avatar người dùng
            SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            String myAvatar = prefs.getString("USER_AVATAR", "");

            CreateVH createVH = (CreateVH) holder;

            if (!myAvatar.isEmpty()) {
                Glide.with(context).load(myAvatar)
                        .placeholder(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(createVH.imgBg);
            } else {
                createVH.imgBg.setImageResource(R.drawable.ic_launcher_background);
            }

            // Xử lý sự kiện click mở màn hình Tạo tin mới
            holder.itemView.setOnClickListener(v ->
                    context.startActivity(new Intent(context, CreateStoryActivity.class)));
        } else {
            StoryGroup g = items.get(pos - 1);
            StoryVH h = (StoryVH) holder;
            h.tvName.setText(g.getAuthor() != null ? g.getAuthor().getUsername() : "");

            // Load Avatar của người đăng Story
            if (g.getAuthor() != null && g.getAuthor().getAvatar() != null) {
                Glide.with(context).load(g.getAuthor().getAvatar())
                        .placeholder(R.drawable.ic_user).into(h.imgAvatar);
            }

            // Load Background (ảnh của story mới nhất)
            if (g.getLatestMediaUrl() != null) {
                Glide.with(context).load(g.getLatestMediaUrl())
                        .centerCrop().into(h.imgBg);
            }

            // Xử lý sự kiện click mở màn hình Xem tin
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, StoryViewActivity.class);
                intent.putExtra("STORY_GROUP_AUTHOR_ID",
                        g.getAuthor() != null ? g.getAuthor().getId() : "");
                intent.putExtra("STORY_GROUP_AUTHOR_NAME",
                        g.getAuthor() != null ? g.getAuthor().getUsername() : "");

                // Truyền ID của story đầu tiên sang màn hình View
                if (g.getStories() != null && !g.getStories().isEmpty()) {
                    intent.putExtra("STORY_ID", g.getStories().get(0).getId());
                }
                context.startActivity(intent);
            });
        }
    }

    static class CreateVH extends RecyclerView.ViewHolder {
        ImageView imgBg;
        CreateVH(@NonNull View v) {
            super(v);
            imgBg = v.findViewById(R.id.imgMyAvatarBg);
        }
    }

    static class StoryVH extends RecyclerView.ViewHolder {
        ImageView imgBg;
        ShapeableImageView imgAvatar;
        TextView tvName;
        StoryVH(@NonNull View v) {
            super(v);
            imgBg     = v.findViewById(R.id.imgStoryBackground);
            imgAvatar = v.findViewById(R.id.imgStoryAvatar);
            tvName    = v.findViewById(R.id.tvStoryName);
        }
    }
}