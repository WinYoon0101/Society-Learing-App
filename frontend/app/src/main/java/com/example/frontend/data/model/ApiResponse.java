package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public T data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}