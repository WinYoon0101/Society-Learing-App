package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class Friend {

    // Retrofit hiểu:  chữ "_id" trên mạng về thì nhét vào biến "id"
    @SerializedName("_id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("mutualFriends")
    private int mutualFriends;

    private boolean isPending;



    // ==========================================
    // Getter và Setter
    // ==========================================

    public boolean isPending() { return isPending; }
    public void setPending(boolean pending) { isPending = pending; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public int getMutualFriends() { return mutualFriends; }
    public void setMutualFriends(int mutualFriends) { this.mutualFriends = mutualFriends; }


}