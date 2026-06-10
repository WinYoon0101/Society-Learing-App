package com.example.frontend.data.remote;

import java.util.List;

public class SubmitQuizRequest {
    public String quizId;
    public List<String> answers;

    public SubmitQuizRequest(String quizId, List<String> answers) {
        this.quizId = quizId;
        this.answers = answers;
    }
}
