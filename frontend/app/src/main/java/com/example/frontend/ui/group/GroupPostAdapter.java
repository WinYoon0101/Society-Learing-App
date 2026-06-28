package com.example.frontend.ui.group;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.GroupPost;
import com.example.frontend.ui.feed.HashtagTextHelper;
import com.example.frontend.ui.feed.PostDetailActivity;
import com.example.frontend.ui.feed.PostImageAdapter;
import com.example.frontend.ui.feed.ReactionListBottomSheet;
import com.example.frontend.ui.feed.ReactionUiHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class GroupPostAdapter extends RecyclerView.Adapter<GroupPostAdapter.VH> {

    public interface OnReactionListener {
        void onReactClick(String postId, String type);
    }

    private final List<GroupPost> items = new ArrayList<>();
    private OnReactionListener reactionListener;

    public void setOnReactionListener(OnReactionListener listener) {
        this.reactionListener = listener;
    }

    public void submit(List<GroupPost> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void appendAll(List<GroupPost> data) {
        if (data == null) return;
        int start = items.size();
        items.addAll(data);
        notifyItemRangeInserted(start, data.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_post, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        GroupPost p = items.get(position);

        // Avatar & tên tác giả
        if (p.getAuthorId() != null) {
            h.tvAuthor.setText(p.getAuthorId().getUsername());
            if (p.getAuthorId().getAvatar() != null && !p.getAuthorId().getAvatar().isEmpty()) {
                Glide.with(h.imgAvatar.getContext())
                        .load(p.getAuthorId().getAvatar())
                        .placeholder(R.drawable.ic_user)
                        .into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_user);
            }
        }

        // Tên nhóm
        if (p.getGroupId() != null && p.getGroupId().getGroupName() != null) {
            h.tvGroupName.setText(p.getGroupId().getGroupName());
            h.tvGroupName.setVisibility(View.VISIBLE);
        } else {
            h.tvGroupName.setVisibility(View.GONE);
        }

        // Thời gian
        h.tvTime.setText(formatTime(p.getCreatedAt()));

        // Nội dung
        h.tvContent.setText(HashtagTextHelper.highlight(p.getContent()));
        h.tvContent.setVisibility(
                p.getContent() != null && !p.getContent().isEmpty() ? View.VISIBLE : View.GONE);

        // Ảnh
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            h.rvPostImages.setVisibility(View.VISIBLE);
            PostImageAdapter imageAdapter = new PostImageAdapter(h.rvPostImages.getContext(), p.getImages());
            h.rvPostImages.setAdapter(imageAdapter);
        } else {
            h.rvPostImages.setVisibility(View.GONE);
        }

        // --- Top reactions ---
        int reactCount = p.getCountReaction();
        List<String> topReactions = p.getTopReactions();

        ReactionUiHelper.bindTopReactions(
                h.layoutTopReactions,
                h.imgReact1,
                h.imgReact2,
                h.tvReactionCount,
                reactCount,
                topReactions
        );

        h.layoutTopReactions.setOnClickListener(v -> {
            Context ctx = v.getContext();
            if (ctx instanceof AppCompatActivity) {
                ReactionListBottomSheet sheet = ReactionListBottomSheet.newInstance(p.getId());
                sheet.show(((AppCompatActivity) ctx).getSupportFragmentManager(), "ReactionSheet");
            }
        });

        // --- Reaction button ---
        String currentReaction = p.getMyReaction();
        ReactionUiHelper.bindReactionButton(h.imgLike, h.tvLikeLabel, currentReaction);

        h.btnLike.setOnClickListener(v -> {
            String newType = (p.getMyReaction() != null) ? null : "Like";
            handleReactionUpdate(h, p, newType);
        });

        h.btnLike.setOnLongClickListener(v -> {
            View popupView = LayoutInflater.from(v.getContext())
                    .inflate(R.layout.item_feed_reaction_popup, null);
            PopupWindow pw = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            pw.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));

            View btnReactLike = popupView.findViewById(R.id.btnReactLike);
            View btnReactLove = popupView.findViewById(R.id.btnReactLove);
            View btnReactHaha = popupView.findViewById(R.id.btnReactHaha);
            View btnReactWow = popupView.findViewById(R.id.btnReactWow);
            View btnReactSad = popupView.findViewById(R.id.btnReactSad);
            View btnReactAngry = popupView.findViewById(R.id.btnReactAngry);

            btnReactLike.setOnClickListener(x -> { handleReactionUpdate(h, p, "Like"); pw.dismiss(); });
            btnReactLove.setOnClickListener(x -> { handleReactionUpdate(h, p, "Love"); pw.dismiss(); });
            btnReactHaha.setOnClickListener(x -> { handleReactionUpdate(h, p, "Haha"); pw.dismiss(); });
            btnReactWow.setOnClickListener(x ->  { handleReactionUpdate(h, p, "Wow");  pw.dismiss(); });
            btnReactSad.setOnClickListener(x ->  { handleReactionUpdate(h, p, "Sad");  pw.dismiss(); });
            btnReactAngry.setOnClickListener(x ->{ handleReactionUpdate(h, p, "Angry");pw.dismiss(); });

            pw.showAsDropDown(v, 0, -v.getHeight() - 140);
            return true;
        });

        // --- Comment button ---
        h.btnComment.setOnClickListener(v -> {
            Context ctx = v.getContext();
            Intent intent = new Intent(ctx, PostDetailActivity.class);
            intent.putExtra("POST_ID", p.getId());
            intent.putExtra("POST_CONTENT", p.getContent());
            intent.putExtra(PostDetailActivity.EXTRA_POST_FEELING, p.getFeeling());
            if (p.getAuthorId() != null) {
                intent.putExtra("AUTHOR_NAME", p.getAuthorId().getUsername());
                intent.putExtra("AUTHOR_AVATAR", p.getAuthorId().getAvatar());
            }
            if (p.getImages() != null) {
                intent.putStringArrayListExtra("POST_IMAGES", new ArrayList<>(p.getImages()));
            }
            intent.putExtra("COMMENT_COUNT", p.getCountComment());
            intent.putExtra("REACTION_COUNT", p.getCountReaction());
            intent.putExtra("MY_REACTION", p.getMyReaction());
            if (p.getTopReactions() != null) {
                intent.putStringArrayListExtra("TOP_REACTIONS", new ArrayList<>(p.getTopReactions()));
            }
            ctx.startActivity(intent);
        });

        h.tvCommentCount.setText(String.valueOf(p.getCountComment()));
    }

    private void handleReactionUpdate(VH h, GroupPost p, String newType) {
        String oldType = p.getMyReaction();
        int count = p.getCountReaction();
        List<String> top = p.getTopReactions();
        if (top == null) top = new ArrayList<>();

        if (oldType == null && newType != null) {
            count++;
            if (!top.contains(newType)) top.add(0, newType);
        } else if (oldType != null && newType == null) {
            count--;
            if (count <= 0) top.clear();
            else top.remove(oldType);
        } else if (oldType != null && !oldType.equals(newType)) {
            top.remove(oldType);
            if (!top.contains(newType)) top.add(0, newType);
        }
        if (top.size() > 2) top = new ArrayList<>(top.subList(0, 2));

        p.setMyReaction(newType);
        p.setCountReaction(count);
        p.setTopReactions(top);

        ReactionUiHelper.bindReactionButton(h.imgLike, h.tvLikeLabel, newType);

        ReactionUiHelper.bindTopReactions(
                h.layoutTopReactions,
                h.imgReact1,
                h.imgReact2,
                h.tvReactionCount,
                count,
                top
        );

        if (reactionListener != null) {
            String toSend = newType != null ? newType : oldType;
            reactionListener.onReactClick(p.getId(), toSend);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

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
        de.hdodenhof.circleimageview.CircleImageView imgAvatar;
        TextView tvAuthor, tvGroupName, tvTime, tvContent, tvReactionCount, tvCommentCount, tvLikeLabel;
        RecyclerView rvPostImages;
        LinearLayout layoutTopReactions, btnLike, btnComment;
        ImageView imgReact1, imgReact2, imgLike;

        VH(@NonNull View v) {
            super(v);
            imgAvatar         = v.findViewById(R.id.imgAvatar);
            tvAuthor          = v.findViewById(R.id.tvAuthorName);
            tvGroupName       = v.findViewById(R.id.tvGroupName);
            tvTime            = v.findViewById(R.id.tvTime);
            tvContent         = v.findViewById(R.id.tvContent);
            rvPostImages      = v.findViewById(R.id.rvPostImages);
            rvPostImages.setLayoutManager(new LinearLayoutManager(v.getContext(),
                    LinearLayoutManager.HORIZONTAL, false));
            rvPostImages.setOnFlingListener(null);
            new PagerSnapHelper().attachToRecyclerView(rvPostImages);

            layoutTopReactions = v.findViewById(R.id.layoutTopReactions);
            tvReactionCount    = v.findViewById(R.id.tvReactionCount);
            imgReact1          = v.findViewById(R.id.imgReact1);
            imgReact2          = v.findViewById(R.id.imgReact2);

            btnLike    = v.findViewById(R.id.btnLike);
            imgLike    = v.findViewById(R.id.imgLike);
            tvLikeLabel= v.findViewById(R.id.tvLikeCount);
            btnComment = v.findViewById(R.id.btnComment);
            tvCommentCount = v.findViewById(R.id.tvCommentCount);
        }
    }
}
