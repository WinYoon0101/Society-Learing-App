package com.example.frontend.ui.group;

import android.os.Bundle;
import android.widget.Toast;

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
        findViewById(R.id.btnSearch).setOnClickListener(v ->
                Toast.makeText(this, "Tìm kiếm (chưa làm)", Toast.LENGTH_SHORT).show());

        ViewPager2 viewPager = findViewById(R.id.viewPagerGroup);
        TabLayout tabLayout = findViewById(R.id.tabGroup);

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
            // TODO: refresh tab "Nhóm của bạn" sau khi tạo thành công
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
                case 1: return PlaceholderFragment.newInstance("Bài viết");
                case 2: return PlaceholderFragment.newInstance("Khám phá");
                default: return PlaceholderFragment.newInstance("Lời mời");
            }
        }
    }
}
