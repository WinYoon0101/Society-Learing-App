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

 
    @SerializedName("content")
    private String content;

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters and Setters
    public String getId() { return id; }
    public User getSender() { return sender; }
    public String getType() { return type; }
    public String getTargetId() { return targetId; }
    public String getContent() { return content; } // Thêm getter cho content
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getCreatedAt() { return createdAt; }
}