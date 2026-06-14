package com.example.frontend.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    /**
     * Chuyển đổi thời gian dạng chuỗi ISO 8601 từ backend thành định dạng "X phút trước"
     */
    public static String getTimeAgo(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return "";
        }

        // Định dạng thời gian thường thấy từ Backend (Node.js/MongoDB)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Đặt múi giờ UTC

        try {
            Date date = sdf.parse(timeString);
            if (date == null) return timeString;

            long time = date.getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;

            // Xử lý chênh lệch thời gian
            if (diff < 60 * 1000) {
                return "Vừa xong";
            } else if (diff < 60 * 60 * 1000) {
                long minutes = diff / (60 * 1000);
                return minutes + " phút trước";
            } else if (diff < 24 * 60 * 60 * 1000) {
                long hours = diff / (60 * 60 * 1000);
                return hours + " giờ trước";
            } else if (diff < 7L * 24 * 60 * 60 * 1000) {
                long days = diff / (24 * 60 * 60 * 1000);
                return days + " ngày trước";
            } else {
                // Nếu quá 7 ngày thì hiển thị ngày tháng năm
                SimpleDateFormat outFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return outFormat.format(date);
            }

        } catch (ParseException e) {
            // Trường hợp backend trả về format không có milliseconds
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date2 = sdf2.parse(timeString);
                if (date2 != null) {
                    long diff = System.currentTimeMillis() - date2.getTime();
                    if (diff < 60 * 1000) return "Vừa xong";
                    if (diff < 60 * 60 * 1000) return (diff / (60 * 1000)) + " phút trước";
                    if (diff < 24 * 60 * 60 * 1000) return (diff / (60 * 60 * 1000)) + " giờ trước";
                    if (diff < 7L * 24 * 60 * 60 * 1000) return (diff / (24 * 60 * 60 * 1000)) + " ngày trước";
                    SimpleDateFormat outFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    return outFormat.format(date2);
                }
            } catch (ParseException ex) {
                // Nếu vẫn lỗi, in ra log và trả về chuỗi gốc
                ex.printStackTrace();
            }
        }
        return timeString; // Trả về chuỗi gốc nếu mọi nỗ lực parse đều thất bại
    }

    // Overload hàm trong trường hợp getCreatedAt() của bạn trả về kiểu long (milliseconds)
    public static String getTimeAgo(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;

        if (diff < 60 * 1000) return "Vừa xong";
        if (diff < 60 * 60 * 1000) return (diff / (60 * 1000)) + " phút trước";
        if (diff < 24 * 60 * 60 * 1000) return (diff / (60 * 60 * 1000)) + " giờ trước";
        if (diff < 7L * 24 * 60 * 60 * 1000) return (diff / (24 * 60 * 60 * 1000)) + " ngày trước";

        SimpleDateFormat outFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return outFormat.format(new Date(time));
    }
}