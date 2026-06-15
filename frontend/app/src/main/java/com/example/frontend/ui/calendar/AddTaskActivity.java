package com.example.frontend.ui.calendar;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;
import com.example.frontend.data.model.Task;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.app.DatePickerDialog;
import android.widget.ImageView;

import java.util.Calendar;

public class AddTaskActivity extends AppCompatActivity {

    private EditText edtTitle;
    private EditText edtDescription;
    private EditText edtDate;

    private Button btnSave;

    private ApiService apiService;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDate = findViewById(R.id.edtDate);

        btnSave = findViewById(R.id.btnSave);

        apiService = ApiClient.getApiService(this);

        btnSave.setOnClickListener(v -> saveTask());
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        edtDate.setFocusable(false);
        edtDate.setClickable(true);

        edtDate.setOnClickListener(v -> showDatePicker());
    }

    private void saveTask() {
        Task task = new Task();
        task.setTitle(edtTitle.getText().toString());
        task.setDescription(edtDescription.getText().toString());
        task.setDate(edtDate.getText().toString());

        apiService.createTask(task).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call,Response<Task> response) {
                if(response.isSuccessful()) {
                    Toast.makeText(AddTaskActivity.this,"Thêm thành công",Toast.LENGTH_SHORT).show();finish();}}
            @Override
            public void onFailure(Call<Task> call,Throwable t) {
                Toast.makeText(AddTaskActivity.this,t.getMessage(),Toast.LENGTH_SHORT).show();}});
    }
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
            String date = selectedYear + "-" + String.format("%02d", selectedMonth + 1) + "-" + String.format("%02d", selectedDay);edtDate.setText(date);}, year, month, day);
        datePickerDialog.show();
    }
}