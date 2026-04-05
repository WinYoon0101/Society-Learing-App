package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private User data;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public User getData() { return data; }
}