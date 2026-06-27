package com.example.admin.ui.main; // 1. Đảm bảo package name này đúng với thư mục của bạn

import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.admin.R;

import com.example.admin.ui.dashboard.DashboardFragment;
import com.example.admin.ui.notifications.NotificationsFragment;
import com.example.admin.ui.posts.PostsFragment;
import com.example.admin.ui.users.UsersFragment;

public class MainActivity extends AppCompatActivity {

    // Khai báo các biến UI
    private FrameLayout btnNavDashboard, btnNavReports, btnNavPosts, btnNavUsers;
    private ImageView imgNavDashboard, imgNavReports, imgNavPosts, imgNavUsers;

    // Định nghĩa mã màu
    private final String COLOR_ACTIVE_ICON = "#064E3B";
    private final String COLOR_INACTIVE_ICON = "#A7F3D0";

    // Biến lưu vị trí tab hiện tại để tránh việc nạp lại (reload) cùng 1 màn hình
    private int currentTabIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Ánh xạ các thành phần giao diện
        initViews();

        // 2. Mặc định load màn hình Dashboard đầu tiên (Tab index: 0)
        selectTab(0, imgNavDashboard, new DashboardFragment());

        // 3. Xử lý sự kiện click cho từng Tab
        btnNavDashboard.setOnClickListener(v ->
                selectTab(0, imgNavDashboard, new DashboardFragment())
        );

        btnNavReports.setOnClickListener(v ->
                selectTab(1, imgNavReports, new NotificationsFragment())
        );

        btnNavPosts.setOnClickListener(v ->
                selectTab(2, imgNavPosts, new PostsFragment())
        );

        btnNavUsers.setOnClickListener(v ->
                selectTab(3, imgNavUsers, new UsersFragment())
        );
    }

    private void initViews() {
        // Ánh xạ FrameLayout
        btnNavDashboard = findViewById(R.id.btn_nav_dashboard);
        btnNavReports = findViewById(R.id.btn_nav_reports);
        btnNavPosts = findViewById(R.id.btn_nav_posts);
        btnNavUsers = findViewById(R.id.btn_nav_users);

        // Ánh xạ ImageView
        imgNavDashboard = findViewById(R.id.img_nav_dashboard);
        imgNavReports = findViewById(R.id.img_nav_reports);
        imgNavPosts = findViewById(R.id.img_nav_posts);
        imgNavUsers = findViewById(R.id.img_nav_users);
    }

    // Hàm gộp logic: Kiểm tra click trùng -> Đổi màu -> Đổi màn hình
    private void selectTab(int tabIndex, ImageView activeImage, Fragment fragment) {
        // Nếu user click vào tab đang hiển thị thì KHÔNG làm gì cả (Chống spam click)
        if (currentTabIndex == tabIndex) {
            return;
        }

        // Cập nhật lại vết tab hiện tại
        currentTabIndex = tabIndex;

        // Đổi màu giao diện
        updateNavState(activeImage);

        // Chuyển đổi màn hình
        loadFragment(fragment);
    }

    // Hàm xử lý đổi màu UI
    private void updateNavState(ImageView activeImage) {
        // Bước 1: Reset toàn bộ icon về trạng thái Inactive
        ImageView[] allImages = {imgNavDashboard, imgNavReports, imgNavPosts, imgNavUsers};
        for (ImageView img : allImages) {
            img.setBackgroundResource(android.R.color.transparent);
            img.setColorFilter(Color.parseColor(COLOR_INACTIVE_ICON));
        }

        // Bước 2: Kích hoạt trạng thái Active cho icon vừa được click
        activeImage.setBackgroundResource(R.drawable.bg_item_active);
        activeImage.setColorFilter(Color.parseColor(COLOR_ACTIVE_ICON));
    }

    // Hàm load nội dung mới
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}