package com.example.frontend.data.remote;

public class CreateTaskRequest {
    private String title;
    private String description;
    private String dueDate;
    private String priority;

    public CreateTaskRequest(String title, String description, String dueDate, String priority) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
    }
}
