package com.example.frontend.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.ui.calendar.TaskAdapter;
import com.example.frontend.data.model.Task;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarActivity extends AppCompatActivity {
    private RecyclerView rvTasks;
    private CalendarView calendarView;
    private TaskAdapter adapter;
    private ApiService apiService;
    private String selectedDate;
    private TextView txtSelectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnAdd = findViewById(R.id.btnAddTask);
        calendarView = findViewById(R.id.calendarView);
        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        txtSelectedDate = findViewById(R.id.txtSelectedDate);
        adapter = new TaskAdapter(new ArrayList<>());
        rvTasks.setAdapter(adapter);
        apiService = ApiClient.getApiService(this);
        loadTasks();
        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> {Intent intent = new Intent(this, AddTaskActivity.class);startActivity(intent);});
        calendarView.setOnDateChangeListener(
                (view, year, month, dayOfMonth) -> {
                    selectedDate = year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", dayOfMonth);
                    txtSelectedDate.setText("Công việc ngày " + dayOfMonth + "/" + (month + 1) + "/" + year);
                    loadTasksByDate(selectedDate);
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(selectedDate != null){
            loadTasksByDate(selectedDate);
        }else{
            loadTasks();
        }
    }

    private void loadTasks() {
        apiService.getTasks().enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    adapter.setTasks(response.body());}}
            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {}});
    }
    private void loadTasksByDate(String date) {
        apiService.getTasksByDate(date).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if(response.isSuccessful() && response.body() != null){
                    adapter.setTasks(response.body());}}
            @Override
            public void onFailure(Call<List<Task>> call,Throwable t) {
                t.printStackTrace();}});
    }
}