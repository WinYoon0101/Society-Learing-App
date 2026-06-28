package com.example.frontend.ui.feed;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;

import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {

    private Context context;
    private List<Uri> imageUris;
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onRemove(int position);
        void onImageClick(int position);
    }

    public ImagePreviewAdapter(Context context, List<Uri> imageUris, OnImageClickListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri uri = imageUris.get(position);

        // Load ảnh vào ImageView
        Glide.with(context).load(uri).into(holder.imgItem);

        // Bắt sự kiện bấm nút X
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(position);
        });

        // Bắt sự kiện bấm vào chính bức ảnh
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onImageClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return imageUris != null ? imageUris.size() : 0;
    }

    // Class ViewHolder giờ chỉ chứa đúng 2 View có thật trong XML
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgItem, btnRemove;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgItem = itemView.findViewById(R.id.imgItem);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}