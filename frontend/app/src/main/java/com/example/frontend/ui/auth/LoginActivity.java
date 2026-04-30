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
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.GoogleLoginRequest;
import com.example.frontend.ui.main.HomeActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import android.widget.LinearLayout;
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

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        api = ApiClient.getApiService(this);

        observeViewModel();

        // ===== LOGIN NORMAL =====
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

        // ===== GOOGLE CONFIG =====
        String webClientId = getString(R.string.default_web_client_id);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // ===== GOOGLE BUTTON =====
        btnGoogle.setOnClickListener(v -> {
            // 🔥 FIX: luôn cho chọn account lại
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_GOOGLE);
            });
        });
    }

    // ===== GOOGLE RESULT =====
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);

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
                Log.e(TAG, "Google login failed", e);
                Toast.makeText(this,
                        "Google login failed: " + e.getStatusCode(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ===== CALL SERVER =====
    private void sendGoogleTokenToServer(String idToken) {
        api.googleLogin(new GoogleLoginRequest(idToken))
                .enqueue(new retrofit2.Callback<com.example.frontend.data.model.ApiResponse<LoginResponse>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.frontend.data.model.ApiResponse<LoginResponse>> call,
                                           retrofit2.Response<com.example.frontend.data.model.ApiResponse<LoginResponse>> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            LoginResponse data = response.body().getData();

                            if (data == null || data.getUser() == null) {
                                Toast.makeText(LoginActivity.this,
                                        "Dữ liệu lỗi", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            saveLogin(data);
                            Toast.makeText(LoginActivity.this,
                                    "Login Google thành công",
                                    Toast.LENGTH_SHORT).show();

                            goToHome();

                        } else {
                            String err = "";
                            try {
                                if (response.errorBody() != null)
                                    err = response.errorBody().string();
                            } catch (IOException e) {
                                Log.e(TAG, "errorBody read fail", e);
                            }

                            Log.e(TAG, "Google login failed: " + err);
                            Toast.makeText(LoginActivity.this,
                                    "Google login thất bại",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.frontend.data.model.ApiResponse<LoginResponse>> call,
                                          Throwable t) {
                        Log.e(TAG, "Network error", t);
                        Toast.makeText(LoginActivity.this,
                                "Lỗi mạng: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ===== SAVE LOGIN =====
    private void saveLogin(LoginResponse data) {
        SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString("JWT_TOKEN", data.getAccessToken());
        editor.putString("USER_ID", data.getUser().getId());
        editor.putBoolean("IS_LOGGED_IN", true);

        editor.apply();
    }

    // ===== LOGOUT GOOGLE (DÙNG CHO NÚT LOGOUT) =====
    private void logoutGoogle() {
        googleSignInClient.revokeAccess().addOnCompleteListener(task -> {
            SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            pref.edit().clear().apply();

            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ===== NAVIGATE =====
    private void goToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    // ===== OBSERVE LOGIN NORMAL =====
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
}