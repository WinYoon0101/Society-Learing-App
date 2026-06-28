package com.example.frontend.ui.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.User;
import com.example.frontend.ui.profile.ProfileNavigationHelper;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnReactionListener {
        void onReactClick(String targetId, String type);
    }

    public interface OnPostDeleteListener {
        void onDeletePost(String postId);
    }

    public interface OnPostSaveListener {
        void onSavePost(String postId);
    }

    private List<Post> postList;
    private Context context;
    private OnReactionListener reactionListener;
    private OnPostDeleteListener deleteListener;
    private OnPostSaveListener saveListener;

    public PostAdapter(Context context, List<Post> postList, OnReactionListener listener) {
        this.context = context;
        this.postList = postList;
        this.reactionListener = listener;
    }

    public void setOnPostDeleteListener(OnPostDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnPostSaveListener(OnPostSaveListener listener) {
        this.saveListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Post> newPostList) {
        this.postList = newPostList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_posts, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvContent.setText(HashtagTextHelper.highlight(post.getContent()));

        if (post.getAuthorId() != null) {
            String authorName = post.getAuthorId().getUsername();
            if (authorName == null || authorName.trim().isEmpty()) {
                authorName = "Người dùng";
            }
            final String finalAuthorName = authorName;
            List<User> tags = post.getTags();

            View.OnClickListener goToAuthorProfile = v -> {
                if (post.getAuthorId().getId() != null) {
                    ProfileNavigationHelper.openProfile(
                            context,
                            post.getAuthorId().getId(),
                            finalAuthorName,
                            post.getAuthorId().getAvatar()
                    );
                }
            };

            holder.imgAvatar.setOnClickListener(goToAuthorProfile);

            if (tags != null && !tags.isEmpty() && tags.get(0) != null) {
                String taggedName = tags.get(0).getUsername();
                if (taggedName == null || taggedName.trim().isEmpty()) {
                    taggedName = "Người dùng";
                }
                final String finalTaggedName = taggedName;
                String prefix = " — cùng với ";
                String suffix = "";
                if (tags.size() > 1) {
                    suffix = " và " + (tags.size() - 1) + " người khác";
                }

                String feelingText = "";

                if (post.getFeeling() != null && !post.getFeeling().trim().isEmpty()) {
                    feelingText = " đang cảm thấy " + getFeelingTextInVietnamese(post.getFeeling());
                }

                String fullText = finalAuthorName + prefix + finalTaggedName + suffix + feelingText;
                SpannableString spannableString = new SpannableString(fullText);

                ClickableSpan authorSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        goToAuthorProfile.onClick(widget);
                    }
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(Color.parseColor("#050505"));
                        ds.setFakeBoldText(true);
                    }
                };
                spannableString.setSpan(authorSpan, 0, finalAuthorName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                ClickableSpan taggedSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (tags.get(0).getId() != null) {
                            ProfileNavigationHelper.openProfile(
                                    context,
                                    tags.get(0).getId(),
                                    finalTaggedName,
                                    tags.get(0).getAvatar()
                            );
                        }
                    }
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(Color.parseColor("#050505"));
                        ds.setFakeBoldText(true);
                    }
                };

                int startTag = finalAuthorName.length() + prefix.length();
                int endTag = startTag + finalTaggedName.length();
                spannableString.setSpan(taggedSpan, startTag, endTag, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (!suffix.isEmpty()) {
                    int startMore = endTag;
                    int endMore = startMore + suffix.length();
                    ClickableSpan moreTaggedSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            showTaggedUsers(tags);
                        }

                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false);
                            ds.setColor(Color.parseColor("#050505"));
                            ds.setFakeBoldText(true);
                        }
                    };
                    spannableString.setSpan(moreTaggedSpan, startMore, endMore, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                holder.tvUserName.setText(spannableString);
                holder.tvUserName.setMovementMethod(LinkMovementMethod.getInstance());
                holder.tvUserName.setHighlightColor(Color.TRANSPARENT);
                holder.tvUserName.setOnClickListener(null);

            } else {
                String displayName = finalAuthorName;

                if (post.getFeeling() != null && !post.getFeeling().trim().isEmpty()) {
                    displayName += " đang cảm thấy " + getFeelingTextInVietnamese(post.getFeeling());
                }
                SpannableString spannableString = new SpannableString(displayName);
                ClickableSpan authorSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        goToAuthorProfile.onClick(widget);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(Color.parseColor("#050505"));
                        ds.setFakeBoldText(true);
                    }
                };
                spannableString.setSpan(authorSpan, 0, finalAuthorName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.tvUserName.setText(spannableString);
                holder.tvUserName.setMovementMethod(LinkMovementMethod.getInstance());
                holder.tvUserName.setHighlightColor(Color.TRANSPARENT);
                holder.tvUserName.setOnClickListener(null);
            }

            Glide.with(context).load(post.getAuthorId().getAvatar()).placeholder(R.drawable.ic_user).into(holder.imgAvatar);

        } else {
            holder.tvUserName.setText("Người dùng ẩn danh");
            holder.imgAvatar.setOnClickListener(null);
            holder.tvUserName.setOnClickListener(null);
            holder.tvUserName.setMovementMethod(null);
        }

        if (post.getCreatedAt() != null) {
            holder.tvTime.setText(formatTime(post.getCreatedAt()));
        } else {
            holder.tvTime.setText("Vừa xong");
        }

        if (holder.imgPrivacy != null) {
            String privacy = post.getPrivacy();
            if (privacy != null) {
                holder.imgPrivacy.setVisibility(View.VISIBLE);
                if (privacy.equalsIgnoreCase("Private")) {
                    holder.imgPrivacy.setImageResource(R.drawable.ic_private);
                } else if (privacy.equalsIgnoreCase("Friends")) {
                    holder.imgPrivacy.setImageResource(R.drawable.ic_friend);
                } else {
                    holder.imgPrivacy.setImageResource(R.drawable.ic_public);
                }
            } else {
                holder.imgPrivacy.setVisibility(View.VISIBLE);
                holder.imgPrivacy.setImageResource(R.drawable.ic_public);
            }
        }

        if (holder.btnMoreOptions != null) {
            holder.btnMoreOptions.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, holder.btnMoreOptions);
                popupMenu.getMenu().add(Menu.NONE, 1, 1, "Lưu bài viết");

                SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                String myUserId = prefs.getString("USER_ID", "");

                if (post.getAuthorId() != null && post.getAuthorId().getId() != null && post.getAuthorId().getId().equals(myUserId)) {
                    popupMenu.getMenu().add(Menu.NONE, 2, 2, "Xóa bài viết");
                }

                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1:
                            if (saveListener != null) saveListener.onSavePost(post.getId());
                            return true;
                        case 2:
                            if (deleteListener != null) deleteListener.onDeletePost(post.getId());
                            return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        }

        boolean hasImages = post.getImages() != null && !post.getImages().isEmpty();
        boolean hasVideos = post.getVideos() != null && !post.getVideos().isEmpty();
        if (hasImages || hasVideos) {
            holder.rvPostImages.setVisibility(View.VISIBLE);
            PostImageAdapter imageAdapter = new PostImageAdapter(context, post.getImages(), post.getVideos());
            holder.rvPostImages.setAdapter(imageAdapter);
        } else {
            holder.rvPostImages.setVisibility(View.GONE);
        }

        if (holder.tvCommentCount != null) {
            holder.tvCommentCount.setText(String.valueOf(post.getcountComment()));
            holder.tvCommentCount.setVisibility(View.VISIBLE);
        }

        int reactCount = post.getcountReaction();
        List<String> topReactions = post.getTopReactions();

        ReactionUiHelper.bindTopReactions(
                holder.layoutTopReactions,
                holder.imgReact1,
                holder.imgReact2,
                holder.tvReactionCount,
                reactCount,
                topReactions
        );

        holder.layoutTopReactions.setOnClickListener(v -> {
            if (context instanceof AppCompatActivity) {
                ReactionListBottomSheet bottomSheet = ReactionListBottomSheet.newInstance(post.getId());
                bottomSheet.show(((AppCompatActivity) context).getSupportFragmentManager(), "ReactionBottomSheet");
            }
        });

        if (holder.btnShare != null) {
            holder.btnShare.setOnClickListener(v -> {
                if (showSharePostSheet(post)) {
                    return;
                }
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");

                String authorName = post.getAuthorId() != null ? post.getAuthorId().getUsername() : "Một người bạn";
                String shareMessage = authorName + " vừa chia sẻ một bài viết thú vị:\n\n"
                        + "\"" + post.getContent() + "\"\n\n"
                        + "👉 Tải ngay ứng dụng để tham gia thảo luận nhé!";

                if (post.getImages() != null && !post.getImages().isEmpty()) {
                    shareMessage += "\n\nXem ảnh tại: " + post.getImages().get(0);
                }

                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ bài viết qua"));
            });
        }

        // ==========================================
        // 👉 ĐÃ SỬA: CHUYỂN DỮ LIỆU TAG SANG POST DETAIL
        // ==========================================
        if (holder.btnComment != null) {
            holder.btnComment.setOnClickListener(v -> {
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("POST_ID", post.getId());
                intent.putExtra("POST_CONTENT", post.getContent());
                intent.putExtra("POST_TIME", post.getCreatedAt());
                intent.putExtra(PostDetailActivity.EXTRA_POST_FEELING, post.getFeeling());

                if (post.getAuthorId() != null) {
                    intent.putExtra("AUTHOR_ID", post.getAuthorId().getId());
                    intent.putExtra("AUTHOR_NAME", post.getAuthorId().getUsername());
                    intent.putExtra("AUTHOR_AVATAR", post.getAuthorId().getAvatar());
                }

                // Chèn thêm thông tin Tag vào Intent
                if (post.getTags() != null && !post.getTags().isEmpty() && post.getTags().get(0) != null) {
                    intent.putExtra("TAG_ID", post.getTags().get(0).getId());
                    intent.putExtra("TAG_NAME", post.getTags().get(0).getUsername());
                    intent.putExtra("TAG_COUNT", post.getTags().size());
                    intent.putStringArrayListExtra(PostDetailActivity.EXTRA_TAG_IDS, getTagIds(post.getTags()));
                    intent.putStringArrayListExtra(PostDetailActivity.EXTRA_TAG_NAMES, getTagNames(post.getTags()));
                    intent.putStringArrayListExtra(PostDetailActivity.EXTRA_TAG_AVATARS, getTagAvatars(post.getTags()));
                }

                if (post.getImages() != null) {
                    intent.putStringArrayListExtra("POST_IMAGES", new ArrayList<>(post.getImages()));
                }
                if (post.getVideos() != null) {
                    intent.putStringArrayListExtra(PostDetailActivity.EXTRA_POST_VIDEOS, new ArrayList<>(post.getVideos()));
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

        if (holder.btnLikeContainer != null) {
            String currentReaction = post.getMyReaction();
            ReactionUiHelper.bindReactionButton(holder.imgLikeIcon, holder.tvLikeLabel, currentReaction);

            holder.btnLikeContainer.setOnClickListener(v -> {
                String newReaction = (post.getMyReaction() != null) ? null : "Like";
                handleReactionUpdate(holder, post, newReaction);
            });

            holder.btnLikeContainer.setOnLongClickListener(v -> {
                View popupView = LayoutInflater.from(context).inflate(R.layout.item_feed_reaction_popup, null);
                PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

                View btnReactLike = popupView.findViewById(R.id.btnReactLike);
                View btnReactLove = popupView.findViewById(R.id.btnReactLove);
                View btnReactHaha = popupView.findViewById(R.id.btnReactHaha);
                View btnReactWow = popupView.findViewById(R.id.btnReactWow);
                View btnReactSad = popupView.findViewById(R.id.btnReactSad);
                View btnReactAngry = popupView.findViewById(R.id.btnReactAngry);

                btnReactLike.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Like"); popupWindow.dismiss(); });
                btnReactLove.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Love"); popupWindow.dismiss(); });
                btnReactHaha.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Haha"); popupWindow.dismiss(); });
                btnReactWow.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Wow"); popupWindow.dismiss(); });
                btnReactSad.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Sad"); popupWindow.dismiss(); });
                btnReactAngry.setOnClickListener(view -> { handleReactionUpdate(holder, post, "Angry"); popupWindow.dismiss(); });

                popupWindow.showAsDropDown(v, 0, -v.getHeight() - 140);
                return true;
            });
        }
    }

    private void showTaggedUsers(List<User> tags) {
        if (tags == null || tags.isEmpty() || !(context instanceof AppCompatActivity)) return;
        TaggedUsersBottomSheet bottomSheet = TaggedUsersBottomSheet.newInstance(tags);
        bottomSheet.show(((AppCompatActivity) context).getSupportFragmentManager(), "TaggedUsersBottomSheet");
    }

    private ArrayList<String> getTagIds(List<User> tags) {
        ArrayList<String> values = new ArrayList<>();
        if (tags == null) return values;
        for (User user : tags) values.add(user != null && user.getId() != null ? user.getId() : "");
        return values;
    }

    private ArrayList<String> getTagNames(List<User> tags) {
        ArrayList<String> values = new ArrayList<>();
        if (tags == null) return values;
        for (User user : tags) values.add(user != null && user.getUsername() != null ? user.getUsername() : "");
        return values;
    }

    private ArrayList<String> getTagAvatars(List<User> tags) {
        ArrayList<String> values = new ArrayList<>();
        if (tags == null) return values;
        for (User user : tags) values.add(user != null && user.getAvatar() != null ? user.getAvatar() : "");
        return values;
    }

    @Override
    public int getItemCount() { return postList != null ? postList.size() : 0; }

    private void handleReactionUpdate(PostViewHolder holder, Post post, String newReactionType) {
        String oldReaction = post.getMyReaction();
        int currentCount = post.getcountReaction();
        List<String> topReactions = post.getTopReactions();

        if (topReactions == null) topReactions = new ArrayList<>();

        if (oldReaction == null && newReactionType != null) {
            currentCount++;
            if (!topReactions.contains(newReactionType)) topReactions.add(0, newReactionType);
        } else if (oldReaction != null && newReactionType == null) {
            currentCount--;
            if (currentCount <= 0) {
                topReactions.clear();
            } else {
                topReactions.remove(oldReaction);
            }
        } else if (oldReaction != null && newReactionType != null && !oldReaction.equals(newReactionType)) {
            topReactions.remove(oldReaction);
            if (!topReactions.contains(newReactionType)) {
                topReactions.add(0, newReactionType);
            }
        }

        if (topReactions.size() > 2) {
            topReactions = new ArrayList<>(topReactions.subList(0, 2));
        }

        post.setMyReaction(newReactionType);
        post.setcountReaction(currentCount);
        post.setTopReactions(topReactions);

        ReactionUiHelper.bindReactionButton(holder.imgLikeIcon, holder.tvLikeLabel, newReactionType);
        ReactionUiHelper.bindTopReactions(
                holder.layoutTopReactions,
                holder.imgReact1,
                holder.imgReact2,
                holder.tvReactionCount,
                currentCount,
                topReactions
        );

        if (reactionListener != null) {
            String typeToSend = newReactionType != null ? newReactionType : oldReaction;
            reactionListener.onReactClick(post.getId(), typeToSend);
        }
    }

    private boolean showSharePostSheet(Post post) {
        if (!(context instanceof AppCompatActivity)) {
            return false;
        }
        if (post == null || post.getId() == null || post.getId().trim().isEmpty()) {
            Toast.makeText(context, "Không tìm thấy bài viết để chia sẻ", Toast.LENGTH_SHORT).show();
            return true;
        }

        SharePostBottomSheet.newInstance(
                post.getId(),
                getAuthorDisplayName(post),
                post.getContent(),
                getFirstImage(post.getImages())
        ).show(((AppCompatActivity) context).getSupportFragmentManager(), SharePostBottomSheet.TAG);
        return true;
    }

    private String getAuthorDisplayName(Post post) {
        if (post != null && post.getAuthorId() != null) {
            String name = post.getAuthorId().getUsername();
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        }
        return "Người dùng";
    }

    private String getFirstImage(List<String> images) {
        return images != null && !images.isEmpty() ? images.get(0) : "";
    }

    private String formatTime(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "Vừa xong";
        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = format.parse(dateString);
            if (date == null) return "Vừa xong";

            long diffMs = System.currentTimeMillis() - date.getTime();
            long minutes = diffMs / (60 * 1000);
            long hours = diffMs / (60 * 60 * 1000);
            long days = hours / 24;

            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            if (days < 7) return days + " ngày trước";

            return new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "Vừa xong";
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvContent, tvCommentCount, tvTime;
        ImageView imgAvatar;
        View btnComment;
        View btnShare;
        ImageView btnMoreOptions;
        ImageView imgPrivacy;
        RecyclerView rvPostImages;
        LinearLayout layoutTopReactions;
        TextView tvReactionCount;
        ImageView imgReact1, imgReact2;
        LinearLayout btnLikeContainer;
        ImageView imgLikeIcon;
        TextView tvLikeLabel;


        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvAuthorName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);



            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
            btnShare = itemView.findViewById(R.id.btnShare);
            imgPrivacy = itemView.findViewById(R.id.imgPrivacy);

            rvPostImages = itemView.findViewById(R.id.rvPostImages);
            rvPostImages.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvPostImages.setOnFlingListener(null);
            PagerSnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(rvPostImages);
            rvPostImages.addItemDecoration(new DotsIndicatorDecoration());

            btnComment = itemView.findViewById(R.id.btnComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);

            layoutTopReactions = itemView.findViewById(R.id.layoutTopReactions);
            tvReactionCount = itemView.findViewById(R.id.tvReactionCount);
            imgReact1 = itemView.findViewById(R.id.imgReact1);
            imgReact2 = itemView.findViewById(R.id.imgReact2);

            btnLikeContainer = itemView.findViewById(R.id.btnLike);
            imgLikeIcon = itemView.findViewById(R.id.imgLike);
            tvLikeLabel = itemView.findViewById(R.id.tvLikeCount);
        }
    }
    private String getFeelingTextInVietnamese(String type) {
        if (type == null) return "";
        switch (type) {
            case "Like": return "tuyệt vời 👍";
            case "Love": return "được yêu ❤️";
            case "Haha": return "vui vẻ 😆";
            case "Wow": return "ngạc nhiên 😮";
            case "Sad": return "buồn 😢";
            case "Angry": return "tức giận 😡";
            case "Lucky": return "may mắn 🍀";
            case "Loved": return "đong đầy tình yêu 🥰";
            case "Sick": return "mệt mỏi 🤒";
            case "Question": return "tò mò 🤔";
            case "Cool": return "rất ngầu 😎";
            case "Smart": return "thông minh 🧠";
            default: return type;
        }
    }
}

