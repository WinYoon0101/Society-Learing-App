package com.example.frontend.ui.quiz;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Quiz;
import com.example.frontend.data.repository.QuizRepository;
import com.example.frontend.utils.Result;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizViewModel extends AndroidViewModel { // Chuyển sang AndroidViewModel
    private QuizRepository repository;
    private MutableLiveData<Result<Quiz>> quizResult = new MutableLiveData<>();

    public QuizViewModel(@NonNull Application application) {
        super(application);
        // Truyền context từ application vào repository ở đây
        this.repository = new QuizRepository(application);
    }

    public LiveData<Result<Quiz>> getQuizResult() {
        return quizResult;
    }

    public void createQuiz(String content, int num) {
        quizResult.setValue(Result.loading());

        repository.generateAIQuiz(content, num, "Quiz mới", new Callback<ApiResponse<Quiz>>() {
            @Override
            public void onResponse(Call<ApiResponse<Quiz>> call, Response<ApiResponse<Quiz>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Lấy data từ ApiResponse và đẩy vào Result.success
                    quizResult.setValue(Result.success(response.body().getData()));
                } else {
                    quizResult.setValue(Result.error("Không thể tạo Quiz, thử lại sau nhé!"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Quiz>> call, Throwable t) {
                quizResult.setValue(Result.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }
}