package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Post {
    @SerializedName("_id")
    private String id;

    private String content;

    // ĐÃ SỬA: Nhận danh sách URL ảnh từ Backend
    @SerializedName("images")
    private List<String> images;

    private User authorId;
    private String createdAt;

    private int countComment;
    private int countReaction;
    private String myReaction;

    private List<String> topReactions;

    public List<String> getTopReactions() { return topReactions; }
    public void setTopReactions(List<String> topReactions) { this.topReactions = topReactions; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    // ĐÃ SỬA GETTER & SETTER
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public User getAuthorId() { return authorId; }
    public void setAuthorId(User authorId) { this.authorId = authorId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getcountComment() { return countComment; }
    public void setcountComment(int countComment) { this.countComment = countComment; }

    public int getcountReaction() { return countReaction; }
    public void setcountReaction(int countReaction) { this.countReaction = countReaction; }

    public String getMyReaction() { return myReaction; }
    public void setMyReaction(String myReaction) { this.myReaction = myReaction; }
}