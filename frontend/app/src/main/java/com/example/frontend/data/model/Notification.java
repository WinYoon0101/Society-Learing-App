package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("_id")
    private String id;

    @SerializedName("senderId")
    private User sender;

    @SerializedName("targetId")
    private String targetId;

    @SerializedName("targetType")
    private String targetType;

    @SerializedName("type")
    private String type;

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId()        { return id; }
    public User getSender()      { return sender; }
    public String getTargetId()  { return targetId; }
    public String getTargetType(){ return targetType; }
    public String getType()      { return type; }
    public boolean isRead()      { return isRead; }
    public String getCreatedAt() { return createdAt; }

    /** Trả về nội dung hiển thị dựa trên type */
    public String getMessage() {
        if (sender == null) return "Có thông báo mới";
        String name = sender.getUsername() != null ? sender.getUsername() : "Ai đó";
        switch (type != null ? type.toUpperCase() : "") {
            case "REACTION":       return name + " đã thích bài viết của bạn";
            case "COMMENT":        return name + " đã bình luận bài viết của bạn";
            case "FRIEND_REQUEST": return name + " đã gửi lời mời kết bạn";
            case "FRIEND_ACCEPT":  return name + " đã chấp nhận lời mời kết bạn";
            case "GROUP_INVITE":   return name + " đã mời bạn vào nhóm";
            default:               return name + " đã tương tác với bạn";
        }
    }
}