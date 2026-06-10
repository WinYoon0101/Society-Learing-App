package com.example.frontend.data.remote;

public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static class EmailRequest {
        private String email;

        public EmailRequest(String email) {
            this.email = email;
        }
    }
}