package com.example.frontend.ui.feed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;

import java.util.List;

public class PostImageAdapter extends RecyclerView.Adapter<PostImageAdapter.ImageSliderViewHolder> {

    private Context context;
    private List<String> imageUrls; // Dùng List String vì API sẽ trả về các đường link URL

    public PostImageAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ImageSliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_image, parent, false);
        return new ImageSliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageSliderViewHolder holder, int position) {
        String url = imageUrls.get(position);
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.bg_card) // Màu nền xám chờ load ảnh
                .into(holder.imgSliderItem);
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    public static class ImageSliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSliderItem;

        public ImageSliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSliderItem = itemView.findViewById(R.id.imgSliderItem);
        }
    }
}