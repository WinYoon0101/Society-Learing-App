package com.example.frontend.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.ui.main.HomeActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {

            SharedPreferences pref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            boolean isLoggedIn = pref.getBoolean("IS_LOGGED_IN", false);

            if (isLoggedIn) {
                startActivity(new Intent(this, HomeActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }

            finish();

        }, 1500);
    }
}