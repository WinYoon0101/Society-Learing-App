package com.example.frontend.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Post;
import com.example.frontend.ui.feed.PostDetailActivity;
import com.example.frontend.ui.feed.PostImageAdapter;
import com.example.frontend.ui.feed.ReactionUiHelper;

import java.util.ArrayList;
import java.util.List;

public class ProfilePostAdapter extends RecyclerView.Adapter<ProfilePostAdapter.ViewHolder> {

    private final Context context;
    private List<Post> posts;

    public ProfilePostAdapter(Context context, List<Post> posts) {
        this.context = context;
        this.posts = posts != null ? posts : new ArrayList<>();
    }

    public void updateData(List<Post> newPosts) {
        this.posts = newPosts != null ? newPosts : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_home_posts, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Post post = posts.get(pos);

        // ── Nội dung bài viết ──
        h.tvContent.setText(post.getContent() != null ? post.getContent() : "");

        // ── Tác giả ──
        if (post.getAuthorId() != null) {
            h.tvUserName.setText(post.getAuthorId().getUsername() != null
                    ? post.getAuthorId().getUsername() : "Người dùng");
            Glide.with(context)
                    .load(post.getAuthorId().getAvatar())
                    .placeholder(R.drawable.ic_user)
                    .into(h.imgAvatar);
        } else {
            h.tvUserName.setText("Người dùng");
            h.imgAvatar.setImageResource(R.drawable.ic_user);
        }

        // ── Ảnh bài viết ──
        if (h.rvPostImages != null) {
            boolean hasImages = post.getImages() != null && !post.getImages().isEmpty();
            h.rvPostImages.setVisibility(hasImages ? View.VISIBLE : View.GONE);
            if (hasImages) {
                h.rvPostImages.setAdapter(new PostImageAdapter(context, post.getImages()));
            }
        }

        // ── Số react ──
        int rc = post.getcountReaction();
        List<String> topReactions = post.getTopReactions();

        ReactionUiHelper.bindTopReactions(
                h.layoutTopReactions,
                h.imgReact1,
                h.imgReact2,
                h.tvReactionCount,
                rc,
                topReactions
        );

        // ── Nhãn nút Like ──
        ReactionUiHelper.bindReactionButton(h.imgLikeIcon, h.tvLikeLabel, post.getMyReaction());

        // ── Số comment ──
        if (h.tvCommentCount != null) {
            int cc = post.getcountComment();
            h.tvCommentCount.setText(cc > 0 ? String.valueOf(cc) : "");
            h.tvCommentCount.setVisibility(cc > 0 ? View.VISIBLE : View.GONE);
        }

        // ── Click bài viết → mở PostDetailActivity ──
        h.itemView.setOnClickListener(v -> {
            if (post.getId() == null) return;
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("POST_ID", post.getId());
            intent.putExtra("POST_CONTENT", post.getContent());
            intent.putExtra(PostDetailActivity.EXTRA_POST_FEELING, post.getFeeling());
            if (post.getAuthorId() != null) {
                intent.putExtra("AUTHOR_NAME", post.getAuthorId().getUsername());
                intent.putExtra("AUTHOR_AVATAR", post.getAuthorId().getAvatar());
            }
            if (post.getImages() != null) {
                intent.putStringArrayListExtra("POST_IMAGES", new ArrayList<>(post.getImages()));
            }
            intent.putExtra("COMMENT_COUNT", post.getcountComment());
            intent.putExtra("REACTION_COUNT", post.getcountReaction());
            intent.putExtra("MY_REACTION", post.getMyReaction());
            if (post.getTopReactions() != null) {
                intent.putStringArrayListExtra("TOP_REACTIONS",
                        new ArrayList<>(post.getTopReactions()));
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return posts.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvContent, tvCommentCount, tvReactionCount, tvLikeLabel;
        ImageView imgAvatar, imgLikeIcon;
        ImageView imgReact1, imgReact2;
        RecyclerView rvPostImages;
        android.widget.LinearLayout layoutTopReactions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName      = itemView.findViewById(R.id.tvAuthorName);
            tvContent       = itemView.findViewById(R.id.tvContent);
            imgAvatar       = itemView.findViewById(R.id.imgAvatar);
            rvPostImages    = itemView.findViewById(R.id.rvPostImages);
            tvCommentCount  = itemView.findViewById(R.id.tvCommentCount);
            tvReactionCount = itemView.findViewById(R.id.tvReactionCount);
            tvLikeLabel     = itemView.findViewById(R.id.tvLikeCount);
            imgLikeIcon     = itemView.findViewById(R.id.imgLike);
            imgReact1       = itemView.findViewById(R.id.imgReact1);
            imgReact2       = itemView.findViewById(R.id.imgReact2);
            layoutTopReactions = itemView.findViewById(R.id.layoutTopReactions);

            if (rvPostImages != null) {
                rvPostImages.setLayoutManager(new LinearLayoutManager(
                        itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                rvPostImages.setOnFlingListener(null);
                new PagerSnapHelper().attachToRecyclerView(rvPostImages);
            }
        }
    }
}
