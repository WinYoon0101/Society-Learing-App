package com.example.frontend.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.repository.TaskRepository;
import com.example.frontend.ui.calendar.TaskAdapter;
import com.example.frontend.data.model.Task;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.ApiClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TextView txtProgress;
    private TextView txtDone;
    private CircularProgressIndicator progressCircle;
    private TaskRepository repository;
    private TextView btnAddTask;
    private List<Task> allTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        btnBack = findViewById(R.id.btnBack);
        txtProgress = findViewById(R.id.txtProgress);
        txtDone = findViewById(R.id.txtDone);
        progressCircle = findViewById(R.id.progressCircle);
        btnAddTask=findViewById(R.id.btnAddTask);

        repository = new TaskRepository(this);
        btnBack.setOnClickListener(v -> finish());
        btnAddTask.setOnClickListener(v->{BottomSheetAddTask sheet = new BottomSheetAddTask();
            sheet.setOnTaskSavedListener(() -> {loadTasks();});
            sheet.show(getSupportFragmentManager(), "ADD_TASK");
        });
        loadTasks();
    }
    private void loadTasks() {
        repository.getTasks().enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if(response.isSuccessful() && response.body()!=null){
                    allTasks = response.body();
                    updateProgress();
                }

            }
            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(CalendarActivity.this,
                        t.getMessage(),
                        Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void updateProgress() {
        int total = 0;
        int completed = 0;
        for(Task task : allTasks){
            if(task.getPriority().equals("daily")){
                total++;
                if(task.getStatus().equals("completed")){
                    completed++;

                }
            }
        }
        int percent = 0;
        if(total != 0){
            percent = completed * 100 / total;

        }
        txtProgress.setText(percent + "%");
        txtDone.setText(completed + " / " + total + " hoàn thành");
        progressCircle.setProgress(percent);
    }
}