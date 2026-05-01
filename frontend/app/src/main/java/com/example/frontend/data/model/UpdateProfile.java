package com.example.frontend.data.model;

public class UpdateProfile {
    private String username;
    private String bio;
    private String location;
    private String hometown;
    private String gender;
    private String dateOfBirth;

    public UpdateProfile(String bio, String location, String hometown, String gender, String dateOfBirth) {
        this.bio = bio;
        this.location = location;
        this.hometown = hometown;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
    }
}
