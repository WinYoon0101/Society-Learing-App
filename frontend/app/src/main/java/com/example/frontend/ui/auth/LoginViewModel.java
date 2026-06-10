package com.example.frontend.ui.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.repository.AuthRepository;
import com.example.frontend.utils.Result;

public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Result<LoginResponse>> loginResult = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application.getApplicationContext());
    }

    public LiveData<Result<LoginResponse>> getLoginResult() {
        return loginResult;
    }

    public void login(String email, String password) {
        authRepository.login(email, password)
                .observeForever(result -> loginResult.postValue(result));
    }
}