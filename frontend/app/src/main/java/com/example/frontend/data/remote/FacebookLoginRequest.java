package com.example.frontend.data.remote;

public class FacebookLoginRequest {

    private String accessToken;

    public FacebookLoginRequest(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}