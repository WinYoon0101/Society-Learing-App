package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class Story {
    @SerializedName("_id")
    private String id;

    @SerializedName("authorId")
    private User author;

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("caption")
    private String caption;

    @SerializedName("expiresAt")
    private String expiresAt;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId()        { return id; }
    public User getAuthor()      { return author; }
    public String getMediaUrl()  { return mediaUrl; }
    public String getMediaType() { return mediaType; }
    public String getCaption()   { return caption; }
    public String getExpiresAt() { return expiresAt; }
    public String getCreatedAt() { return createdAt; }
}
