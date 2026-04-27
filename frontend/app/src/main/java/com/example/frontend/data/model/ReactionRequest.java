package com.example.frontend.data.model;

public class ReactionRequest {
    private String targetId;   // ID của bài viết hoặc comment
    private String targetType; // 'Posts', 'Comment', hoặc 'Stories'
    private String type;       // 'Like', 'Love', 'Haha', 'Wow', 'Angry', 'Sad'

    public ReactionRequest(String targetId, String targetType, String type) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.type = type;
    }
}