package com.example.frontend.ui.group;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupPostAdapter extends RecyclerView.Adapter<GroupPostAdapter.VH> {

    public interface OnReactionListener {
        void onReactClick(String targetId, String type);
    }

    private final List<Post> items = new ArrayList<>();
    private final Context context;
    private OnReactionListener reactionListener;

    public GroupPostAdapter(Context context) {
        this.context = context;
    }

    public void setOnReactionListener(OnReactionListener listener) {
        this.reactionListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<Post> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_group_post, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Post post = items.get(position);

        // Group header
        if (post.getGroupId() != null) {
            h.tvGroupName.setText(post.getGroupId().getGroupName());
            Glide.with(context)
                    .load(post.getGroupId().getAvatarUrl())
                    .placeholder(R.drawable.ic_group)
                    .error(R.drawable.ic_group)
                    .into(h.imgGroupAvatar);
        } else {
            h.layoutGroupHeader.setVisibility(View.GONE);
        }

        // Author
        if (post.getAuthorId() != null) {
            h.tvAuthorName.setText(post.getAuthorId().getUsername());
            Glide.with(context)
                    .load(post.getAuthorId().getAvatar())
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(h.imgAvatar);
        }

        h.tvContent.setText(post.getContent());

        // Images
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            h.rvPostImages.setVisibility(View.VISIBLE);
            PostImageAdapter imageAdapter = new PostImageAdapter(context, post.getImages());
            h.rvPostImages.setAdapter(imageAdapter);
        } else {
            h.rvPostImages.setVisibility(View.GONE);
        }

        // Comment count
        if (h.tvCommentCount != null) {
            h.tvCommentCount.setText(String.valueOf(post.getcountComment()));
        }

        // Reactions
        bindReactions(h, post);

        // Like button
        bindLikeButton(h, post);

        // Comment button → open PostDetail
        h.btnComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("POST_ID", post.getId());
            intent.putExtra("POST_CONTENT", post.getContent());
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
                intent.putStringArrayListExtra("TOP_REACTIONS", new ArrayList<>(post.getTopReactions()));
            }
            context.startActivity(intent);
        });
    }

    private void bindReactions(VH h, Post post) {
        int reactCount = post.getcountReaction();
        List<String> topReactions = post.getTopReactions();

        if (reactCount > 0) {
            h.layoutTopReactions.setVisibility(View.VISIBLE);
            h.tvReactionCount.setText(String.valueOf(reactCount));
            h.imgReact1.setVisibility(View.GONE);
            h.imgReact2.setVisibility(View.GONE);
            if (topReactions != null && !topReactions.isEmpty()) {
                h.imgReact1.setVisibility(View.VISIBLE);
                h.imgReact1.setImageResource(reactionIcon(topReactions.get(0)));
                if (topReactions.size() > 1) {
                    h.imgReact2.setVisibility(View.VISIBLE);
                    h.imgReact2.setImageResource(reactionIcon(topReactions.get(1)));
                }
            }
        } else {
            h.layoutTopReactions.setVisibility(View.GONE);
        }
    }

    private void bindLikeButton(VH h, Post post) {
        String current = post.getMyReaction();
        h.imgLike.setImageResource(reactionIcon(current));
        h.tvLikeLabel.setText(current != null ? current : "Thích");

        h.btnLikeContainer.setOnClickListener(v -> {
            String next = post.getMyReaction() != null ? null : "Like";
            applyReaction(h, post, next);
        });

        h.btnLikeContainer.setOnLongClickListener(v -> {
            View popupView = LayoutInflater.from(context).inflate(R.layout.item_feed_reaction_popup, null);
            PopupWindow pw = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, true);
            pw.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            int[] emojis = {R.id.btnReactLike, R.id.btnReactLove, R.id.btnReactHaha,
                    R.id.btnReactWow, R.id.btnReactSad, R.id.btnReactAngry};
            String[] types = {"Like", "Love", "Haha", "Wow", "Sad", "Angry"};
            for (int i = 0; i < emojis.length; i++) {
                final String type = types[i];
                popupView.findViewById(emojis[i]).setOnClickListener(ev -> {
                    applyReaction(h, post, type);
                    pw.dismiss();
                });
            }
            pw.showAsDropDown(v, 0, -v.getHeight() - 140);
            return true;
        });
    }

    private void applyReaction(VH h, Post post, String next) {
        String old = post.getMyReaction();
        int count = post.getcountReaction();
        List<String> tops = post.getTopReactions() != null ? new ArrayList<>(post.getTopReactions()) : new ArrayList<>();

        if (old == null && next != null) {
            count++;
            if (!tops.contains(next)) tops.add(0, next);
        } else if (old != null && next == null) {
            count = Math.max(0, count - 1);
            if (count == 0) tops.clear(); else tops.remove(old);
        } else if (old != null && !old.equals(next)) {
            tops.remove(old);
            if (!tops.contains(next)) tops.add(0, next);
        }
        if (tops.size() > 2) tops = new ArrayList<>(tops.subList(0, 2));

        post.setMyReaction(next);
        post.setcountReaction(count);
        post.setTopReactions(tops);

        h.imgLike.setImageResource(reactionIcon(next));
        h.tvLikeLabel.setText(next != null ? next : "Thích");
        bindReactions(h, post);

        if (reactionListener != null) {
            String send = next != null ? next : old;
            reactionListener.onReactClick(post.getId(), send);
        }
    }

    private int reactionIcon(String type) {
        if (type == null) return R.drawable.ic_like;
        switch (type) {
            case "Like":  return R.drawable.ic_like_color;
            case "Love":  return R.drawable.ic_love;
            case "Haha":  return R.drawable.ic_haha;
            case "Wow":   return R.drawable.ic_wow;
            case "Sad":   return R.drawable.ic_sad;
            case "Angry": return R.drawable.ic_angry;
            default:      return R.drawable.ic_like;
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View layoutGroupHeader;
        CircleImageView imgGroupAvatar;
        TextView tvGroupName;

        ImageView imgAvatar;
        TextView tvAuthorName, tvContent, tvCommentCount, tvLikeLabel;
        RecyclerView rvPostImages;
        LinearLayout layoutTopReactions, btnLikeContainer;
        ImageView imgReact1, imgReact2, imgLike;
        TextView tvReactionCount;
        View btnComment;

        VH(@NonNull View v) {
            super(v);
            layoutGroupHeader = v.findViewById(R.id.layoutGroupHeader);
            imgGroupAvatar = v.findViewById(R.id.imgGroupAvatar);
            tvGroupName = v.findViewById(R.id.tvGroupName);

            imgAvatar = v.findViewById(R.id.imgAvatar);
            tvAuthorName = v.findViewById(R.id.tvAuthorName);
            tvContent = v.findViewById(R.id.tvContent);
            tvCommentCount = v.findViewById(R.id.tvCommentCount);
            tvLikeLabel = v.findViewById(R.id.tvLikeCount);
            imgLike = v.findViewById(R.id.imgLike);

            rvPostImages = v.findViewById(R.id.rvPostImages);
            rvPostImages.setLayoutManager(new LinearLayoutManager(
                    v.getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvPostImages.setOnFlingListener(null);
            PagerSnapHelper snap = new PagerSnapHelper();
            snap.attachToRecyclerView(rvPostImages);

            layoutTopReactions = v.findViewById(R.id.layoutTopReactions);
            tvReactionCount = v.findViewById(R.id.tvReactionCount);
            imgReact1 = v.findViewById(R.id.imgReact1);
            imgReact2 = v.findViewById(R.id.imgReact2);

            btnLikeContainer = v.findViewById(R.id.btnLike);
            btnComment = v.findViewById(R.id.btnComment);
        }
    }
}
