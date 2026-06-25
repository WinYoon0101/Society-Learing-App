package com.example.frontend.data.remote;

public class UpdateTaskRequest {
    private String title;
    private String description;
    private String dueDate;
    private String priority;

    public UpdateTaskRequest(String title, String description, String dueDate, String priority) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
    }
}
