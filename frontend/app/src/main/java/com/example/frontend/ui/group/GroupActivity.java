package com.example.frontend.ui.group;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import com.example.frontend.ui.group.InvitationsFragment;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.frontend.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class GroupActivity extends AppCompatActivity {

    private static final String[] TAB_TITLES =
            {"Nhóm của bạn", "Bài viết", "Khám phá", "Lời mời"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdd).setOnClickListener(v -> showCreateGroupSheet());
        // Tìm kiếm: chuyển sang tab Khám phá với focus vào ô search
        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            ViewPager2 pager = findViewById(R.id.viewPagerGroup);
            pager.setCurrentItem(2, true); // Tab "Khám phá"
        });

        ViewPager2 viewPager = findViewById(R.id.viewPagerGroup);
        TabLayout tabLayout  = findViewById(R.id.tabGroup);

        int dp36 = (int) (36 * getResources().getDisplayMetrics().density);
        tabLayout.setMinimumHeight(dp36);

        viewPager.setAdapter(new GroupPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();
    }

    private void showCreateGroupSheet() {
        CreateGroupBottomSheet sheet = CreateGroupBottomSheet.newInstance();
        sheet.setOnGroupCreatedListener(() -> {
            // Refresh tab "Nhóm của bạn"
            ViewPager2 pager = findViewById(R.id.viewPagerGroup);
            pager.setCurrentItem(0, true);
            // Fragment sẽ tự reload khi resume
        });
        sheet.show(getSupportFragmentManager(), CreateGroupBottomSheet.TAG);
    }

    private static class GroupPagerAdapter extends FragmentStateAdapter {
        GroupPagerAdapter(FragmentActivity fa) { super(fa); }

        @Override
        public int getItemCount() { return TAB_TITLES.length; }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new MyGroupsFragment();
                case 1: return new GroupFeedFragment();
                case 2: return new DiscoverGroupsFragment();
                default: return new InvitationsFragment();
            }
        }
    }
}