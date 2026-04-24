package com.example.frontend.ui.quiz;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.frontend.R;
import com.example.frontend.data.model.Question;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.ViewHolder> {
    private List<Question> list = new ArrayList<>();
    private Set<Integer> answeredPositions = new HashSet<>(); // Lưu những câu đã làm
    private int correctCount = 0;

    public void setData(List<Question> list) {
        this.list = list;
        this.answeredPositions.clear();
        this.correctCount = 0;
        notifyDataSetChanged();
    }

    public int getCorrectCount() { return correctCount; }
    public int getTotalQuestions() { return list.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Layout này ông đã tạo ở bước trước đó nha
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question q = list.get(position);
        holder.tvQuestion.setText("Câu " + (position + 1) + ": " + q.question);
        holder.rbA.setText(q.A);
        holder.rbB.setText(q.B);
        holder.rbC.setText(q.C);
        holder.rbD.setText(q.D);

        // Reset trạng thái View khi scroll
        holder.rgOptions.setOnCheckedChangeListener(null);
        holder.rgOptions.clearCheck();
        resetUI(holder);

        // Nếu câu này đã làm rồi
        if (answeredPositions.contains(position)) {
            lockAndShow(holder, q.correct);
        } else {
            // Logic chọn phát biết luôn
            holder.rgOptions.setOnCheckedChangeListener((group, checkedId) -> {
                answeredPositions.add(position);
                String selected = "";
                if (checkedId == R.id.rbA) selected = "A";
                else if (checkedId == R.id.rbB) selected = "B";
                else if (checkedId == R.id.rbC) selected = "C";
                else if (checkedId == R.id.rbD) selected = "D";

                if (selected.equals(q.correct)) {
                    correctCount++;
                    // Hiệu ứng xanh nếu đúng (Tùy chọn: dùng TextView feedback nếu có)
                }
                lockAndShow(holder, q.correct);
            });
        }
    }

    private void lockAndShow(ViewHolder holder, String correct) {
        for (int i = 0; i < holder.rgOptions.getChildCount(); i++) {
            RadioButton rb = (RadioButton) holder.rgOptions.getChildAt(i);
            rb.setEnabled(false); // Khóa không cho chọn lại
            String tag = (i == 0) ? "A" : (i == 1) ? "B" : (i == 2) ? "C" : "D";
            if (tag.equals(correct)) {
                rb.setTextColor(Color.parseColor("#10B981"));
                if (!rb.getText().toString().contains("✔")) { // Kiểm tra nếu chưa có dấu ✔ thì mới thêm
                    rb.setText(rb.getText() + " ✔");
                }
            }
        }
    }

    private void resetUI(ViewHolder holder) {
        for (int i = 0; i < holder.rgOptions.getChildCount(); i++) {
            RadioButton rb = (RadioButton) holder.rgOptions.getChildAt(i);
            rb.setEnabled(true);
            rb.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion;
        RadioGroup rgOptions;
        RadioButton rbA, rbB, rbC, rbD;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            rgOptions = itemView.findViewById(R.id.rgOptions);
            rbA = itemView.findViewById(R.id.rbA);
            rbB = itemView.findViewById(R.id.rbB);
            rbC = itemView.findViewById(R.id.rbC);
            rbD = itemView.findViewById(R.id.rbD);
        }
    }
}