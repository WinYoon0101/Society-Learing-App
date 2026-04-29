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

    @SerializedName("cover")
    private String cover;

    @SerializedName("bio")
    private String bio;

    @SerializedName("hometown")
    private String hometown;

    @SerializedName("location")
    private String location;

    @SerializedName("birthday")
    private String birthday;

    @SerializedName("createdAt")
    private String joinedDate;

    @SerializedName("friendCount")
    private int friendCount;

    @SerializedName("groupCount")
    private int groupCount;

    @SerializedName("gender")
    private String gender;

    public User() {}

    public User(String id, String username, String avatar) {
        this.mongoId = id;
        this.username = username;
        this.avatar = avatar;
    }

    public String getId() {
        String id = mongoId != null ? mongoId : authId;
        return id != null ? id.trim() : null;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getAvatar() { return avatar; }
    public boolean isActive() { return isActive; }
    public String getBio() {return bio;}
    public String getCover() {return cover;}
    public String getHometown() { return hometown; }
    public String getLocation() { return location; }
    public String getBirthday() { return birthday; }
    public String getJoinedDate() { return joinedDate; }
    public int getFriendCount() { return friendCount; }
    public int getGroupCount() { return groupCount; }
    public String getGender() {return gender; }
}