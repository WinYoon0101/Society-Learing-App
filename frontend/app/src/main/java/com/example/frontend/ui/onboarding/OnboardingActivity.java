package com.example.frontend.ui.onboarding;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.frontend.R;
import com.example.frontend.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import android.view.View;
public class OnboardingActivity extends AppCompatActivity {

    ViewPager2 viewPager;
    MaterialButton btnNext;
    TextView btnSkip;
    LinearLayout layoutDots;

    int total = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        layoutDots = findViewById(R.id.layoutDots);

        viewPager.setAdapter(new OnboardingAdapter());

        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupDots(position);

                if (position == total - 1) {
                    btnNext.setText("Tham gia ngay");
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText("Tiếp tục");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());

        btnNext.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();

            if (pos < total - 1) {
                viewPager.setCurrentItem(pos + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    private void setupDots(int current) {
        layoutDots.removeAllViews();

        for (int i = 0; i < total; i++) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(30);
            dot.setPadding(8, 0, 8, 0);

            if (i == current) {
                dot.setAlpha(1f);
            } else {
                dot.setAlpha(0.3f);
            }

            layoutDots.addView(dot);
        }
    }

    private void finishOnboarding() {
        SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        pref.edit().putBoolean("HAS_SEEN_ONBOARDING", true).apply();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}