package com.example.frontend.data.model;

import java.io.Serializable;
import java.util.List;

public class Quiz implements Serializable {
    public String _id;
    public String title;
    public String content;
    public List<Question> questions;

    public int bestScore;

}