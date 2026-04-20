package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class Post {
    @SerializedName("_id")
    private String id;
    private String content;
    private String image; // Link ảnh từ Cloudinary
    private User authorId; // Object User chứa username, avatar
    private String createdAt;

    // Getter
    public String getContent() { return content; }
    public String getImage() { return image; }
    public User getAuthorId() { return authorId; }
    public String getId() { return id; }
}