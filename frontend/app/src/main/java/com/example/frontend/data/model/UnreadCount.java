package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

/** Phản hồi cho GET chat/unread-count: { count }. */
public class UnreadCount {
    @SerializedName("count")
    private int count;

    public int getCount() {
        return count;
    }
}
