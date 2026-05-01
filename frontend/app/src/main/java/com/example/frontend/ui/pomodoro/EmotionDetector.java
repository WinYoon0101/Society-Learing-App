package com.example.frontend.ui.pomodoro;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class EmotionDetector {
    private Interpreter interpreter;
    private List<String> labels;

    public EmotionDetector(Context context) throws IOException {
        // 1. Nạp Model
        interpreter = new Interpreter(loadModelFile(context));
        // 2. Nạp Nhãn từ file labels.txt
        labels = loadLabelList(context);
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("emotion_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    // Logic đọc file labels.txt từ thư mục assets
    private List<String> loadLabelList(Context context) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line.trim().toUpperCase());
        }
        reader.close();
        return labelList;
    }

    public String predict(Bitmap faceBitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(faceBitmap, 48, 48, true);

        // Chuẩn bị đầu vào (Grayscale 48x48)
        ByteBuffer input = ByteBuffer.allocateDirect(4 * 48 * 48 * 1);
        input.order(ByteOrder.nativeOrder());

        int[] pixels = new int[48 * 48];
        scaled.getPixels(pixels, 0, 48, 0, 0, 48, 48);

        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            float gray = (r * 0.299f + g * 0.587f + b * 0.114f);


            input.putFloat(gray / 255.0f);
        }

        // Đầu ra khớp với số lượng nhãn trong file labels.txt
        float[][] output = new float[1][labels.size()];
        interpreter.run(input, output);

        // Tìm cảm xúc có xác suất cao nhất
        int maxIdx = 0;
        for (int i = 0; i < labels.size(); i++) {
            if (output[0][i] > output[0][maxIdx]) maxIdx = i;
        }

        return labels.get(maxIdx);
    }
}
