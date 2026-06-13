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
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.repository.QuizRepository;
import com.example.frontend.data.model.Question;
import java.util.List;
import java.util.ArrayList;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

public class QuizActivity extends AppCompatActivity {

    private QuizViewModel viewModel;
    private QuizAdapter adapter;
    private QuizRepository repository;
    private TextInputEditText edtContent, edtNum;
    private MaterialButton btnGenerate, btnSubmit;
    private LinearProgressIndicator progressBar;
    private RecyclerView rvQuestions;
    private MaterialCardView cardAIInput; // Box dán nội dung AI
    private Quiz currentQuiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_quiz);

        initViews();
        repository = new QuizRepository(this);
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);

        // 1. KIỂM TRA XEM MỞ TỪ DANH SÁCH HAY TẠO MỚI
        Quiz existingQuiz = (Quiz) getIntent().getSerializableExtra("QUIZ_DATA");

        if (existingQuiz != null) {
            // Chế độ: Làm bài từ danh sách -> Ẩn box AI, hiện câu hỏi luôn
            cardAIInput.setVisibility(View.GONE);
            rvQuestions.setVisibility(View.VISIBLE);
            List<Question> existingQuestions = existingQuiz.questions != null
                    ? existingQuiz.questions
                    : new ArrayList<>();
            adapter.setData(existingQuestions);
            currentQuiz = existingQuiz;
            btnSubmit.setVisibility(View.VISIBLE);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(existingQuiz.title);
        }

        // 2. Logic tạo Quiz mới bằng AI
        btnGenerate.setOnClickListener(v -> {
            String text = edtContent.getText() != null ? edtContent.getText().toString().trim() : "";
            String numStr = edtNum.getText() != null ? edtNum.getText().toString().trim() : "";
            if (text.isEmpty()) {
                Toast.makeText(this, "Hãy dán nội dung bài học vào nhé!", Toast.LENGTH_SHORT).show();
                return;
            }
            int num;
            try {
                num = numStr.isEmpty() ? 5 : Integer.parseInt(numStr);
            } catch (NumberFormatException e) {
                num = 5;
            }
            num = Math.max(1, Math.min(50, num));
            viewModel.createQuiz(text, num);
        });

        // 3. Nộp bài -> Mở màn hình kết quả 80%
        btnSubmit.setOnClickListener(v -> {
            List<Question> questions = currentQuiz != null && currentQuiz.questions != null
                    ? currentQuiz.questions
                    : new ArrayList<>();
            List<String> answers = adapter.getUserAnswers();
            int score = calculateScore(questions, answers);
            int total = adapter.getTotalQuestions();
            if (total <= 0) {
                Toast.makeText(this, "Chưa có câu hỏi để nộp bài", Toast.LENGTH_SHORT).show();
                return;
            }

            // Nếu có quizId thì gọi API nộp bài trước khi mở kết quả
            if (currentQuiz != null && currentQuiz._id != null && !currentQuiz._id.isEmpty()) {
                repository.submitQuiz(currentQuiz._id, answers, new retrofit2.Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                        if (response.isSuccessful()) {
                            Intent intent = new Intent(QuizActivity.this, QuizResultActivity.class);
                            intent.putExtra("SCORE", score);
                            intent.putExtra("TOTAL", total);
                            intent.putExtra("QUIZ_ID", currentQuiz._id);
                            intent.putExtra("QUIZ_DATA", currentQuiz);
                            intent.putStringArrayListExtra("USER_ANSWERS", new ArrayList<>(answers));
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(QuizActivity.this, "Nộp bài thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(QuizActivity.this, QuizResultActivity.class);
                            intent.putExtra("SCORE", score);
                            intent.putExtra("TOTAL", total);
                            intent.putExtra("QUIZ_DATA", currentQuiz);
                            intent.putStringArrayListExtra("USER_ANSWERS", new ArrayList<>(answers));
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                        Toast.makeText(QuizActivity.this, "Không thể nộp bài: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(QuizActivity.this, QuizResultActivity.class);
                        intent.putExtra("SCORE", score);
                        intent.putExtra("TOTAL", total);
                        intent.putExtra("QUIZ_DATA", currentQuiz);
                        intent.putStringArrayListExtra("USER_ANSWERS", new ArrayList<>(answers));
                        startActivity(intent);
                        finish();
                    }
                });
            } else {
                Intent intent = new Intent(this, QuizResultActivity.class);
                intent.putExtra("SCORE", score);
                intent.putExtra("TOTAL", total);
                intent.putExtra("QUIZ_DATA", currentQuiz);
                intent.putStringArrayListExtra("USER_ANSWERS", new ArrayList<>(answers));
                startActivity(intent);
                finish(); // Làm xong thì đóng màn hình này
            }
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
                        List<Question> questions = result.data.questions != null
                                ? result.data.questions
                                : new ArrayList<>();
                        cardAIInput.setVisibility(View.GONE);
                        rvQuestions.setVisibility(View.VISIBLE);
                        adapter.setData(questions);
                        currentQuiz = result.data;
                        btnSubmit.setVisibility(questions.isEmpty() ? View.GONE : View.VISIBLE);
                        if (!questions.isEmpty()) {
                            rvQuestions.smoothScrollToPosition(0);
                        }
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
        rvQuestions.setVisibility(View.GONE);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });

        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizAdapter();
        rvQuestions.setAdapter(adapter);
    }

    private int calculateScore(List<Question> questions, List<String> answers) {
        int score = 0;
        int limit = Math.min(questions.size(), answers.size());
        for (int i = 0; i < limit; i++) {
            Question question = questions.get(i);
            String selected = answers.get(i) != null ? answers.get(i).trim().toUpperCase() : "";
            String correct = question != null && question.correct != null
                    ? question.correct.trim().toUpperCase()
                    : "";
            if (!selected.isEmpty() && selected.equals(correct)) {
                score++;
            }
        }
        return score;
    }
}