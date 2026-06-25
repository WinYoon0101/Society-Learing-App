package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Comment {
    @SerializedName("_id")
    private String id;

    @SerializedName("content")
    private String content;

    @SerializedName("userId")
    private User userId;

    @SerializedName("parentId")
    private String parentId;

    @SerializedName("replies")
    private List<Comment> replies;

    @SerializedName("countReaction")
    private int countReaction;

    @SerializedName("myReaction")
    private String myReaction;

    @SerializedName("topReactions")
    private List<String> topReactions;

    // 👉 ĐÃ THÊM: Biến theo dõi cấp độ (Độ sâu) của bình luận
    private int depth = 0;

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
    public void setUserId(User userId) { this.userId = userId; }

    // 👉 Getter/Setter cho depth
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
}