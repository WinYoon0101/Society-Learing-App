package com.example.frontend.ui.feed;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HashtagTextHelper {
    private static final int HASHTAG_COLOR = Color.parseColor("#1877F2");
    private static final Pattern HASHTAG_PATTERN =
            Pattern.compile("(^|\\s)(#[\\p{L}\\p{M}\\p{N}_]+)");

    private HashtagTextHelper() {
    }

    public static CharSequence highlight(String content) {
        String safeContent = content == null ? "" : content;
        SpannableString spannable = new SpannableString(safeContent);
        Matcher matcher = HASHTAG_PATTERN.matcher(safeContent);
        while (matcher.find()) {
            int start = matcher.start(2);
            int end = matcher.end(2);
            spannable.setSpan(
                    new ForegroundColorSpan(HASHTAG_COLOR),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return spannable;
    }
}
