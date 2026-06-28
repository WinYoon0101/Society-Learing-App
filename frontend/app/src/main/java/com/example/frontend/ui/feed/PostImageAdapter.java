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

        // Load ảnh hiển thị bên ngoài bảng tin (không zoom)
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.bg_card) // Màu nền xám chờ load ảnh
                .into(holder.imgSliderItem);

        // Sự kiện Click vào ảnh: Mở chế độ xem full màn hình và có Zoom
        holder.imgSliderItem.setOnClickListener(v -> {
            // 1. Tạo Dialog với giao diện toàn màn hình, nền màu đen cho đẹp
            android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

            // 2. Khởi tạo PhotoView của Chris Banes (Hỗ trợ kéo, thả, zoom 2 ngón tay)
            com.github.chrisbanes.photoview.PhotoView photoView = new com.github.chrisbanes.photoview.PhotoView(context);

            // 3. Load ảnh độ phân giải cao vào PhotoView
            Glide.with(context).load(url).into(photoView);

            // 4. Đưa PhotoView vào Dialog và hiển thị
            dialog.setContentView(photoView);
            dialog.show();

            // 5. Tính năng phụ: Chạm nhẹ 1 lần vào ảnh để thoát chế độ xem ảnh
            photoView.setOnClickListener(view -> dialog.dismiss());
        });
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