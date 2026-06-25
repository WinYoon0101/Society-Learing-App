package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

/** Kết quả "Yêu cầu tham gia": "joined" (nhóm Public, vào thẳng) hoặc "pending" (nhóm Private, chờ duyệt). */
public class RequestJoinResult {
    @SerializedName("status")
    private String status;

    public String getStatus() { return status; }
    public boolean isJoined() { return "joined".equals(status); }
    public boolean isPending() { return "pending".equals(status); }
}
