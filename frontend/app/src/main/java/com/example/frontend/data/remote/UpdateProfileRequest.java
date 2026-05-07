package com.example.frontend.data.remote;

public class UpdateProfileRequest {
    private String username;
    private String bio;

    public UpdateProfileRequest(String username, String bio) {
        this.username = username;
        this.bio = bio;
    }
}
