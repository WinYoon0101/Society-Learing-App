package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;

public class Task {

    @SerializedName("_id")
    private String id;
    private String title;
    private String description;
    private String date;
    private String type;
    private String priority;
    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }
    public String getDate() {
        return date;
    }
    public String getType() {
        return type;
    }
    public String getPriority() {
        return priority;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public void setType(String type) {
        this.type = type;
    }
    public void setPriority(String priority) {
        this.priority = priority;
    }
}
