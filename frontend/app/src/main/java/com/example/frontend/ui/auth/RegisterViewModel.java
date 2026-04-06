package com.example.frontend.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.repository.AuthRepository;
import com.example.frontend.utils.Result;

public class RegisterViewModel extends ViewModel {
    private AuthRepository authRepository;
    private MutableLiveData<Result<LoginResponse>> authResult = new MutableLiveData<>();

    public RegisterViewModel() {
        authRepository = new AuthRepository();
    }

    public LiveData<Result<LoginResponse>> getAuthResult() {
        return authResult;
    }

    // Hàm 1: Đăng ký
    public void register(String username, String email, String password, String dateOfBirth, String gender) {
        authRepository.register(username, email, password, dateOfBirth, gender).observeForever(result -> {
            authResult.setValue(result);
        });
    }

    // Hàm 2: Đăng nhập (Dùng để tự động login sau khi đăng ký)
    public void login(String email, String password) {
        authRepository.login(email, password).observeForever(result -> {
            authResult.setValue(result);
        });
    }
}