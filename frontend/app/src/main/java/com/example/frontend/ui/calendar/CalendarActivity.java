package com.example.frontend.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageView;

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

public class CalendarActivity
        extends AppCompatActivity {

    private RecyclerView rvTasks;

    private CalendarView calendarView;

    private TaskAdapter adapter;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_calendar
        );

        ImageView btnBack =
                findViewById(R.id.btnBack);

        ImageView btnAdd =
                findViewById(R.id.btnAddTask);

        calendarView =
                findViewById(R.id.calendarView);

        rvTasks =
                findViewById(R.id.rvTasks);

        rvTasks.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new TaskAdapter(
                        new ArrayList<>()
                );

        rvTasks.setAdapter(adapter);

        apiService = ApiClient.getApiService(this);

        loadTasks();

        btnBack.setOnClickListener(v ->
                finish()
        );

        btnAdd.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            AddTaskActivity.class
                    );

            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadTasks();
    }

    private void loadTasks() {

        apiService.getTasks()
                .enqueue(
                        new Callback<List<Task>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Task>> call,
                                    Response<List<Task>> response) {

                                if(response.isSuccessful()
                                        && response.body() != null) {

                                    adapter.setTasks(
                                            response.body()
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Task>> call,
                                    Throwable t) {

                            }
                        });
    }
}