package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class Group {

    @SerializedName("_id")
    private String id;

    @SerializedName("groupName")
    private String groupName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("privacy")
    private String privacy;

    @SerializedName("newPostCount")
    private int newPostCount;

    @SerializedName("lastUpdated")
    private String lastUpdated;

    @SerializedName("description")
    private String description;

    @SerializedName("memberCount")
    private int memberCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public int getNewPostCount() { return newPostCount; }
    public void setNewPostCount(int newPostCount) { this.newPostCount = newPostCount; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}
