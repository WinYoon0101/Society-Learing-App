package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class GroupInvitation {
    @SerializedName("_id")
    private String id;

    @SerializedName("group")
    private PostGroup group;

    @SerializedName("inviter")
    private User inviter;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }
    public PostGroup getGroup() { return group; }
    public User getInviter() { return inviter; }
    public String getCreatedAt() { return createdAt; }
}
