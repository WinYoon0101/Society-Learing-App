package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("_id")
    private String id;

    @SerializedName("sender")
    private User sender;

    @SerializedName("type")
    private String type;

    @SerializedName("targetId")
    private String targetId;

    @SerializedName("postId")
    private String postId;


    @SerializedName("content")
    private String content;

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("targetType")
    private String targetType;


    // Getters and Setters
    public String getTargetType() { return targetType; }
    public String getId() { return id; }
    public User getSender() { return sender; }
    public String getType() { return type; }
    public String getTargetId() { return targetId; }
    public String getPostId() { return postId; }
    public String getContent() { return content; } // Thêm getter cho content
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getCreatedAt() { return createdAt; }
}