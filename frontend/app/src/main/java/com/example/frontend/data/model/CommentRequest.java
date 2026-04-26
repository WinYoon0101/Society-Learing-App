package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class CommentRequest {

    @SerializedName("postId")
    private String postId;

    @SerializedName("content")
    private String content;

    @SerializedName("parentId")
    private String parentId; // Có thể null nếu là bình luận gốc

    // Constructor để khi gọi API, bạn nhét dữ liệu vào cho nhanh
    public CommentRequest(String postId, String content, String parentId) {
        this.postId = postId;
        this.content = content;
        this.parentId = parentId;
    }
    
}