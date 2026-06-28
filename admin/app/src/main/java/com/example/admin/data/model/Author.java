package com.example.admin.data.model;

import com.google.gson.annotations.SerializedName;

public class Author {
    @SerializedName("_id")
    private String id;
    private String username;
    private String avatar;

    @SerializedName("isActive")
    private boolean isActive = true;

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }

    // Thêm 2 hàm này
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}