package com.example.admin.data.model;

import com.google.gson.annotations.SerializedName;
public class Author {
    @SerializedName("_id")
    private String id;
    private String username;
    private String avatar;
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
}