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

import java.util.ArrayList;
import java.util.List;

public class QuizListAdapter extends RecyclerView.Adapter<QuizListAdapter.ViewHolder> {
    private List<Quiz> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Quiz quiz);
    }

    public QuizListAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Quiz> list) {
        this.list = list;
        notifyDataSetChanged();
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

        holder.tvTitle.setText(quiz.title != null ? quiz.title : "Không tiêu đề");
        holder.tvCount.setText(quiz.questions != null ? quiz.questions.size() + " Câu hỏi" : "0 Câu hỏi");

        // Hiển thị tỉ lệ đúng thực tế từ Backend
        int rate = quiz.bestScore;
        holder.tvRate.setText("Tỉ lệ đúng: " + rate + "%");

        // Đổi màu theo kết quả (Xanh nếu >= 80%, Đỏ nếu thấp)
        if (rate >= 80) holder.tvRate.setTextColor(Color.parseColor("#10B981"));
        else if (rate < 50) holder.tvRate.setTextColor(Color.parseColor("#EF4444"));

        // Tự động chọn Icon theo từ khóa trong tiêu đề
        String title = quiz.title.toLowerCase();
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
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCount = itemView.findViewById(R.id.tvCount);
            tvRate = itemView.findViewById(R.id.tvRate);
            imgSubject = itemView.findViewById(R.id.imgSubject);
        }
    }
}