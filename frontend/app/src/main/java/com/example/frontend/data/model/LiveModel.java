package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class LiveModel implements Serializable {
    @SerializedName("_id")
    private String id;

    @SerializedName("hostId")
    private User host; // Object User chứa username, avatar

    @SerializedName("liveId")
    private String liveId;

    @SerializedName("title")
    private String title;

    public String getLiveId() { return liveId; }
    public User getHost() { return host; }
    public String getTitle() { return title; }
}