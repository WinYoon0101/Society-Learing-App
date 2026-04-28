package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Comment {
    @SerializedName("_id")
    private String id;

    @SerializedName("content")
    private String content;

    @SerializedName("userId")
    private User userId; // Object User chứa id, username, avatarUrl

    @SerializedName("parentId")
    private String parentId; // Dùng để quyết định việc thụt lề UI

    @SerializedName("replies")
    private List<Comment> replies;// QUAN TRỌNG: Mảng chứa các câu trả lời

    public String getId() { return id; }
    public String getContent() { return content; }
    public User getUserId() { return userId; }
    public String getParentId() { return parentId; }

    public List<Comment> getReplies() { return replies; }

}
