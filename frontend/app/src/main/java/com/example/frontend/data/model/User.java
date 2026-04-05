package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("token")
    private String token;

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
}