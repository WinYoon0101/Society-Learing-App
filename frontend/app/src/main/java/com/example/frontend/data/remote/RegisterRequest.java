package com.example.frontend.data.remote;

public class RegisterRequest {
    private String username; // Tương ứng với Họ và Tên trên UI
    private String email;
    private String password;
    private String dateOfBirth;
    private String gender;

    public RegisterRequest(String username, String email, String password, String dateOfBirth, String gender) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
    }
}