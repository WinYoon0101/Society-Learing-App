package com.example.admin.data.model;
import java.util.List;

public class NotificationRequest {
    private String content;
    private String type;
    private List<String> userIds; // Thêm dòng này

    public NotificationRequest(String content, String type, List<String> userIds) {
        this.content = content;
        this.type = type;
        this.userIds = userIds;
    }

    public String getContent() { return content; }
    public String getType() { return type; }
    public List<String> getUserIds() { return userIds; }
}