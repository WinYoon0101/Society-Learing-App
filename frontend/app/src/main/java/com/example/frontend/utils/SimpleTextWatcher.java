package com.example.frontend.utils;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    public void afterTextChanged(Editable s) {}
}
