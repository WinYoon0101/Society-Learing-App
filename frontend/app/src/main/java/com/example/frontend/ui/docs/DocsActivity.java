package com.example.frontend.ui.docs;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.frontend.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DocsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docs);

        ImageView btnBack = findViewById(R.id.btnBack);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        btnBack.setOnClickListener(v -> finish());

        // Setup ViewPager với Adapter
        DocsPagerAdapter pagerAdapter = new DocsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Liên kết TabLayout và ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Đã đăng");
            } else {
                tab.setText("Đã lưu");
            }
        }).attach();
    }
}