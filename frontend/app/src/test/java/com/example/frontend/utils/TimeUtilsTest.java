package com.example.frontend.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeUtilsTest {
    @Test
    public void timeAgo_seconds() {
        long now = 1_000_000L;
        long event = now - 30_000L; // 30 seconds earlier
        String result = TimeUtils.timeAgo(now, event);
        assertEquals("30 giây trước", result);
    }

    @Test
    public void timeAgo_minutes() {
        long now = 1_000_000L;
        long event = now - 5 * 60_000L; // 5 minutes earlier
        String result = TimeUtils.timeAgo(now, event);
        assertEquals("5 phút trước", result);
    }
}
