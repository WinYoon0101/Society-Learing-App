package com.example.frontend.ui.quiz;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.frontend.R;
import com.example.frontend.data.model.Question;
import com.example.frontend.data.model.Quiz;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class QuizResultActivity extends AppCompatActivity {

    private CircularProgressIndicator cpResult;
    private TextView tvPercent, tvCorrect, tvWrong;
    private MaterialButton btnBackHome;
    private RecyclerView rvReview;
    private QuizReviewAdapter reviewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        // Ánh xạ View
        cpResult = findViewById(R.id.cpResult);
        tvPercent = findViewById(R.id.tvPercent);
        tvCorrect = findViewById(R.id.tvCorrect);
        tvWrong = findViewById(R.id.tvWrong);
        btnBackHome = findViewById(R.id.btnBackHome);
        rvReview = findViewById(R.id.rvReview);

        // Nhận dữ liệu từ Intent
        int correctCount = getIntent().getIntExtra("SCORE", 0);
        int totalCount = getIntent().getIntExtra("TOTAL", 0);
        if (totalCount <= 0) {
            totalCount = 1;
        }
        int wrongCount = totalCount - correctCount;

        // Tính toán phần trăm thực tế
        int percentage = (int) (((double) correctCount / totalCount) * 100);
        percentage = Math.max(0, Math.min(100, percentage));

        // Hiển thị số lượng đúng/sai
        tvCorrect.setText(String.valueOf(correctCount));
        tvWrong.setText(String.valueOf(wrongCount));

        Quiz quiz = (Quiz) getIntent().getSerializableExtra("QUIZ_DATA");
        ArrayList<String> userAnswers = getIntent().getStringArrayListExtra("USER_ANSWERS");
        List<Question> questions = quiz != null && quiz.questions != null
                ? quiz.questions
                : new ArrayList<>();

        if (questions.isEmpty()) {
            rvReview.setVisibility(View.GONE);
        } else {
            rvReview.setVisibility(View.VISIBLE);
            rvReview.setLayoutManager(new LinearLayoutManager(this));
            reviewAdapter = new QuizReviewAdapter();
            rvReview.setAdapter(reviewAdapter);
            reviewAdapter.setData(questions, userAnswers);
        }

        // Chạy hiệu ứng vòng tròn và con số nhảy
        animateResult(percentage);

        btnBackHome.setOnClickListener(v -> finish());
    }

    private void animateResult(int targetPercent) {
        // 1. Chạy thanh Progress của vòng tròn
        cpResult.setProgress(targetPercent, true);

        // 2. Chạy con số % nhảy từ 0 -> targetPercent
        ValueAnimator animator = ValueAnimator.ofInt(0, targetPercent);
        animator.setDuration(1500); // Chạy trong 1.5 giây
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            tvPercent.setText(getString(R.string.percent_format, value));
        });
        animator.start();
    }
}