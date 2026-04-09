package com.example.frontend.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.frontend.R;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.ui.main.HomeActivity;
import com.example.frontend.utils.Result;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLogin;

    private TextView tvSignUpLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ View từ file XML
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);

        // 2. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 3. Bắt sự kiện Click
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi ViewModel xử lý logic gọi mạng
            viewModel.login(email, password);
            observeViewModel();
        });

        // Bắt sự kiện bấm chữ "Sign Up" để qua màn hình Đăng ký
        tvSignUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // 4. Lắng nghe và xử lý kết quả
    private void observeViewModel() {
        viewModel.getLoginResult().observe(this, new Observer<Result<LoginResponse>>() {
            @Override
            public void onChanged(Result<LoginResponse> result) {
                if (result == null) return;

                switch (result.status) {
                    case LOADING:
                        btnLogin.setEnabled(false);
                        btnLogin.setText("Đang xử lý...");
                        break;

                    case SUCCESS:
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Đăng nhập");

                        if (result.data != null && result.data.getUser() != null) {

                            String token = result.data.getAccessToken();
                            String username = result.data.getUser().getUsername();

                            // Lưu Token vào SharedPreferences
                            SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                            sharedPref.edit().putString("JWT_TOKEN", token).apply();

                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công! Chào mừng " + username, Toast.LENGTH_SHORT).show();

                            // ------------------ CHUYỂN SANG TRANG HOME ------------------
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            // Xóa lịch sử màn hình Login để không Back lại được
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                        break;

                    case ERROR:
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        Toast.makeText(LoginActivity.this, result.message, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }
}