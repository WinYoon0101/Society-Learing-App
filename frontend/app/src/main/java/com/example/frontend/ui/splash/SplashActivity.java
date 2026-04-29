package com.example.frontend.ui.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.ui.auth.LoginActivity;
import com.example.frontend.ui.main.HomeActivity;
import com.example.frontend.ui.onboarding.OnboardingActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {

            SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

            boolean hasSeenOnboarding = pref.getBoolean("HAS_SEEN_ONBOARDING", false);
            boolean isLoggedIn = pref.getBoolean("IS_LOGGED_IN", false);

            if (!hasSeenOnboarding) {
                startActivity(new Intent(this, OnboardingActivity.class));
            } else if (isLoggedIn) {
                startActivity(new Intent(this, HomeActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }

            finish();

        }, 1000);
    }
}