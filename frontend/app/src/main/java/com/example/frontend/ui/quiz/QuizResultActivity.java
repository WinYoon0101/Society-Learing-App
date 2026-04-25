package com.example.frontend.ui.quiz;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.frontend.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class QuizResultActivity extends AppCompatActivity {

    private CircularProgressIndicator cpResult;
    private TextView tvPercent, tvCorrect, tvWrong;
    private MaterialButton btnBackHome;

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

        // Nhận dữ liệu từ Intent
        int correctCount = getIntent().getIntExtra("SCORE", 0);
        int totalCount = getIntent().getIntExtra("TOTAL", 1);
        int wrongCount = totalCount - correctCount;

        // Tính toán phần trăm thực tế
        int percentage = (int) (((double) correctCount / totalCount) * 100);

        // Hiển thị số lượng đúng/sai
        tvCorrect.setText(String.valueOf(correctCount));
        tvWrong.setText(String.valueOf(wrongCount));

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
            tvPercent.setText(animation.getAnimatedValue().toString() + "%");
        });
        animator.start();
    }
}