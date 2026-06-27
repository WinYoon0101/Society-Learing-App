package com.example.admin.ui.posts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.flex.FlexDelegate;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ToxicScanner {
    private Interpreter tflite;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ToxicScanner(Context context) {
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.addDelegate(new FlexDelegate());
            tflite = new Interpreter(loadModelFile(context, "vietnamese_toxic_model.tflite"), options);
        } catch (Exception e) {
            Log.e("ToxicScanner", "Lỗi nạp mô hình", e);
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws Exception {
        android.content.res.AssetFileDescriptor fd = context.getAssets().openFd(modelPath);
        FileInputStream is = new FileInputStream(fd.getFileDescriptor());
        return is.getChannel().map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }

    public interface ScanCallback {
        void onResult(int label, float confidence, String labelName);
    }

    public void scanPost(String postContent, ScanCallback callback) {
        executor.execute(() -> {
            if (tflite == null || postContent == null) return;

            String cleanText = postContent.toLowerCase().replaceAll("[^\\w\\s]", " ").replaceAll("\\s+", " ").trim();
            String[] inputs = { cleanText };
            float[][] outputs = new float[1][3];

            try {
                tflite.run(inputs, outputs);

                float maxProb = outputs[0][0];
                int maxIndex = 0;
                for (int i = 1; i < 3; i++) {
                    if (outputs[0][i] > maxProb) { maxProb = outputs[0][i]; maxIndex = i; }
                }

                String labelName = maxIndex == 1 ? "THÔ TỤC" : (maxIndex == 2 ? "THÙ ĐỊCH" : "SẠCH");
                final int fLabel = maxIndex; final float fConf = maxProb; final String fName = labelName;

                mainHandler.post(() -> callback.onResult(fLabel, fConf, fName));
            } catch (Exception e) {
                Log.e("AI_ERROR", "Lỗi quét", e);
            }
        });
    }

    public void close() {
        if (tflite != null) { tflite.close(); tflite = null; }
    }
}