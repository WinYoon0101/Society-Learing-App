package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class GroupDetail {
    @SerializedName("_id")
    private String id;
    @SerializedName("groupName")
    private String groupName;
    @SerializedName("description")
    private String description;
    @SerializedName("avatarUrl")
    private String avatarUrl;
    @SerializedName("coverUrl")
    private String coverUrl;
    @SerializedName("privacy")
    private String privacy;
    @SerializedName("memberCount")
    private int memberCount;
    @SerializedName("isMember")
    private boolean isMember;
    @SerializedName("isAdmin")
    private boolean isAdmin;
    @SerializedName("hasPendingRequest")
    private boolean hasPendingRequest;
    @SerializedName("requirePostApproval")
    private boolean requirePostApproval;
    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getCoverUrl() { return coverUrl; }
    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }
    public int getMemberCount() { return memberCount; }
    public boolean isMember() { return isMember; }
    public boolean isAdmin() { return isAdmin; }
    public boolean hasPendingRequest() { return hasPendingRequest; }
    public boolean isRequirePostApproval() { return requirePostApproval; }
    public String getCreatedAt() { return createdAt; }
}