package com.example.frontend.ui.quiz;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Quiz;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class QuizListAdapter extends RecyclerView.Adapter<QuizListAdapter.ViewHolder> {
    private static final int COLOR_GREEN = Color.parseColor("#10B981");
    private static final int COLOR_AMBER = Color.parseColor("#F59E0B");
    private static final int COLOR_RED = Color.parseColor("#EF4444");
    private static final int TAG_BG_GREEN = Color.parseColor("#E6F4F1");
    private static final int TAG_BG_AMBER = Color.parseColor("#FEF3C7");
    private static final int TAG_BG_RED = Color.parseColor("#FEE2E2");

    private List<Quiz> list = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Quiz quiz);
    }

    public QuizListAdapter(OnItemClickListener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setData(List<Quiz> list) {
        this.list = (list != null) ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        Quiz quiz = list.get(position);
        if (quiz != null && quiz._id != null) {
            return quiz._id.hashCode();
        }
        return position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Quiz quiz = list.get(position);

        String title = (quiz.title != null) ? quiz.title : holder.itemView.getContext().getString(R.string.quiz_untitled);
        holder.tvTitle.setText(title);
        int questionCount = quiz.questions != null ? quiz.questions.size() : 0;
        holder.tvCount.setText(holder.itemView.getContext().getString(R.string.quiz_count_format, questionCount));

        // Hiển thị tỉ lệ đúng thực tế từ Backend
        int rate = Math.max(0, Math.min(100, quiz.bestScore));
        holder.tvRate.setText(holder.itemView.getContext().getString(R.string.quiz_rate_format, rate));

        if (rate >= 80) {
            holder.tvRate.setTextColor(COLOR_GREEN);
            holder.tagRate.setCardBackgroundColor(TAG_BG_GREEN);
        } else if (rate >= 50) {
            holder.tvRate.setTextColor(COLOR_AMBER);
            holder.tagRate.setCardBackgroundColor(TAG_BG_AMBER);
        } else {
            holder.tvRate.setTextColor(COLOR_RED);
            holder.tagRate.setCardBackgroundColor(TAG_BG_RED);
        }

        // Tự động chọn Icon theo từ khóa trong tiêu đề
//        if (title.contains("vật lý")) holder.imgSubject.setImageResource(R.drawable.ic_physics);
//        else if (title.contains("hóa")) holder.imgSubject.setImageResource(R.drawable.ic_chemistry);
//        else if (title.contains("sinh")) holder.imgSubject.setImageResource(R.drawable.ic_biology);
//        else if (title.contains("anh") || title.contains("english")) holder.imgSubject.setImageResource(R.drawable.ic_english);
//        else holder.imgSubject.setImageResource(R.drawable.ic_book); // Mặc định

        holder.itemView.setOnClickListener(v -> listener.onItemClick(quiz));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCount, tvRate;
        ImageView imgSubject;
        MaterialCardView tagRate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCount = itemView.findViewById(R.id.tvCount);
            tvRate = itemView.findViewById(R.id.tvRate);
            imgSubject = itemView.findViewById(R.id.imgSubject);
            tagRate = itemView.findViewById(R.id.tagRate);
        }
    }
}