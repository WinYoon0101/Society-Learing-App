package com.example.frontend.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static File getFileFromUri(Context context, Uri uri) {
        try {
            // Tạo file tạm trong bộ nhớ đệm của App
            String fileName = getDisplayName(context, uri);
            if (fileName == null || fileName.trim().isEmpty()) {
                String mimeType = context.getContentResolver().getType(uri);
                String extension = mimeType != null ? MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) : null;
                fileName = "media_" + System.currentTimeMillis() + (extension != null ? "." + extension : "");
            }
            File tempFile = new File(context.getCacheDir(), "upload_temp_" + System.currentTimeMillis() + "_" + sanitizeFileName(fileName));
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getDisplayName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
