package com.example.frontend.ui.auth;

import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.EmailRequest;
import com.example.frontend.data.remote.OtpRequest;
import com.example.frontend.data.remote.ResetPasswordRequest;
import com.example.frontend.data.model.ApiResponse;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private LinearLayout layoutEmail, layoutOtp, layoutReset;
    private TextInputEditText edtEmail, edtOtp, edtNewPassword;
    private MaterialButton btnSendOtp, btnVerifyOtp, btnReset;
    private TextView tvTitle;

    private String email = "";

    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = ApiClient.getApiService(this);

        // ánh xạ
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutOtp = findViewById(R.id.layoutOtp);
        layoutReset = findViewById(R.id.layoutReset);

        edtEmail = findViewById(R.id.edtEmail);
        edtOtp = findViewById(R.id.edtOtp);
        edtNewPassword = findViewById(R.id.edtNewPassword);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnReset = findViewById(R.id.btnReset);

        tvTitle = findViewById(R.id.tvTitle);

        // ================= STEP 1 =================
        btnSendOtp.setOnClickListener(v -> {
            email = edtEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Nhập email", Toast.LENGTH_SHORT).show();
                return;
            }

            api.sendOtp(new EmailRequest(email)).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body().isSuccess()) {

                        Toast.makeText(ForgotPasswordActivity.this, "OTP đã gửi", Toast.LENGTH_SHORT).show();

                        layoutEmail.setVisibility(View.GONE);
                        layoutOtp.setVisibility(View.VISIBLE);
                        tvTitle.setText("Nhập mã OTP");

                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Gửi OTP thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // ================= STEP 2 =================
        btnVerifyOtp.setOnClickListener(v -> {
            String otp = edtOtp.getText().toString().trim();

            if (otp.isEmpty()) {
                Toast.makeText(this, "Nhập OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            api.verifyOtp(new OtpRequest(email, otp)).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {

                    if (response.isSuccessful() && response.body().isSuccess()) {

                        layoutOtp.setVisibility(View.GONE);
                        layoutReset.setVisibility(View.VISIBLE);
                        tvTitle.setText("Đặt mật khẩu mới");

                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "OTP sai", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(ForgotPasswordActivity.this, "Lỗi", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // ================= STEP 3 =================
        btnReset.setOnClickListener(v -> {
            String newPass = edtNewPassword.getText().toString().trim();

            if (newPass.isEmpty()) {
                Toast.makeText(this, "Nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
                return;
            }

            api.resetPassword(new ResetPasswordRequest(email, newPass)).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {

                    if (response.isSuccessful() && response.body().isSuccess()) {

                        Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                        finish();

                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Lỗi reset", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}