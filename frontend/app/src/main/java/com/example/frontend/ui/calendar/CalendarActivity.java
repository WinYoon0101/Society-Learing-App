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
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarActivity extends AppCompatActivity {
    private static final boolean USE_FAKE_DATA = true;
    private ImageView btnBack;
    private TextView txtProgress;
    private TextView txtDone;
    private CircularProgressIndicator progressCircle;
    private TaskRepository repository;
    private TextView btnAddTask;
    private List<Task> allTasks = new ArrayList<>();
    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private Chip chipAll;
    private Chip chipDaily;
    private Chip chipImportant;
    private Chip chipUrgent;
    private Chip chipOverdue;
    private List<Chip> chips;
    private TextView txtSelectedDate;
    private String currentFilter = "daily";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        btnBack = findViewById(R.id.btnBack);
        txtProgress = findViewById(R.id.txtProgress);
        txtDone = findViewById(R.id.txtDone);
        progressCircle = findViewById(R.id.progressCircle);
        btnAddTask=findViewById(R.id.btnAddTask);
        chipAll = findViewById(R.id.chipAll);
        chipDaily = findViewById(R.id.chipDaily);
        chipImportant = findViewById(R.id.chipImportant);
        chipUrgent = findViewById(R.id.chipUrgent);
        chipOverdue = findViewById(R.id.chipOverdue);
        chips = new ArrayList<>();
        chips.add(chipAll);
        chips.add(chipDaily);
        chips.add(chipImportant);
        chips.add(chipUrgent);
        chips.add(chipOverdue);
        txtSelectedDate = findViewById(R.id.txtSelectedDate);
        rvTasks = findViewById(R.id.rvTasks);
        adapter = new TaskAdapter();
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);
        repository = new TaskRepository(this);
        btnBack.setOnClickListener(v -> finish());
        btnAddTask.setOnClickListener(v->{BottomSheetAddTask sheet = new BottomSheetAddTask();
            sheet.setOnTaskSavedListener(() -> {loadTasks();});
            sheet.show(getSupportFragmentManager(), "ADD_TASK");
        });
        chipAll.setOnClickListener(v -> {
            currentFilter = "all";
            selectChip(chipAll);
            filterTasks("all");
        });

        chipDaily.setOnClickListener(v -> {
            currentFilter = "daily";
            selectChip(chipDaily);
            filterTasks("daily");
        });

        chipImportant.setOnClickListener(v -> {
            currentFilter = "medium";
            selectChip(chipImportant);
            filterTasks("medium");
        });

        chipUrgent.setOnClickListener(v -> {
            currentFilter = "high";
            selectChip(chipUrgent);
            filterTasks("high");
        });

        chipOverdue.setOnClickListener(v -> {
            currentFilter = "overdue";
            selectChip(chipOverdue);
            filterTasks("overdue");
        });
        adapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(Task task) {
                BottomSheetAddTask sheet = new BottomSheetAddTask(task);
                sheet.setOnTaskSavedListener(() -> {
                    loadTasks();
                });
                sheet.show(getSupportFragmentManager(), "EDIT");
            }

            @Override
            public void onDeleteClick(Task task) {
                repository.deleteTask(task.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        loadTasks();
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(CalendarActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onToggleStatus(Task task) {
                repository.toggleStatus(task.getId()).enqueue(new Callback<Task>() {
                    @Override
                    public void onResponse(Call<Task> call, Response<Task> response) {
                        loadTasks();
                    }
                    @Override
                    public void onFailure(Call<Task> call, Throwable t) {
                    }
                });
            }
        });
        loadTasks();
        selectChip(chipDaily);
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }
    private void loadTasks() {
        repository.getTasks().enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if(response.isSuccessful() && response.body()!=null){
                    allTasks = response.body();
                    filterTasks(currentFilter);
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
    private void filterTasks(String type){
        List<Task> filtered = new ArrayList<>();
        switch (type){
            case "all":
                filtered.addAll(allTasks);
                break;
            case "daily":
                for(Task task : allTasks){
                    if(task.getPriority().equals("daily")){
                        filtered.add(task);
                    }
                }
                break;
            case "medium":
                for(Task task : allTasks){
                    if(task.getPriority().equals("medium")){
                        filtered.add(task);
                    }
                }
                break;
            case "high":
                for(Task task : allTasks){
                    if(task.getPriority().equals("high")){
                        filtered.add(task);
                    }
                }
                break;
            case "overdue":
                long now = System.currentTimeMillis();
                for(Task task : allTasks){
                    try{
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault());
                        Date date = sdf.parse(task.getDueDate());
                        if(date != null && date.getTime() < now && task.getStatus().equals("pending")){
                            filtered.add(task);
                        }
                    }catch(Exception ignored){}
                }
                break;
        }
        adapter.setTasks(filtered);
        txtSelectedDate.setText("Công việc (" + filtered.size() + ")");
    }
    private void selectChip(Chip selectedChip) {
        for (Chip chip : chips) {
            chip.setChipBackgroundColorResource(R.color.gray_chip);
            chip.setTextColor(getResources().getColor(R.color.gray_text));
        }
        selectedChip.setChipBackgroundColorResource(R.color.green);
        selectedChip.setTextColor(getResources().getColor(android.R.color.white));
    }
    private void loadFakeTasks() {

        allTasks.clear();

        Task t1 = new Task();
        t1.setId("1");
        t1.setTitle("Làm báo cáo Android");
        t1.setDescription("Hoàn thành trước 17h");
        t1.setPriority("daily");
        t1.setStatus("completed");
        t1.setDueDate("2026-06-26T08:30:00.000Z");

        Task t2 = new Task();
        t2.setId("2");
        t2.setTitle("Đọc tài liệu AI");
        t2.setDescription("Đọc chương 3");
        t2.setPriority("daily");
        t2.setStatus("pending");
        t2.setDueDate("2026-06-26T10:00:00.000Z");

        Task t3 = new Task();
        t3.setId("3");
        t3.setTitle("Làm Slide");
        t3.setDescription("Chuẩn bị thuyết trình");
        t3.setPriority("medium");
        t3.setStatus("pending");
        t3.setDueDate("2026-06-26T13:30:00.000Z");

        Task t4 = new Task();
        t4.setId("4");
        t4.setTitle("Nộp Assignment");
        t4.setDescription("Môn Android");
        t4.setPriority("high");
        t4.setStatus("completed");
        t4.setDueDate("2026-06-26T16:30:00.000Z");

        allTasks.add(t1);
        allTasks.add(t2);
        allTasks.add(t3);
        allTasks.add(t4);

        adapter.setTasks(allTasks);

        updateProgress();
    }
}