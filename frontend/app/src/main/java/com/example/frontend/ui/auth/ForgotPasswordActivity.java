package com.example.frontend.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.EmailRequest;
import com.example.frontend.data.remote.OtpRequest;
import com.example.frontend.data.remote.ResetPasswordRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private LinearLayout layoutEmail, layoutOtp, layoutReset;
    // 1. Khai báo thêm edtConfirmNewPassword
    private TextInputEditText edtEmail, edtOtp, edtNewPassword, edtConfirmNewPassword;
    private MaterialButton btnSendOtp, btnVerifyOtp, btnReset;
    private TextView tvTitle;

    private String email = "";
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = ApiClient.getApiService(this);

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutOtp = findViewById(R.id.layoutOtp);
        layoutReset = findViewById(R.id.layoutReset);

        edtEmail = findViewById(R.id.edtEmail);
        edtOtp = findViewById(R.id.edtOtp);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        // 2. Ánh xạ biến với View trong XML
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnReset = findViewById(R.id.btnReset);
        tvTitle = findViewById(R.id.tvTitle);

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        btnReset.setOnClickListener(v -> resetPassword());

        if (edtEmail != null) {
            edtEmail.setOnEditorActionListener((textView, actionId, event) -> {
                sendOtp();
                return true;
            });
        }
    }

    private void sendOtp() {
        hideKeyboard();
        email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            edtEmail.requestFocus();
            return;
        }

        setButtonLoading(btnSendOtp, true, "Đang gửi...");

        api.sendOtp(new EmailRequest(email)).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setButtonLoading(btnSendOtp, false, "Gửi mã OTP");

                ApiResponse body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess()) {
                    Toast.makeText(ForgotPasswordActivity.this, "OTP đã gửi về email", Toast.LENGTH_SHORT).show();
                    layoutEmail.setVisibility(View.GONE);
                    layoutOtp.setVisibility(View.VISIBLE);
                    tvTitle.setText("Nhập mã OTP");
                    if (edtOtp != null) {
                        edtOtp.requestFocus();
                    }
                } else {
                    String message = body != null && body.getMessage() != null
                            ? body.getMessage()
                            : "Gửi OTP thất bại";
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                setButtonLoading(btnSendOtp, false, "Gửi mã OTP");
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp() {
        hideKeyboard();
        String otp = edtOtp.getText() != null ? edtOtp.getText().toString().trim() : "";

        if (TextUtils.isEmpty(otp)) {
            Toast.makeText(this, "Vui lòng nhập OTP", Toast.LENGTH_SHORT).show();
            edtOtp.requestFocus();
            return;
        }

        setButtonLoading(btnVerifyOtp, true, "Đang xác nhận...");

        api.verifyOtp(new OtpRequest(email, otp)).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setButtonLoading(btnVerifyOtp, false, "Xác nhận OTP");

                ApiResponse body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess()) {
                    layoutOtp.setVisibility(View.GONE);
                    layoutReset.setVisibility(View.VISIBLE);
                    tvTitle.setText("Đặt mật khẩu mới");
                    if (edtNewPassword != null) {
                        edtNewPassword.requestFocus();
                    }
                } else {
                    String message = body != null && body.getMessage() != null
                            ? body.getMessage()
                            : "OTP sai hoặc hết hạn";
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                setButtonLoading(btnVerifyOtp, false, "Xác nhận OTP");
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetPassword() {
        hideKeyboard();
        String newPass = edtNewPassword.getText() != null ? edtNewPassword.getText().toString().trim() : "";
        // 3. Lấy dữ liệu từ ô xác nhận mật khẩu
        String confirmPass = edtConfirmNewPassword.getText() != null ? edtConfirmNewPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(newPass)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
            edtNewPassword.requestFocus();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            edtNewPassword.requestFocus();
            return;
        }

        // 4. Validate mật khẩu xác nhận
        if (TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "Vui lòng xác nhận mật khẩu", Toast.LENGTH_SHORT).show();
            edtConfirmNewPassword.requestFocus();
            return;
        }

        // 5. Kiểm tra hai mật khẩu có khớp nhau không
        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            edtConfirmNewPassword.requestFocus();
            return;
        }

        setButtonLoading(btnReset, true, "Đang đổi...");

        api.resetPassword(new ResetPasswordRequest(email, newPass)).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setButtonLoading(btnReset, false, "Đổi mật khẩu");

                ApiResponse body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                    finish();
                } else {
                    String message = body != null && body.getMessage() != null
                            ? body.getMessage()
                            : "Không thể đổi mật khẩu";
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                setButtonLoading(btnReset, false, "Đổi mật khẩu");
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = edtEmail;
        }
        if (view == null) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setButtonLoading(MaterialButton button, boolean loading, String label) {
        if (button == null) return;
        button.setEnabled(!loading);
        button.setText(label);
    }
}