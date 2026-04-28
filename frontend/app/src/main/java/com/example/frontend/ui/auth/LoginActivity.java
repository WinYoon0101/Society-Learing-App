package com.example.frontend.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    private TextView tvSignUpLink, tvForgotPassword;
    private CheckBox cbRemember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRemember = findViewById(R.id.cbRemember);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        observeViewModel(); // ✅ FIX

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.login(email, password);
        });

        tvSignUpLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void observeViewModel() {
        viewModel.getLoginResult().observe(this, result -> {
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
                        String userId = result.data.getUser().getId();

                        SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();

                        if (cbRemember.isChecked()) {
                            editor.putString("JWT_TOKEN", token);
                            editor.putString("USER_ID", userId);
                            editor.putBoolean("IS_LOGGED_IN", true);
                        } else {
                            editor.clear();
                        }

                        editor.apply();

                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    }
                    break;

                case ERROR:
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
}