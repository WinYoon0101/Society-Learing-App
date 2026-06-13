package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificationListResponse {
    @SerializedName("data")
    private List<Notification> data;

    @SerializedName("unreadCount")
    private int unreadCount;

    @SerializedName("success")
    private boolean success;

    public List<Notification> getData()  { return data; }
    public int getUnreadCount()          { return unreadCount; }
    public boolean isSuccess()           { return success; }
}