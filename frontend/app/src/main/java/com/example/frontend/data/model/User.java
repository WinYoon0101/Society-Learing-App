package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class User implements Serializable {

    // MongoDB populated objects dùng "_id", auth response dùng "id"
    @SerializedName("_id")
    private String mongoId;

    @SerializedName("id")
    private String authId;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("isActive")
    private boolean isActive;

    public User() {}

    public User(String id, String username, String avatar) {
        this.mongoId = id;
        this.username = username;
        this.avatar = avatar;
    }

    public String getId() {
        return mongoId != null ? mongoId : authId;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getAvatar() { return avatar; }
    public boolean isActive() { return isActive; }
}