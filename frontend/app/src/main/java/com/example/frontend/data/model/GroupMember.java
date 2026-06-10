package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class GroupMember {
    @SerializedName("userId")
    private String userId;
    @SerializedName("username")
    private String username;
    @SerializedName("avatar")
    private String avatar;
    @SerializedName("role")
    private String role;
    @SerializedName("joinAt")
    private String joinAt;

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public String getRole() { return role; }
    public String getJoinAt() { return joinAt; }
}
