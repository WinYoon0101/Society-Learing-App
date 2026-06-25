package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

/** Một yêu cầu tham gia nhóm đang chờ admin duyệt. */
public class PendingMember {
    @SerializedName("userId")
    private String userId;
    @SerializedName("username")
    private String username;
    @SerializedName("avatar")
    private String avatar;
    @SerializedName("requestedAt")
    private String requestedAt;

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public String getRequestedAt() { return requestedAt; }
}
