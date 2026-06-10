package com.example.frontend.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    // Hàm tính toán khoảng thời gian từ lúc đăng bài đến hiện tại
    public static String getTimeAgo(String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) return "Vừa xong";

        try {
            // Định dạng chuỗi thời gian do MongoDB trả về
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(createdAt);

            if (date == null) return createdAt;

            long time = date.getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;

            // Tính toán ra định dạng phù hợp
            if (diff < 60 * 1000) {
                return "Vừa xong";
            } else if (diff < 60 * 60 * 1000) {
                return diff / (60 * 1000) + " phút trước";
            } else if (diff < 24 * 60 * 60 * 1000) {
                return diff / (60 * 60 * 1000) + " giờ trước";
            } else if (diff < 7 * 24 * 60 * 60 * 1000) {
                return diff / (24 * 60 * 60 * 1000) + " ngày trước";
            } else {
                // Đã quá 7 ngày thì hiển thị ngày/tháng/năm
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return createdAt; // Nếu lỗi parse thì hiện nguyên bản
        }
    }
}