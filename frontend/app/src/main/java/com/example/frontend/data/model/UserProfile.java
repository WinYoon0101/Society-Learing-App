package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    @SerializedName("userId")
    private String userID;

    @SerializedName("hometown")
    private String hometown;

    @SerializedName("location")
    private String location;

    @SerializedName("birthday")
    private String birthday;

    @SerializedName("joinDate")
    private String joinedDate;

    public String getUserID() {return userID;}
    public String getHometown() {return hometown;}
    public String getLocation() {return location;}
    public String getBirthday() {return birthday;}
    public String getJoinedDate() {return joinedDate;}
}
