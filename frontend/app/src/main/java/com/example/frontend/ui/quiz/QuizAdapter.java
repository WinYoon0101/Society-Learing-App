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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.ViewHolder> {
    private List<Question> list = new ArrayList<>();
    private List<String> userAnswers = new ArrayList<>();

    public void setData(List<Question> list) {
        this.list = (list != null) ? list : Collections.emptyList();
        this.userAnswers = new ArrayList<>();
        for (int i = 0; i < this.list.size(); i++) this.userAnswers.add("");
        notifyDataSetChanged();
    }

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
        holder.tvQuestion.setText(
                holder.itemView.getContext().getString(
                        R.string.question_title_format,
                        position + 1,
                        safeText(q.question)
                )
        );
        holder.rbA.setText(safeText(q.A));
        holder.rbB.setText(safeText(q.B));
        holder.rbC.setText(safeText(q.C));
        holder.rbD.setText(safeText(q.D));

        // Reset trạng thái View khi scroll
        holder.rgOptions.setOnCheckedChangeListener(null);
        holder.rgOptions.clearCheck();

        // Nếu đã có câu trả lời lưu sẵn cho vị trí này thì khôi phục lựa chọn
        String saved = position < userAnswers.size() ? userAnswers.get(position) : "";
        if (!saved.isEmpty()) {
            if (saved.equals("A")) holder.rbA.setChecked(true);
            else if (saved.equals("B")) holder.rbB.setChecked(true);
            else if (saved.equals("C")) holder.rbC.setChecked(true);
            else if (saved.equals("D")) holder.rbD.setChecked(true);
        }

        // Logic chọn chỉ lưu đáp án, chưa chấm điểm
        holder.rgOptions.setOnCheckedChangeListener((group, checkedId) -> {
            String selected = "";
            if (checkedId == R.id.rbA) selected = "A";
            else if (checkedId == R.id.rbB) selected = "B";
            else if (checkedId == R.id.rbC) selected = "C";
            else if (checkedId == R.id.rbD) selected = "D";

            // Lưu lại câu trả lời của người dùng
            if (position < userAnswers.size()) userAnswers.set(position, selected);
        });
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().toUpperCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value != null ? value : "";
    }

    @Override
    public int getItemCount() { return list.size(); }

    public List<String> getUserAnswers() {
        return new ArrayList<>(userAnswers);
    }

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