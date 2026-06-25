package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class TrendingTopic {
    private String name;
    private int mentions;

    @SerializedName("trendPercentage")
    private float trendPercentage;

    public String getName() { return name; }
    public int getMentions() { return mentions; }
    public float getTrendPercentage() { return trendPercentage; }
}
