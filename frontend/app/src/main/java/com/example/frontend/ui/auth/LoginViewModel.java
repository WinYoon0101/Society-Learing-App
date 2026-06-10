package com.example.frontend.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.AndroidViewModel;

import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.repository.AuthRepository;
import com.example.frontend.utils.Result;

public class LoginViewModel extends AndroidViewModel {
    private AuthRepository authRepository;
    private LiveData<Result<LoginResponse>> loginResult;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        // Truyền context vào AuthRepository
        authRepository = new AuthRepository(application.getApplicationContext());
    }

    public LiveData<Result<LoginResponse>> getLoginResult() {
        if (loginResult == null) {
            loginResult = new MutableLiveData<>();
        }
        return loginResult;
    }

    public void login(String email, String password) {
        loginResult = authRepository.login(email, password);
    }
}