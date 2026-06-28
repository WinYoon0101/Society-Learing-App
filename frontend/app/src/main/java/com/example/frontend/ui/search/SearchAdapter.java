package com.example.frontend.ui.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Group;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.SearchItem;
import com.example.frontend.data.model.User;
import com.example.frontend.ui.feed.HashtagTextHelper;
import com.example.frontend.ui.feed.PostDetailActivity;
import com.example.frontend.ui.group.GroupDetailActivity;
import com.example.frontend.ui.profile.FriendProfileActivity;

// LƯU Ý: Import các Activity đích của bạn vào đây
// import com.example.frontend.ui.profile.UserProfileActivity;
// import com.example.frontend.ui.group.GroupDetailActivity;
// import com.example.frontend.ui.post.PostDetailActivity;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SearchItem> items = new ArrayList<>();
    private Context context;

    public void submit(List<SearchItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == SearchItem.TYPE_USER) {
            View v = inflater.inflate(R.layout.item_search_user, parent, false);
            return new UserViewHolder(v);
        } else if (viewType == SearchItem.TYPE_GROUP) {
            View v = inflater.inflate(R.layout.item_search_group, parent, false);
            return new GroupViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_search_post, parent, false);
            return new PostViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchItem item = items.get(position);

        // =========================================================
        // 1. DỮ LIỆU NGƯỜI DÙNG
        // =========================================================
        if (holder instanceof UserViewHolder) {
            User user = (User) item.getData();
            UserViewHolder userHolder = (UserViewHolder) holder;

            userHolder.tvUsername.setText(user.getUsername());

            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                Glide.with(context).load(user.getAvatar()).placeholder(R.drawable.ic_user).into(userHolder.imgAvatar);
            } else {
                userHolder.imgAvatar.setImageResource(R.drawable.ic_user);
            }

            // XỬ LÝ CLICK: Chuyển đến Trang cá nhân
            userHolder.itemView.setOnClickListener(v -> {
                try {

                    Intent intent = new Intent(context, FriendProfileActivity.class);
                    intent.putExtra("USER_ID", user.getId()); // Gửi ID của user qua màn hình Profile
                    context.startActivity(intent);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // =========================================================
            // 2. DỮ LIỆU NHÓM
            // =========================================================
        } else if (holder instanceof GroupViewHolder) {
            Group group = (Group) item.getData();
            GroupViewHolder groupHolder = (GroupViewHolder) holder;

            groupHolder.tvGroupName.setText(group.getGroupName() != null ? group.getGroupName() : "Tên Nhóm");

            String privacyText = group.getPrivacy() != null ? group.getPrivacy() : "Nhóm";
            groupHolder.tvMemberCount.setText(group.getMemberCount() + " thành viên • " + privacyText);

            if (group.getDescription() != null && !group.getDescription().trim().isEmpty()) {
                groupHolder.tvGroupDescription.setText(group.getDescription());
                groupHolder.tvGroupDescription.setVisibility(View.VISIBLE);
            } else {
                groupHolder.tvGroupDescription.setVisibility(View.GONE);
            }

            if (group.getAvatarUrl() != null && !group.getAvatarUrl().isEmpty()) {
                Glide.with(context).load(group.getAvatarUrl()).placeholder(R.drawable.ic_search).into(groupHolder.imgGroupAvatar);
            } else {
                groupHolder.imgGroupAvatar.setImageResource(R.drawable.ic_search);
            }

            if (group.getCoverUrl() != null && !group.getCoverUrl().isEmpty()) {
                Glide.with(context).load(group.getCoverUrl()).into(groupHolder.imgGroupCover);
            } else {
                groupHolder.imgGroupCover.setBackgroundColor(Color.parseColor("#F3F4F6"));
            }

            // XỬ LÝ CLICK: Chuyển đến Trang Chi tiết Nhóm
            groupHolder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(context, GroupDetailActivity.class);
                    // Kiểm tra xem GroupDetailActivity của bạn nhận key là "GROUP_ID" hay "EXTRA_GROUP_ID" nhé
                    intent.putExtra("GROUP_ID", group.getId());
                    context.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Lỗi khi mở nhóm", Toast.LENGTH_SHORT).show();
                }
            });

            // =========================================================
            // 3. DỮ LIỆU BÀI VIẾT
            // =========================================================
        } else if (holder instanceof PostViewHolder) {
            Post post = (Post) item.getData();
            PostViewHolder postHolder = (PostViewHolder) holder;

            postHolder.tvPostContent.setText(HashtagTextHelper.highlight(post.getContent()));

            if (post.getAuthorId() != null) {
                postHolder.tvAuthorName.setText(post.getAuthorId().getUsername());

                if (post.getAuthorId().getAvatar() != null && !post.getAuthorId().getAvatar().isEmpty()) {
                    Glide.with(context).load(post.getAuthorId().getAvatar()).placeholder(R.drawable.ic_user).into(postHolder.imgAuthorAvatar);
                } else {
                    postHolder.imgAuthorAvatar.setImageResource(R.drawable.ic_user);
                }
            } else {
                postHolder.tvAuthorName.setText("Người dùng ẩn danh");
                postHolder.imgAuthorAvatar.setImageResource(R.drawable.ic_user);
            }

            if (post.getImages() != null && !post.getImages().isEmpty()) {
                postHolder.imgPostImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(post.getImages().get(0))
                        .into(postHolder.imgPostImage);
            } else if (post.getVideos() != null && !post.getVideos().isEmpty()) {
                postHolder.imgPostImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(post.getVideos().get(0))
                        .placeholder(R.drawable.ic_video)
                        .error(R.drawable.ic_video)
                        .into(postHolder.imgPostImage);
            } else {
                postHolder.imgPostImage.setVisibility(View.GONE);
            }

            // XỬ LÝ CLICK: Chuyển đến Trang Chi tiết Bài Viết
            postHolder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("POST_ID", post.getId());
                    context.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Lỗi khi mở bài viết", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- CÁC VIEWHOLDER ---

    static class UserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgAvatar;
        TextView tvUsername;
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
        }
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgGroupAvatar;
        ImageView imgGroupCover;
        TextView tvGroupName;
        TextView tvMemberCount;
        TextView tvGroupDescription;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            imgGroupAvatar = itemView.findViewById(R.id.imgGroupAvatar);
            imgGroupCover = itemView.findViewById(R.id.imgGroupCover);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            tvGroupDescription = itemView.findViewById(R.id.tvGroupDescription);
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgAuthorAvatar;
        TextView tvAuthorName;
        TextView tvPostContent;
        ImageView imgPostImage;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAuthorAvatar = itemView.findViewById(R.id.imgAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);
        }
    }
}
