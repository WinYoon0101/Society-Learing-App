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
    private LinearProgressIndicator progressHappy, progressNeutral, progressStress;
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

        cardResult = findViewById(R.id.cardResult);
        cardStats = findViewById(R.id.cardStats);
        cardAdvice = findViewById(R.id.cardAdvice);
    }

    private void displayStats() {
        // Nhận dữ liệu từ Intent gửi qua
        HashMap<String, Integer> stats = (HashMap<String, Integer>) getIntent().getSerializableExtra("STATS");

        if (stats == null) return;

        int happy = stats.getOrDefault("HAPPY", 0);
        int neutral = stats.getOrDefault("NEUTRAL", 0);
        int stress = stats.getOrDefault("ANGER", 0) + stats.getOrDefault("SAD", 0);
        int total = happy + neutral + stress;

        if (total == 0) total = 1; // Tránh lỗi chia cho 0

        // Tính toán phần trăm
        int pHappy = (happy * 100) / total;
        int pNeutral = (neutral * 100) / total;
        int pStress = (stress * 100) / total;

        // Hiển thị lên UI
        progressHappy.setProgress(pHappy, true);
        progressNeutral.setProgress(pNeutral, true);
        progressStress.setProgress(pStress, true);

        // Hiệu suất học tập (Dựa trên tỉ lệ Neutral + Happy)
        int efficiency = pNeutral + (pHappy / 2);
        txtEfficiency.setText(efficiency + "%");

        // Lời khuyên AI tiếng Việt
        String advice;
        if (pNeutral > 60) {
            advice = "Tuyệt vời! Khả năng tập trung của bạn rất đáng nể. Hãy duy trì phong độ này nhé!";
        } else if (pStress > 30) {
            advice = "Phiên học này hơi căng thẳng rồi. Lần tới bạn thử nghe một chút nhạc Lo-fi để thư giãn hơn nhé.";
        } else if (pHappy > 50) {
            advice = "Tinh thần học tập rất tích cực! Sự hào hứng này sẽ giúp bạn nhớ bài lâu hơn đó.";
        } else {
            advice = "Một phiên học ổn định. Cố gắng giảm bớt xao nhãng để tăng hiệu quả hơn nữa nha!";
        }
        txtAISummary.setText(advice);
    }

    private void animateUI() {
        // Tạo hiệu ứng trượt từ dưới lên cho các thẻ
        cardResult.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));

        cardStats.setAlpha(0f);
        cardStats.animate().alpha(1f).setDuration(1000).setStartDelay(300).start();

        cardAdvice.setTranslationY(100f);
        cardAdvice.animate().translationY(0f).alpha(1f).setDuration(800).setStartDelay(500).start();
    }
}