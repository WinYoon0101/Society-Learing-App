package com.example.frontend.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Quiz;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

public class QuizActivity extends AppCompatActivity {

    private QuizViewModel viewModel;
    private QuizAdapter adapter;
    private TextInputEditText edtContent, edtNum;
    private MaterialButton btnGenerate, btnSubmit;
    private LinearProgressIndicator progressBar;
    private RecyclerView rvQuestions;
    private MaterialCardView cardAIInput; // Box dán nội dung AI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_quiz);

        initViews();
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);

        // 1. KIỂM TRA XEM MỞ TỪ DANH SÁCH HAY TẠO MỚI
        Quiz existingQuiz = (Quiz) getIntent().getSerializableExtra("QUIZ_DATA");

        if (existingQuiz != null) {
            // Chế độ: Làm bài từ danh sách -> Ẩn box AI, hiện câu hỏi luôn
            cardAIInput.setVisibility(View.GONE);
            adapter.setData(existingQuiz.questions);
            btnSubmit.setVisibility(View.VISIBLE);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(existingQuiz.title);
        }

        // 2. Logic tạo Quiz mới bằng AI
        btnGenerate.setOnClickListener(v -> {
            String text = edtContent.getText().toString().trim();
            String numStr = edtNum.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Hãy dán nội dung bài học vào nhé!", Toast.LENGTH_SHORT).show();
                return;
            }
            int num = numStr.isEmpty() ? 5 : Integer.parseInt(numStr);
            viewModel.createQuiz(text, num);
        });

        // 3. Nộp bài -> Mở màn hình kết quả 80%
        btnSubmit.setOnClickListener(v -> {
            int score = adapter.getCorrectCount();
            int total = adapter.getTotalQuestions();

            Intent intent = new Intent(this, QuizResultActivity.class);
            intent.putExtra("SCORE", score);
            intent.putExtra("TOTAL", total);
            // Gửi thêm QuizId để Backend lưu lại tỉ lệ đúng thực tế
            if (existingQuiz != null) intent.putExtra("QUIZ_ID", existingQuiz._id);

            startActivity(intent);
            finish(); // Làm xong thì đóng màn hình này
        });

        // Observer nhận kết quả từ AI
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
                        rvQuestions.smoothScrollToPosition(0);
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
        cardAIInput = findViewById(R.id.cardAIInput);
        edtContent = findViewById(R.id.edtContent);
        edtNum = findViewById(R.id.edtNum);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        rvQuestions = findViewById(R.id.rvQuestions);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });

        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizAdapter();
        rvQuestions.setAdapter(adapter);
    }
}