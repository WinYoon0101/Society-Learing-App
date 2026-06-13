package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class PostGroup {
    @SerializedName("_id")
    private String id;

    @SerializedName("groupName")
    private String groupName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("privacy")
    private String privacy;

    public String getId() { return id; }
    public String getGroupName() { return groupName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPrivacy() { return privacy; }
}
