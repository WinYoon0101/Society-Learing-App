package com.example.frontend.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

    int[] images = {
            R.drawable.onboarding_1, // feed/social
            R.drawable.onboarding_2, // connect/chat
            R.drawable.onboarding_3  // start/join
    };

    String[] titles = {
            "Khám phá thế giới của bạn",
            "Kết nối không giới hạn",
            "Chia sẻ khoảnh khắc"
    };

    String[] desc = {
            "Lướt feed, khám phá nội dung bạn quan tâm mỗi ngày.",
            "Trò chuyện, tương tác với bạn bè mọi lúc mọi nơi.",
            "Đăng tải hình ảnh, cảm xúc và câu chuyện của riêng bạn."
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        h.img.setImageResource(images[pos]);
        h.title.setText(titles[pos]);
        h.desc.setText(desc[pos]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title, desc;

        ViewHolder(View v) {
            super(v);
            img = v.findViewById(R.id.img);
            title = v.findViewById(R.id.tvTitle);
            desc = v.findViewById(R.id.tvDesc);
        }
    }
}