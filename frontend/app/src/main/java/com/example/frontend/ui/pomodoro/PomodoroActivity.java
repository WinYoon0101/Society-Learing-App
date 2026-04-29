package com.example.frontend.ui.pomodoro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.frontend.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
public class PomodoroActivity extends AppCompatActivity {

    // UI Components
    private TextView txtTimeCountdown, txtAIMessage;
    private CircularProgressIndicator timerProgress;
    private PreviewView aiPreviewView;
    private MaterialButton btnStartPause, btnReset, btnSettings, btnBack;

    // AI Components
    private FaceDetector faceDetector;
    private EmotionDetector emotionDetector;
    private long lastAIProcessTime = 0;
    private HashMap<String, Integer> emotionStats = new HashMap<>();

    // Timer Components
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 1500000; // 25 mins
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        initUI();
        initAI();
        checkPermissions();
        setupListeners();
    }

    private void initUI() {
        txtTimeCountdown = findViewById(R.id.txtTimeCountdown);
        txtAIMessage = findViewById(R.id.txtAIMessage);
        timerProgress = findViewById(R.id.timerProgress);
        aiPreviewView = findViewById(R.id.aiPreviewView);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnSettings = findViewById(R.id.btnSettings);
        btnBack = findViewById(R.id.btnBack);

        txtAIMessage.setText("Sẵn sàng tập trung chưa? Nhấn Bắt đầu nhé!");
    }

    private void setupListeners() {
        // Nút Start/Pause
        btnStartPause.setOnClickListener(v -> {
            if (timerRunning) pauseTimer();
            else startTimer();
        });

        // Nút Reset
        btnReset.setOnClickListener(v -> resetTimer());

        // Nút Quay lại
        btnBack.setOnClickListener(v -> finish());

        // Nút Cài đặt (Hàm Menu chính)
        btnSettings.setOnClickListener(v -> showSettingsMenu());
    }

    // --- LOGIC MENU SETTINGS ---
    private void showSettingsMenu() {
        String[] options = {"Kết thúc phiên học ngay", "Chọn nhạc tập trung", "Thông tin AI"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Cài đặt phiên học")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: confirmFinishEarly(); break;
                        case 1: showMusicSelection(); break;
                        case 2:
                            Toast.makeText(this, "AI đang theo dõi biểu cảm để hỗ trợ bạn!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void confirmFinishEarly() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Kết thúc sớm?")
                .setMessage("Bạn có muốn dừng phiên học này để xem kết quả thống kê không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> showSummary())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showMusicSelection() {
        String[] music = {"Tắt nhạc", "Lo-fi Chill", "Tiếng mưa rơi", "Sóng biển"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Nhạc nền tập trung")
                .setItems(music, (dialog, which) -> {
                    Toast.makeText(this, "Đã chọn: " + music[which], Toast.LENGTH_SHORT).show();
                    // Dương có thể thêm logic MediaPlayer ở đây để phát nhạc thật
                })
                .show();
    }

    // --- LOGIC AI & CAMERA ---
    private void initAI() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        faceDetector = FaceDetection.getClient(options);

        try {
            emotionDetector = new EmotionDetector(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        emotionStats.put("HAPPY", 0);
        emotionStats.put("NEUTRAL", 0);
        emotionStats.put("SAD", 0);
        emotionStats.put("ANGER", 0);
        emotionStats.put("SURPRISED", 0);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(aiPreviewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(480, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(Executors.newSingleThreadExecutor(), imageProxy -> {
                    long currentTime = System.currentTimeMillis();
                    if (timerRunning && (currentTime - lastAIProcessTime > 2000)) {
                        processImageProxy(imageProxy);
                        lastAIProcessTime = currentTime;
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis);

            } catch (ExecutionException | InterruptedException e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImageProxy(androidx.camera.core.ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) { imageProxy.close(); return; }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        faceDetector.process(image).addOnSuccessListener(faces -> {
            if (!faces.isEmpty()) {
                Bitmap bitmap = aiPreviewView.getBitmap();
                if (bitmap != null) {
                    Rect rect = faces.get(0).getBoundingBox();
                    Bitmap face = cropFace(bitmap, rect, imageProxy);
                    if (face != null) {
                        String emotion = emotionDetector.predict(face);
                        handleSmartLogic(emotion);
                    }
                }
            }
        }).addOnCompleteListener(task -> imageProxy.close());
    }

    private Bitmap cropFace(Bitmap original, Rect rect, androidx.camera.core.ImageProxy proxy) {
        try {
            float scaleX = (float) original.getWidth() / (float) proxy.getHeight();
            float scaleY = (float) original.getHeight() / (float) proxy.getWidth();
            int padding = (int) (rect.width() * 0.15);
            int x = Math.max(0, (int) (rect.left * scaleX) - padding);
            int y = Math.max(0, (int) (rect.top * scaleY) - padding);
            int w = Math.min((int) (rect.width() * scaleX) + (padding * 2), original.getWidth() - x);
            int h = Math.min((int) (rect.height() * scaleY) + (padding * 2), original.getHeight() - y);
            return Bitmap.createBitmap(original, x, y, w, h);
        } catch (Exception e) { return null; }
    }

    private void handleSmartLogic(String emotion) {
        emotionStats.put(emotion, emotionStats.getOrDefault(emotion, 0) + 1);
        runOnUiThread(() -> {
            switch (emotion) {
                case "NEUTRAL": txtAIMessage.setText("Bạn tập trung tốt quá. Cố lên nhé! 🔥"); break;
                case "HAPPY": txtAIMessage.setText("Tâm trạng tốt sẽ học nhanh hơn đó! 😊"); break;
                case "ANGER":
                case "SAD": txtAIMessage.setText("Có vẻ bạn đang stress. Hít sâu nào... 🌿"); break;
                case "SURPRISED": txtAIMessage.setText("Đừng để xao nhãng, tập trung lại nhé! 👀"); break;
            }
        });
    }

    // --- TIMER LOGIC ---
    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                timerProgress.setProgress((int) (millisUntilFinished * 100 / 1500000));
            }
            @Override
            public void onFinish() {
                timerRunning = false;
                btnStartPause.setIconResource(R.drawable.ic_play);
                showSummary();
            }
        }.start();
        timerRunning = true;
        btnStartPause.setIconResource(R.drawable.ic_pause);
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timerRunning = false;
        btnStartPause.setIconResource(R.drawable.ic_play);
    }

    private void resetTimer() {
        pauseTimer();
        timeLeftInMillis = 1500000;
        updateCountDownText();
        timerProgress.setProgress(100);
        txtAIMessage.setText("Đã reset. Bắt đầu lại nào!");
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        txtTimeCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void showSummary() {
        if (countDownTimer != null) countDownTimer.cancel();
        Intent intent = new Intent(this, PomodoroSummaryActivity.class);
        intent.putExtra("STATS", emotionStats);
        startActivity(intent);
        finish();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
}