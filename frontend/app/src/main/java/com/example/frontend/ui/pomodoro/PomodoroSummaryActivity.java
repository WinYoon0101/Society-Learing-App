package com.example.frontend.ui.pomodoro;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.HashMap;

public class PomodoroSummaryActivity extends AppCompatActivity {

    private TextView txtEfficiency, txtAISummary;
    private LinearProgressIndicator progressHappy, progressNeutral, progressStress, progressSurprised;
    private View cardResult, cardStats, cardAdvice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro_summary);

        initViews();
        displayStats();
        animateUI();

        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());
    }

    private void initViews() {
        txtEfficiency = findViewById(R.id.txtEfficiency);
        txtAISummary = findViewById(R.id.txtAISummary);

        progressHappy = findViewById(R.id.progressHappy);
        progressNeutral = findViewById(R.id.progressNeutral);
        progressStress = findViewById(R.id.progressStress);
        progressSurprised = findViewById(R.id.progressSurprised);

        cardResult = findViewById(R.id.cardResult);
        cardStats = findViewById(R.id.cardStats);
        cardAdvice = findViewById(R.id.cardAdvice);
    }

    @SuppressWarnings("unchecked")
    private void displayStats() {
        // Nhận dữ liệu từ Intent gửi qua
        HashMap<String, Integer> stats = (HashMap<String, Integer>) getIntent().getSerializableExtra("STATS");

        if (stats == null) return;

        int happy = stats.getOrDefault("HAPPY", 0);
        int neutral = stats.getOrDefault("NEUTRAL", 0);
        int stress = stats.getOrDefault("ANGER", 0) + stats.getOrDefault("SAD", 0);

        // Cộng gộp cả ngạc nhiên (SURPRISED) và không có mặt (NO_FACE) thành tổng số lần XAO NHÃNG
        int distracted = stats.getOrDefault("SURPRISED", 0) + stats.getOrDefault("NO_FACE", 0);

        // Tính tổng dựa trên biến distracted mới
        int total = happy + neutral + stress + distracted;
        if (total == 0) total = 1;

        // Tính toán phần trăm
        int pHappy = (happy * 100) / total;
        int pNeutral = (neutral * 100) / total;
        int pStress = (stress * 100) / total;
        int pDistracted = (distracted * 100) / total; // Phần trăm xao nhãng tổng hợp

        // Hiển thị lên UI
        progressHappy.setProgress(pHappy, true);
        progressNeutral.setProgress(pNeutral, true);
        progressStress.setProgress(pStress, true);
        progressSurprised.setProgress(pDistracted, true);

        // Hiệu suất: Tập trung (Neutral) + Tinh thần tốt (Happy/2) - Xao nhãng tổng hợp (pDistracted/2)
        int efficiency = Math.max(0, pNeutral + (pHappy / 2) - (pDistracted / 2));
        txtEfficiency.setText(efficiency + "%");

        // Lời khuyên AI tiếng Việt cập nhật theo biến pDistracted mới
        String advice;
        if (pDistracted > 25) {
            advice = "Phiên này bạn hơi dễ bị xao nhãng rồi đó nha (có lúc ngó lơ hoặc rời khỏi camera luôn nè). Hãy cất điện thoại và chọn chỗ yên tĩnh hơn đi!";
        } else if (pNeutral > 60) {
            advice = "Đỉnh của chóp! Khả năng tập trung của bạn cực kỳ ổn định. Tiếp tục phát huy nhé!";
        } else if (pStress > 30) {
            advice = "Căng thẳng quá rồi! Thả lỏng cơ mặt, uống miếng nước rồi hãy học tiếp nha.";
        } else if (pHappy > 40) {
            advice = "Tinh thần học tập rất tích cực! Sự lạc quan này sẽ giúp bạn tiếp thu bài rất nhanh.";
        } else {
            advice = "Một phiên học khá ổn định. Cố gắng giảm bớt các tác động bên ngoài để tăng hiệu quả hơn nhé!";
        }
        txtAISummary.setText(advice);
    }

    private void animateUI() {
        // Hiệu ứng trượt từ trái sang cho thẻ kết quả chính
        cardResult.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));

        // Hiệu ứng hiện dần cho bảng chi tiết
        cardStats.setAlpha(0f);
        cardStats.animate().alpha(1f).setDuration(1000).setStartDelay(300).start();

        // Hiệu ứng đẩy từ dưới lên cho thẻ lời khuyên
        cardAdvice.setTranslationY(100f);
        cardAdvice.setAlpha(0f);
        cardAdvice.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(500)
                .start();
    }
}