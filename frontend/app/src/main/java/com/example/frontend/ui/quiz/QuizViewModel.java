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

public class QuizViewModel extends AndroidViewModel {
    
    private final QuizRepository repository;
    private final MutableLiveData<Result<Quiz>> quizResult = new MutableLiveData<>();

    public QuizViewModel(@NonNull Application application) {
        super(application);
        this.repository = new QuizRepository(application);
    }

    public LiveData<Result<Quiz>> getQuizResult() {
        return quizResult;
    }

    public void createQuiz(String content, int num) {
        quizResult.setValue(Result.loading());


        repository.generateAIQuiz(content, num, content, new Callback<ApiResponse<Quiz>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Quiz>> call, @NonNull Response<ApiResponse<Quiz>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    quizResult.setValue(Result.success(response.body().getData()));
                } else {

                    quizResult.setValue(Result.error("Không thể tạo Quiz (Lỗi: " + response.code() + "). Hãy thử lại sau!"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Quiz>> call, @NonNull Throwable t) {

                quizResult.setValue(Result.error("Lỗi kết nối: " + t.getLocalizedMessage()));
            }
        });
    }
}