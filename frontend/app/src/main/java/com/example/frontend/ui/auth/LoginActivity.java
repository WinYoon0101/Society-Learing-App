package com.example.frontend.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.frontend.R;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.ui.main.HomeActivity;
import com.example.frontend.utils.Result;
import android.widget.LinearLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.example.frontend.data.remote.GoogleLoginRequest;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private LoginViewModel viewModel;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLogin;
    private LinearLayout btnGoogle;
    private TextView tvSignUpLink, tvForgotPassword;
    private CheckBox cbRemember;
    private GoogleSignInClient googleSignInClient;
    private ApiService api;
    private static final int RC_GOOGLE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // ===== INIT VIEW =====
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRemember = findViewById(R.id.cbRemember);

        // ===== VIEWMODEL =====
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // ===== API =====
        api = ApiClient.getApiService(this);
        observeViewModel();

        // ===== LOGIN THƯỜNG =====
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.login(email, password);
        });

        // ===== NAVIGATION =====
        tvSignUpLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));

        // ===== GOOGLE LOGIN CONFIG =====
        String webClientId = getString(R.string.default_web_client_id);
        Log.d(TAG, "Using webClientId: " + webClientId);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("854441719795-gqat6aom5ot0u0eqdh6q7v4tu9t6etke.apps.googleusercontent.com")
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // ===== GOOGLE BUTTON =====
        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE);
        });
    }

    // ===== HANDLE RESULT GOOGLE =====
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account == null) {
                    Toast.makeText(this, "Google account null", Toast.LENGTH_SHORT).show();
                    return;
                }
                String idToken = account.getIdToken();
                if (idToken == null) {
                    Toast.makeText(this, "Không lấy được ID Token", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendGoogleTokenToServer(idToken);
            } catch (ApiException e) {
                int statusCode = e.getStatusCode();
                Log.e(TAG, "Google sign-in failed: statusCode=" + statusCode, e);
                Toast.makeText(this, "Google login failed: code=" + statusCode, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected sign-in error", e);
                Toast.makeText(this, "Google login failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===== CALL API GOOGLE =====
    private void sendGoogleTokenToServer(String idToken) {
        api.googleLogin(new GoogleLoginRequest(idToken))
                .enqueue(new retrofit2.Callback<com.example.frontend.data.model.ApiResponse<LoginResponse>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.frontend.data.model.ApiResponse<LoginResponse>> call,
                                           retrofit2.Response<com.example.frontend.data.model.ApiResponse<LoginResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            LoginResponse data = response.body().getData();
                            if (data == null || data.getUser() == null) {
                                Toast.makeText(LoginActivity.this, "Dữ liệu lỗi", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            saveLogin(data);
                            Toast.makeText(LoginActivity.this, "Login Google thành công", Toast.LENGTH_SHORT).show();
                            goToHome();
                        } else {
                            String errBody = "";
                            try {
                                if (response.errorBody() != null) errBody = response.errorBody().string();
                            } catch (IOException io) {
                                Log.e(TAG, "Error reading errorBody", io);
                            }
                            Log.e(TAG, "Google login failed. code=" + response.code() + " body=" + errBody);
                            Toast.makeText(LoginActivity.this, "Google login thất bại: server error", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.frontend.data.model.ApiResponse<LoginResponse>> call, Throwable t) {
                        Log.e(TAG, "Network error on googleLogin", t);
                        Toast.makeText(LoginActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ===== OBSERVE LOGIN THƯỜNG =====
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
                        saveLogin(result.data);
                        goToHome();
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

    // ===== SAVE LOGIN =====
    private void saveLogin(LoginResponse data) {
        String token = data.getAccessToken();
        String userId = data.getUser().getId();
        SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("JWT_TOKEN", token);
        editor.putString("USER_ID", userId);
        editor.putBoolean("IS_LOGGED_IN", true);
        editor.apply();
    }

    // ===== NAVIGATE =====
    private void goToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
