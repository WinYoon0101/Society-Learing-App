package com.example.frontend.ui.pomodoro;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    private TextView txtTimeCountdown;
    private CircularProgressIndicator timerProgress;
    private MaterialButton btnStartPause, btnReset;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 1500000; // 25 phút mặc định
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        txtTimeCountdown = findViewById(R.id.txtTimeCountdown);
        timerProgress = findViewById(R.id.timerProgress);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);

        btnStartPause.setOnClickListener(v -> {
            if (timerRunning) pauseTimer();
            else startTimer();
        });

        btnReset.setOnClickListener(v -> resetTimer());

        updateCountDownText();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                // Cập nhật progress bar (tỉ lệ phần trăm)
                int progress = (int) (millisUntilFinished * 100 / 1500000);
                timerProgress.setProgress(progress);
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                btnStartPause.setIconResource(R.drawable.ic_play);
            }
        }.start();

        timerRunning = true;
        btnStartPause.setIconResource(R.drawable.ic_pause); // Nhớ tạo ic_pause trong drawable nhé
    }

    private void pauseTimer() {
        countDownTimer.cancel();
        timerRunning = false;
        btnStartPause.setIconResource(R.drawable.ic_play);
    }

    private void resetTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timeLeftInMillis = 1500000;
        updateCountDownText();
        timerProgress.setProgress(100);
        timerRunning = false;
        btnStartPause.setIconResource(R.drawable.ic_play);
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        txtTimeCountdown.setText(timeLeftFormatted);
    }
}