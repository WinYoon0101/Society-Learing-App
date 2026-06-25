package com.example.frontend.data.repository;

import static im.zego.connection.internal.ZegoConnectionImpl.context;

import android.content.Context;

import com.example.frontend.data.model.Task;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.CreateTaskRequest;
import com.example.frontend.data.remote.UpdateTaskRequest;

import java.util.List;

import retrofit2.Call;

public class TaskRepository {

    private final ApiService apiService;

    public TaskRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    public Call<List<Task>> getTasks() {
        return apiService.getTasks();
    }

    public Call<Task> createTask(CreateTaskRequest request) {
        return apiService.createTask(request);
    }

    public Call<Task> updateTask(String id, UpdateTaskRequest request) {
        return apiService.updateTask(id, request);
    }

    public Call<Void> deleteTask(String id) {
        return apiService.deleteTask(id);
    }

    public Call<Task> toggleStatus(String id) {
        return apiService.toggleStatus(id);
    }

    public Call<List<Task>> getTasksByDate(String date) {
        return apiService.getTasksByDate(date);
    }

    public Call<Task> getTaskById(String id) {
        return apiService.getTaskById(id);
    }
}
