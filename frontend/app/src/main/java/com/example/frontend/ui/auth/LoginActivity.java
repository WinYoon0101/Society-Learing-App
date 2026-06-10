package com.example.frontend.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;

import java.security.MessageDigest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.remote.FacebookLoginRequest;
import com.example.frontend.data.remote.GoogleLoginRequest;
import com.example.frontend.ui.main.HomeActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    // FACEBOOK
    private CallbackManager callbackManager;

    private LoginButton fbLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {

            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.example.frontend",
                    PackageManager.GET_SIGNING_CERTIFICATES
            );

            for (Signature signature : info.signingInfo.getApkContentsSigners()) {

                MessageDigest md = MessageDigest.getInstance("SHA");

                md.update(signature.toByteArray());

                String hash = Base64.encodeToString(
                        md.digest(),
                        Base64.DEFAULT
                );

                Log.e("FB_HASH", hash);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());

        setContentView(R.layout.activity_login);

        // INIT VIEW
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);

        btnLogin = findViewById(R.id.btnLogin);

        btnGoogle = findViewById(R.id.btnGoogle);

        tvSignUpLink = findViewById(R.id.tvSignUpLink);

        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        cbRemember = findViewById(R.id.cbRemember);

        fbLoginButton = findViewById(R.id.fbLoginButton);

        // PREF
        SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        boolean isRemember = pref.getBoolean("REMEMBER_ME", false);

        if (isRemember) {

            edtEmail.setText(
                    pref.getString("SAVED_EMAIL", "")
            );

            edtPassword.setText(
                    pref.getString("SAVED_PASSWORD", "")
            );

            cbRemember.setChecked(true);
        }

        // VIEWMODEL
        viewModel = new ViewModelProvider(this)
                .get(LoginViewModel.class);

        api = ApiClient.getApiService(this);

        observeViewModel();

        // LOGIN NORMAL
        btnLogin.setOnClickListener(v -> {

            String email = edtEmail.getText().toString().trim();

            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                        this,
                        "Vui lòng nhập đầy đủ thông tin",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            viewModel.login(email, password);
        });

        // REGISTER
        tvSignUpLink.setOnClickListener(v ->
                startActivity(
                        new Intent(this, RegisterActivity.class)
                )
        );

        // FORGOT PASSWORD
        tvForgotPassword.setOnClickListener(v ->
                startActivity(
                        new Intent(this, ForgotPasswordActivity.class)
                )
        );

        // GOOGLE CONFIG
        String webClientId =
                getString(R.string.default_web_client_id);

        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(
                        GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                        .requestEmail()
                        .requestIdToken(webClientId)
                        .build();

        googleSignInClient =
                GoogleSignIn.getClient(this, gso);

        // GOOGLE LOGIN
        btnGoogle.setOnClickListener(v -> {

            googleSignInClient.signOut()
                    .addOnCompleteListener(task -> {

                        Intent signInIntent =
                                googleSignInClient.getSignInIntent();

                        startActivityForResult(
                                signInIntent,
                                RC_GOOGLE
                        );
                    });
        });

        // FACEBOOK
        callbackManager = CallbackManager.Factory.create();

        fbLoginButton.setPermissions(
                "email",
                "public_profile"
        );

        fbLoginButton.registerCallback(
                callbackManager,
                new FacebookCallback<LoginResult>() {

                    @Override
                    public void onSuccess(LoginResult loginResult) {

                        String accessToken =
                                loginResult
                                        .getAccessToken()
                                        .getToken();

                        loginFacebook(accessToken);
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onError(FacebookException error) {

                        Toast.makeText(
                                LoginActivity.this,
                                error.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // FACEBOOK LOGIN API
    private void loginFacebook(String accessToken) {

        api.facebookLogin(
                new FacebookLoginRequest(accessToken)
        ).enqueue(new Callback<ApiResponse<LoginResponse>>() {

            @Override
            public void onResponse(
                    Call<ApiResponse<LoginResponse>> call,
                    Response<ApiResponse<LoginResponse>> response
            ) {

                if (
                        response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                ) {

                    LoginResponse data =
                            response.body().getData();

                    if (data != null) {

                        saveLogin(data);

                        Toast.makeText(
                                LoginActivity.this,
                                "Facebook login thành công",
                                Toast.LENGTH_SHORT
                        ).show();

                        goToHome();
                    }

                } else {

                    Toast.makeText(
                            LoginActivity.this,
                            "Facebook login thất bại",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<ApiResponse<LoginResponse>> call,
                    Throwable t
            ) {

                Toast.makeText(
                        LoginActivity.this,
                        "Lỗi mạng: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // RESULT
    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        // FACEBOOK
        callbackManager.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        // GOOGLE
        if (requestCode == RC_GOOGLE) {

            Task<GoogleSignInAccount> task =
                    GoogleSignIn
                            .getSignedInAccountFromIntent(data);

            try {

                GoogleSignInAccount account =
                        task.getResult(ApiException.class);

                if (account != null) {

                    String idToken = account.getIdToken();

                    if (idToken != null) {

                        sendGoogleTokenToServer(idToken);
                    }
                }

            } catch (ApiException e) {

                Log.e(TAG, "Google login failed", e);

                Toast.makeText(
                        this,
                        "Google login failed",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // GOOGLE LOGIN API
    private void sendGoogleTokenToServer(String idToken) {

        api.googleLogin(
                new GoogleLoginRequest(idToken)
        ).enqueue(new Callback<ApiResponse<LoginResponse>>() {

            @Override
            public void onResponse(
                    Call<ApiResponse<LoginResponse>> call,
                    Response<ApiResponse<LoginResponse>> response
            ) {

                if (
                        response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                ) {

                    LoginResponse data =
                            response.body().getData();

                    if (data != null) {

                        saveLogin(data);

                        Toast.makeText(
                                LoginActivity.this,
                                "Google login thành công",
                                Toast.LENGTH_SHORT
                        ).show();

                        goToHome();
                    }

                } else {

                    Toast.makeText(
                            LoginActivity.this,
                            "Google login thất bại",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<ApiResponse<LoginResponse>> call,
                    Throwable t
            ) {

                Toast.makeText(
                        LoginActivity.this,
                        "Lỗi mạng",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // SAVE LOGIN
    private void saveLogin(LoginResponse data) {

        SharedPreferences pref =
                getSharedPreferences(
                        "MyAppPrefs",
                        MODE_PRIVATE
                );

        SharedPreferences.Editor editor =
                pref.edit();

        editor.putString(
                "JWT_TOKEN",
                data.getAccessToken()
        );

        editor.putString(
                "USER_ID",
                data.getUser().getId()
        );

        editor.putString(
                "USERNAME",
                data.getUser().getUsername()
        );

        editor.putString(
                "USER_AVATAR",
                data.getUser().getAvatar() != null ? data.getUser().getAvatar() : ""
        );

        editor.putBoolean(
                "IS_LOGGED_IN",
                true
        );

        if (cbRemember.isChecked()) {

            editor.putString(
                    "SAVED_EMAIL",
                    edtEmail.getText().toString().trim()
            );

            editor.putString(
                    "SAVED_PASSWORD",
                    edtPassword.getText().toString().trim()
            );

            editor.putBoolean(
                    "REMEMBER_ME",
                    true
            );

        } else {

            editor.remove("SAVED_EMAIL");

            editor.remove("SAVED_PASSWORD");

            editor.putBoolean(
                    "REMEMBER_ME",
                    false
            );
        }

        editor.commit(); // dùng commit() thay apply() để đảm bảo token được ghi xong trước khi chuyển màn hình
    }

    // HOME
    private void goToHome() {

        startActivity(
                new Intent(this, HomeActivity.class)
        );

        finish();
    }

    // OBSERVE LOGIN
    private void observeViewModel() {

        viewModel.getLoginResult()
                .observe(this, result -> {

                    if (result == null) return;

                    switch (result.status) {

                        case LOADING:

                            btnLogin.setEnabled(false);

                            btnLogin.setText("Đang xử lý...");

                            break;

                        case SUCCESS:

                            btnLogin.setEnabled(true);

                            btnLogin.setText("Đăng nhập");

                            if (
                                    result.data != null
                                            && result.data.getUser() != null
                            ) {

                                saveLogin(result.data);

                                goToHome();
                            }

                            break;

                        case ERROR:

                            btnLogin.setEnabled(true);

                            btnLogin.setText("Đăng nhập");

                            Toast.makeText(
                                    this,
                                    result.message,
                                    Toast.LENGTH_LONG
                            ).show();

                            break;
                    }
                });
    }
}