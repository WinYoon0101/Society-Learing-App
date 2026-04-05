package com.example.frontend.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.frontend.R;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.utils.Result;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ View từ file XML của bạn
        // Lưu ý: Đảm bảo bạn đã thêm android:id="@+id/..." vào các thẻ tương ứng trong XML
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

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
                        btnLogin.setText("Login");

                        // Kiểm tra an toàn trước khi lấy dữ liệu
                        if (result.data != null && result.data.getData() != null) {

                            // Chui vào getData() để lấy token và username
                            String token = result.data.getData().getToken();
                            String username = result.data.getData().getUsername();

                            // Lưu Token vào SharedPreferences
                            SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("JWT_TOKEN", token);
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công! Chào " + username, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Lỗi đọc dữ liệu từ Server", Toast.LENGTH_SHORT).show();
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