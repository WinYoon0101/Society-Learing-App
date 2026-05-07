package com.example.frontend.data.remote;

public class QuizRequest {
    public String text;
    public int numQuestions;
    public String title;

    public QuizRequest(String text, int numQuestions, String title) {
        this.text = text;
        this.numQuestions = numQuestions;
        this.title = title;
    }
}
