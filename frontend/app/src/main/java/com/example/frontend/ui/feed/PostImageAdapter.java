package com.example.frontend.ui.feed;

import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;

import java.util.ArrayList;
import java.util.List;

public class PostImageAdapter extends RecyclerView.Adapter<PostImageAdapter.MediaViewHolder> {

    private static final int TYPE_IMAGE = 0;
    private static final int TYPE_VIDEO = 1;

    private final Context context;
    private final List<MediaItem> mediaItems = new ArrayList<>();

    public PostImageAdapter(Context context, List<String> imageUrls) {
        this(context, imageUrls, null);
    }

    public PostImageAdapter(Context context, List<String> imageUrls, List<String> videoUrls) {
        this.context = context;
        if (imageUrls != null) {
            for (String url : imageUrls) {
                if (url != null && !url.trim().isEmpty()) {
                    mediaItems.add(new MediaItem(url, false));
                }
            }
        }
        if (videoUrls != null) {
            for (String url : videoUrls) {
                if (url != null && !url.trim().isEmpty()) {
                    mediaItems.add(new MediaItem(url, true));
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mediaItems.get(position).isVideo ? TYPE_VIDEO : TYPE_IMAGE;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_VIDEO ? R.layout.item_post_video : R.layout.item_post_image;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new MediaViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);
        if (item.isVideo) {
            Glide.with(context)
                    .load(item.url)
                    .placeholder(R.drawable.ic_video)
                    .error(R.drawable.ic_video)
                    .into(holder.imgVideoThumb);
            holder.itemView.setOnClickListener(v -> showVideoDialog(item.url));
            return;
        }

        Glide.with(context)
                .load(item.url)
                .placeholder(R.drawable.bg_card)
                .into(holder.imgSliderItem);

        holder.imgSliderItem.setOnClickListener(v -> {
            android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            com.github.chrisbanes.photoview.PhotoView photoView = new com.github.chrisbanes.photoview.PhotoView(context);
            Glide.with(context).load(item.url).into(photoView);
            dialog.setContentView(photoView);
            dialog.show();
            photoView.setOnClickListener(view -> dialog.dismiss());
        });
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    private void showVideoDialog(String url) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout container = new FrameLayout(context);
        VideoView videoView = new VideoView(context);
        container.addView(videoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        MediaController mediaController = new MediaController(context);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.parse(url));
        videoView.setOnPreparedListener(mp -> videoView.start());
        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(context, "Khong mo duoc video", Toast.LENGTH_SHORT).show();
            return true;
        });
        dialog.setOnDismissListener(d -> videoView.stopPlayback());
        dialog.setContentView(container);
        dialog.show();
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSliderItem;
        ImageView imgVideoThumb;

        public MediaViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == TYPE_VIDEO) {
                imgVideoThumb = itemView.findViewById(R.id.imgVideoThumb);
            } else {
                imgSliderItem = itemView.findViewById(R.id.imgSliderItem);
            }
        }
    }

    private static class MediaItem {
        final String url;
        final boolean isVideo;

        MediaItem(String url, boolean isVideo) {
            this.url = url;
            this.isVideo = isVideo;
        }
    }
}
