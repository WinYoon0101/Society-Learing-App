package com.example.frontend.data.remote;

public class OtpRequest {
    private String email;
    private String otp;

    public OtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }
}