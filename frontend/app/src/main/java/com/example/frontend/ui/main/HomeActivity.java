package com.example.frontend.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;


import com.example.frontend.ui.auth.LoginActivity;
import com.example.frontend.ui.calendar.CalendarActivity;
import com.example.frontend.ui.docs.DocsActivity;
import com.example.frontend.ui.feed.FeedFragment;
import com.example.frontend.ui.friend.FriendFragment;
import com.example.frontend.ui.chat.ChatFragment;
import com.example.frontend.ui.group.GroupActivity;
import com.example.frontend.ui.library.LibraryFragment;
import com.example.frontend.ui.meeting.MeetingActivity;
import com.example.frontend.ui.notify.NotifyFragment;
import com.example.frontend.ui.pomodoro.PomodoroActivity;
import com.example.frontend.ui.profile.ProfileFragment;
import com.example.frontend.ui.quiz.QuizListActivity;
import com.example.frontend.ui.saved.SavedActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout tabHome, tabFriend, tabChat, tabLibrary, tabNotify, tabProfile;
    private ImageView imgHome, imgFriend, imgChat, imgLibrary, imgNotify, imgProfile;
    private View lineHome, lineFriend, lineChat, lineLibrary, lineNotify, lineProfile;
    private ImageView iconSearch, btnOpenMenu;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupDrawer();
        setupBottomTabs();

        iconSearch.setOnClickListener(v -> Toast.makeText(this, "Mở tìm kiếm...", Toast.LENGTH_SHORT).show());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void initViews() {
        tabHome = findViewById(R.id.tabHome);
        tabFriend = findViewById(R.id.tabFriend);
        tabChat = findViewById(R.id.tabChat);
        tabLibrary = findViewById(R.id.tabLibrary);
        tabNotify = findViewById(R.id.tabNotify);
        tabProfile = findViewById(R.id.tabProfile);

        imgHome = findViewById(R.id.imgHome);
        imgFriend = findViewById(R.id.imgFriend);
        imgChat = findViewById(R.id.imgChat);
        imgLibrary = findViewById(R.id.imgLibrary);
        imgNotify = findViewById(R.id.imgNotify);
        imgProfile = findViewById(R.id.imgProfile);

        lineHome = findViewById(R.id.lineHome);
        lineFriend = findViewById(R.id.lineFriend);
        lineChat = findViewById(R.id.lineChat);
        lineLibrary = findViewById(R.id.lineLibrary);
        lineNotify = findViewById(R.id.lineNotify);
        lineProfile = findViewById(R.id.lineProfile);

        iconSearch = findViewById(R.id.iconSearch);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnOpenMenu = findViewById(R.id.btnOpenMenu);
        navigationView = findViewById(R.id.nav_view);
    }

    private void setupDrawer() {
        btnOpenMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Load thông tin user vào nav_header
        loadNavHeader();

        // CHỖ SỬA QUAN TRỌNG:
        // Vì btnNavLogout nằm trực tiếp trong NavigationView (trong FrameLayout cuối cùng),
        // chứ không nằm trong file header (nav_header), nên ta tìm trực tiếp từ navigationView.
        MaterialButton btnNavLogout = navigationView.findViewById(R.id.btnNavLogout);

        if (btnNavLogout != null) {
            btnNavLogout.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                performLogout();
            });
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);

            new Handler().postDelayed(() -> {
                Intent intent = null;
                if (id == R.id.nav_saved) intent = new Intent(this, SavedActivity.class);
                else if (id == R.id.nav_docs) intent = new Intent(this, DocsActivity.class);
                else if (id == R.id.nav_calendar) intent = new Intent(this, CalendarActivity.class);
                else if (id == R.id.nav_group) intent = new Intent(this, GroupActivity.class);
                else if (id == R.id.nav_meeting) intent = new Intent(this, MeetingActivity.class);
                else if (id == R.id.nav_quiz) intent = new Intent(this, QuizListActivity.class);
                else if (id == R.id.nav_pomodoro) intent = new Intent(this, PomodoroActivity.class);

                if (intent != null) {
                    startActivity(intent);
                }
            }, 150);

            return true;
        });
    }

    private void loadNavHeader() {
        // Lấy header view từ NavigationView
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        ImageView imgNavAvatar = headerView.findViewById(R.id.imgNavAvatar);
        TextView tvNavName = headerView.findViewById(R.id.tvNavName);

        // Gọi API lấy profile người dùng đang đăng nhập
        ApiService api = ApiClient.getApiService(this);
        api.getMyProfile().enqueue(new retrofit2.Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<User>> call,
                                   retrofit2.Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user == null) return;

                    // Hiển thị username
                    if (tvNavName != null && user.getUsername() != null) {
                        tvNavName.setText(user.getUsername());
                    }

                    // Load avatar bằng Glide
                    if (imgNavAvatar != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        Glide.with(HomeActivity.this)
                                .load(user.getAvatar())
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .centerCrop()
                                .into(imgNavAvatar);
                    }
                } else {
                    Log.w("HomeActivity", "Không lấy được thông tin user cho nav header");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<User>> call, Throwable t) {
                Log.e("HomeActivity", "Lỗi khi load nav header: " + t.getMessage());
            }
        });
    }

    private void setupBottomTabs() {
        // Mặc định chọn tab Home khi mới vào
        selectTab(imgHome, lineHome, new FeedFragment());

        tabHome.setOnClickListener(v -> selectTab(imgHome, lineHome, new FeedFragment()));
        tabFriend.setOnClickListener(v -> selectTab(imgFriend, lineFriend, new FriendFragment()));
        tabChat.setOnClickListener(v -> selectTab(imgChat, lineChat, new ChatFragment()));
        tabLibrary.setOnClickListener(v -> selectTab(imgLibrary, lineLibrary, new LibraryFragment()));
        tabNotify.setOnClickListener(v -> selectTab(imgNotify, lineNotify, new NotifyFragment()));
        tabProfile.setOnClickListener(v -> selectTab(imgProfile, lineProfile, new ProfileFragment()));
    }

    private void performLogout() {
        // Ngắt kết nối socket
        try {
            com.example.frontend.data.socket.ChatSocketManager.INSTANCE.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // SharedPreferences
        SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        // chỉ xóa session
        editor.remove("JWT_TOKEN");
        editor.remove("USER_ID");
        editor.putBoolean("IS_LOGGED_IN", false);

        editor.apply();

        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void selectTab(ImageView activeImg, View activeLine, Fragment fragment) {
        resetTabs();
        activeImg.setSelected(true);
        activeLine.setVisibility(View.VISIBLE);
        activeImg.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void resetTabs() {
        ImageView[] imgs = {imgHome, imgFriend, imgChat, imgLibrary, imgNotify, imgProfile};
        View[] lines = {lineHome, lineFriend, lineChat, lineLibrary, lineNotify, lineProfile};
        for (int i = 0; i < imgs.length; i++) {
            imgs[i].setSelected(false);
            imgs[i].setScaleX(1f);
            imgs[i].setScaleY(1f);
            lines[i].setVisibility(View.INVISIBLE);
        }
    }
}