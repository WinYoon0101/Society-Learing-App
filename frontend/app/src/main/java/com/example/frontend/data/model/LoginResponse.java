package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("user")
    private User user;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    public User getUser() { return user; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
}