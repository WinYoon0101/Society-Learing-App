package com.example.frontend.utils;

public class TimeUtils {
    public static String timeAgo(long nowMillis, long eventMillis) {
        long diff = nowMillis - eventMillis;
        long secs = diff / 1000;
        if (secs < 60) return secs + " giây trước";
        long mins = secs / 60;
        if (mins < 60) return mins + " phút trước";
        long hours = mins / 60;
        if (hours < 24) return hours + " giờ trước";
        long days = hours / 24;
        return days + " ngày trước";
    }
}
