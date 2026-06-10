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

    // Reaction summary for comment (may be absent from some backend responses)
    @SerializedName("countReaction")
    private int countReaction;

    @SerializedName("myReaction")
    private String myReaction;

    @SerializedName("topReactions")
    private List<String> topReactions;

    public String getId() { return id; }
    public String getContent() { return content; }
    public User getUserId() { return userId; }
    public String getParentId() { return parentId; }

    public List<Comment> getReplies() { return replies; }

    public int getCountReaction() { return countReaction; }
    public void setCountReaction(int countReaction) { this.countReaction = countReaction; }

    public String getMyReaction() { return myReaction; }
    public void setMyReaction(String myReaction) { this.myReaction = myReaction; }

    public List<String> getTopReactions() { return topReactions; }
    public void setTopReactions(List<String> topReactions) { this.topReactions = topReactions; }

    // Allow client code to populate user info when backend omits it
    public void setUserId(User userId) { this.userId = userId; }

}
