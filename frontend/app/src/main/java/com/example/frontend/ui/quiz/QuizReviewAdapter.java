package com.example.frontend.ui.quiz;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class QuizReviewAdapter extends RecyclerView.Adapter<QuizReviewAdapter.ViewHolder> {

    private List<Question> questions = new ArrayList<>();
    private List<String> userAnswers = new ArrayList<>();

    public void setData(List<Question> questions, List<String> userAnswers) {
        this.questions = questions != null ? questions : Collections.emptyList();
        this.userAnswers = userAnswers != null ? userAnswers : Collections.emptyList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question question = questions.get(position);
        String selected = position < userAnswers.size() && userAnswers.get(position) != null
                ? userAnswers.get(position).trim().toUpperCase(Locale.ROOT)
                : "";
        String correct = normalize(question != null ? question.correct : "");
        boolean isCorrect = !selected.isEmpty() && selected.equals(correct);

        holder.tvQuestion.setText((position + 1) + ". " + safeText(question != null ? question.question : ""));
        holder.tvUserAnswer.setText("Bạn chọn: " + formatAnswerLabel(selected, question));
        holder.tvCorrectAnswer.setText("Đáp án đúng: " + formatAnswerLabel(correct, question));

        holder.tvStatus.setText(isCorrect ? "ĐÚNG" : (selected.isEmpty() ? "CHƯA CHỌN" : "SAI"));
        holder.tvStatus.setBackgroundColor(Color.parseColor(isCorrect ? "#10B981" : (selected.isEmpty() ? "#64748B" : "#EF4444")));
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    private String formatAnswerLabel(String key, Question question) {
        if (key == null || key.isEmpty()) {
            return "Chưa chọn";
        }
        switch (key) {
            case "A":
                return "A. " + safeText(question != null ? question.A : "");
            case "B":
                return "B. " + safeText(question != null ? question.B : "");
            case "C":
                return "C. " + safeText(question != null ? question.C : "");
            case "D":
                return "D. " + safeText(question != null ? question.D : "");
            default:
                return key;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value != null ? value : "";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvStatus, tvUserAnswer, tvCorrectAnswer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvUserAnswer = itemView.findViewById(R.id.tvUserAnswer);
            tvCorrectAnswer = itemView.findViewById(R.id.tvCorrectAnswer);
        }
    }
}