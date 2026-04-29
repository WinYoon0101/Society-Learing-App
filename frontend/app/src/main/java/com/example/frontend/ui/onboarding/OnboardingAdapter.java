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

    // Danh sách dữ liệu
    int[] images = {
            R.drawable.logo,
            R.drawable.text_logo,
            R.drawable.text_logo
    };

    String[] titles = {
            "Chào mừng",
            "Kết nối bạn bè",
            "Bắt đầu ngay"
    };

    String[] desc = {
            "Ứng dụng mạng xã hội hiện đại",
            "Kết nối mọi người dễ dàng",
            "Trải nghiệm ngay hôm nay"
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

    // Lớp giữ các thành phần giao diện
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