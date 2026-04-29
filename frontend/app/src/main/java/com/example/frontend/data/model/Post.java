package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Post {
    @SerializedName("_id")
    private String id;

    private String content;
    private String image;
    private User authorId;
    private String createdAt;

    private int countComment;
    private int countReaction;
    private String myReaction; // Chứa giá trị: "Like", "Love", "Haha", "Wow", "Sad", "Angry" hoặc null

    private List<String> topReactions; // Chứa danh sách như ["Love", "Haha", "Like"]
    public List<String> getTopReactions() { return topReactions; }
    public void setTopReactions(List<String> topReactions) { this.topReactions = topReactions; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public User getAuthorId() {
        return authorId;
    }

    public void setAuthorId(User authorId) {
        this.authorId = authorId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getcountComment() {
        return countComment;
    }

    public void setcountComment(int countComment) {
        this.countComment = countComment;
    }

    public int getcountReaction() {
        return countReaction;
    }

    public void setcountReaction(int countReaction) {
        this.countReaction = countReaction;
    }

    public String getMyReaction() {
        return myReaction;
    }

    public void setMyReaction(String myReaction) {
        this.myReaction = myReaction;
    }
}