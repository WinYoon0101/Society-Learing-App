package com.example.admin.data.model;

public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;
}