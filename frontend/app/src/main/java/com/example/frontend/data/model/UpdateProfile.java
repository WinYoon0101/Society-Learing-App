package com.example.frontend.data.model;

public class UpdateProfile {
    private String username;
    private String bio;
    private String intro;

    public UpdateProfile(String username, String bio, String intro) {
        this.username = username;
        this.bio = bio;
        this.intro = intro;
    }
}
