package com.example.frontend.data.model;

import java.io.Serializable;

public class Question implements Serializable {
    public String question;
    public String A;
    public String B;
    public String C;
    public String D;
    public String correct; // Trả về "A", "B", "C" hoặc "D"
}