package com.example.frontend.ui.chat;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class SharedPostMessage {
    private static final String PREFIX = "__POST_SHARE__";

    private final String postId;
    private final String authorName;
    private final String content;
    private final String imageUrl;

    private SharedPostMessage(String postId, String authorName, String content, String imageUrl) {
        this.postId = postId;
        this.authorName = authorName;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public static String encode(String postId, String authorName, String content, String imageUrl) {
        try {
            JSONObject json = new JSONObject();
            json.put("postId", safe(postId));
            json.put("authorName", safe(authorName));
            json.put("content", safe(content));
            json.put("imageUrl", safe(imageUrl));
            String payload = Base64.encodeToString(
                    json.toString().getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
            );
            return PREFIX + payload;
        } catch (Exception e) {
            return PREFIX + safe(postId);
        }
    }

    public static SharedPostMessage parse(String text) {
        if (text == null || !text.startsWith(PREFIX)) return null;
        String payload = text.substring(PREFIX.length());
        try {
            String jsonText = new String(Base64.decode(payload, Base64.NO_WRAP), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(jsonText);
            String postId = json.optString("postId", "");
            if (postId.trim().isEmpty()) return null;
            return new SharedPostMessage(
                    postId,
                    json.optString("authorName", ""),
                    json.optString("content", ""),
                    json.optString("imageUrl", "")
            );
        } catch (Exception e) {
            if (payload.trim().isEmpty()) return null;
            return new SharedPostMessage(payload.trim(), "", "", "");
        }
    }

    public static String previewText(String text) {
        return parse(text) != null ? "Đã chia sẻ một bài viết" : text;
    }

    public String getPostId() {
        return postId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
