package com.example.frontend.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Quiz;
import com.example.frontend.data.repository.QuizRepository;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizListActivity extends AppCompatActivity {
    private RecyclerView rvQuizList;
    private QuizListAdapter adapter;
    private QuizRepository repository;
    private LinearProgressIndicator progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_list);

        initViews();
        loadData();
    }

    private void initViews() {
        rvQuizList = findViewById(R.id.rvQuizList);
        progressBar = findViewById(R.id.progressBar);

        // Nút quay lại
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvQuizList.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new QuizListAdapter(quiz -> {
            Intent intent = new Intent(this, QuizActivity.class);
            // ĐẢM BẢO class Quiz đã implements Serializable
            intent.putExtra("QUIZ_DATA", quiz);
            startActivity(intent);
        });

        rvQuizList.setAdapter(adapter);

        findViewById(R.id.fabCreate).setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
        });
    }

    private void loadData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        repository = new QuizRepository(this);
        repository.getMyQuizzes(new Callback<ApiResponse<List<Quiz>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Quiz>>> call, Response<ApiResponse<List<Quiz>>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body().getData());
                } else {
                    Toast.makeText(QuizListActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Quiz>>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(QuizListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData(); // Cập nhật lại danh sách khi vừa tạo Quiz mới xong
    }
}