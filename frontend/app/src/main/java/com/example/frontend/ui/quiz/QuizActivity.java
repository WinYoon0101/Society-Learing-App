package com.example.frontend.ui.quiz;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class QuizActivity extends AppCompatActivity {

    private QuizViewModel viewModel;
    private QuizAdapter adapter;

    // FIX: Dùng đúng class của Material Design
    private TextInputEditText edtContent, edtNum;
    private MaterialButton btnGenerate, btnSubmit;
    private LinearProgressIndicator progressBar;
    private RecyclerView rvQuestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        setContentView(R.layout.activity_quiz);

        initViews();
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);

        btnGenerate.setOnClickListener(v -> {
            String text = edtContent.getText().toString().trim();
            String numStr = edtNum.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Nhập nội dung bài học!", Toast.LENGTH_SHORT).show();
                return;
            }
            int num = numStr.isEmpty() ? 5 : Integer.parseInt(numStr);
            viewModel.createQuiz(text, num);
        });

        btnSubmit.setOnClickListener(v -> {
            int score = adapter.getCorrectCount();
            int total = adapter.getTotalQuestions();
            new AlertDialog.Builder(this)
                    .setTitle("Kết quả bài làm")
                    .setMessage("Bạn đúng " + score + "/" + total + " câu!")
                    .setPositiveButton("Làm lại", (d, w) -> {
                        edtContent.setText("");
                        btnSubmit.setVisibility(View.GONE);
                        adapter.setData(new ArrayList<>());
                    })
                    .setNegativeButton("Xem đáp án", null)
                    .show();
        });

        viewModel.getQuizResult().observe(this, result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    btnGenerate.setEnabled(false);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    if (result.data != null) {
                        adapter.setData(result.data.questions);
                        btnSubmit.setVisibility(View.VISIBLE);
                    }
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void initViews() {
        edtContent = findViewById(R.id.edtContent);
        edtNum = findViewById(R.id.edtNum);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        rvQuestions = findViewById(R.id.rvQuestions);

        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizAdapter();
        rvQuestions.setAdapter(adapter);
    }
}