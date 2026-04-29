package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class ReactionItem {
    @SerializedName("_id")
    private String id;
    private User userId; // Đối tượng User chứa username và avatar
    private String type; // "Like", "Love", "Haha"...

    public String getId() { return id; }
    public User getUserId() { return userId; }
    public String getType() { return type; }
}