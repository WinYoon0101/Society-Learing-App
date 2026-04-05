package com.example.frontend.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.repository.AuthRepository;
import com.example.frontend.utils.Result;

public class LoginViewModel extends ViewModel {
    private AuthRepository authRepository;
    private LiveData<Result<LoginResponse>> loginResult;

    public LoginViewModel() {
        authRepository = new AuthRepository();
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