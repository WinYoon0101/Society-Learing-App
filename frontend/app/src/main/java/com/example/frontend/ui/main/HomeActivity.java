package com.example.frontend.ui.main;

import android.annotation.SuppressLint;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Notification;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.ui.auth.LoginActivity;
import com.example.frontend.ui.calendar.CalendarActivity;
import com.example.frontend.ui.chat.ChatFragment;
import com.example.frontend.ui.docs.DocsActivity;
import com.example.frontend.ui.feed.FeedFragment;
import com.example.frontend.ui.feed.SavedActivity;
import com.example.frontend.ui.friend.FriendFragment;
import com.example.frontend.ui.group.GroupActivity;
import com.example.frontend.ui.library.LibraryFragment;
import com.example.frontend.ui.meeting.MeetingActivity;
import com.example.frontend.ui.notify.NotifyFragment;
import com.example.frontend.ui.pomodoro.PomodoroActivity;
import com.example.frontend.ui.profile.ProfileFragment;
import com.example.frontend.ui.quiz.QuizListActivity;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("UnsafeOptInUsageError")
public class HomeActivity extends AppCompatActivity {

    private LinearLayout tabHome, tabFriend, tabChat, tabLibrary, tabNotify, tabProfile;
    private ImageView imgHome, imgFriend, imgChat, imgLibrary, imgNotify, imgProfile;
    private View lineHome, lineFriend, lineChat, lineLibrary, lineNotify, lineProfile;
    private ImageView iconSearch, btnOpenMenu;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Biến quản lý chấm đỏ thông báo
    private BadgeDrawable notifyBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupDrawer();
        setupBottomTabs();

        // Khởi tạo hình ảnh chấm đỏ cho tab thông báo
        setupNotifyBadge();

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

    // Tự động kiểm tra thông báo mỗi khi người dùng mở lại màn hình Home
    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadNotifications();
    }

    private void setupNotifyBadge() {
        imgNotify.post(() -> {
            notifyBadge = BadgeDrawable.create(this);
            notifyBadge.setVisible(false); // Ban đầu ẩn đi
            BadgeUtils.attachBadgeDrawable(notifyBadge, imgNotify);
        });
    }

    // ĐÃ FIX: Đồng bộ chuẩn class ApiResponse<List<Notification>>
    private void checkUnreadNotifications() {
        ApiService api = ApiClient.getApiService(this);
        api.getNotifications().enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Notification>>> call,
                                   @NonNull Response<ApiResponse<List<Notification>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    int unreadCount = 0;
                    if (response.body().getData() != null) {
                        // Đếm những thông báo chưa được đọc (isRead = false)
                        for (Notification n : response.body().getData()) {
                            if (!n.isRead()) unreadCount++;
                        }
                    }

                    // Hiển thị số đếm lên UI
                    if (notifyBadge != null) {
                        if (unreadCount > 0) {
                            notifyBadge.setVisible(true);
                            notifyBadge.setNumber(unreadCount);
                        } else {
                            notifyBadge.setVisible(false);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Throwable t) {
                Log.e("HomeActivity", "Lỗi đếm chấm đỏ thông báo: " + t.getMessage());
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

        loadNavHeader();

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
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        ImageView imgNavAvatar = headerView.findViewById(R.id.imgNavAvatar);
        TextView tvNavName = headerView.findViewById(R.id.tvNavName);

        ApiService api = ApiClient.getApiService(this);
        api.getMyProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user == null) return;

                    if (tvNavName != null && user.getUsername() != null) {
                        tvNavName.setText(user.getUsername());
                    }

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
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                Log.e("HomeActivity", "Lỗi khi load nav header: " + t.getMessage());
            }
        });
    }

    private void setupBottomTabs() {
        selectTab(imgHome, lineHome, new FeedFragment());

        tabHome.setOnClickListener(v -> selectTab(imgHome, lineHome, new FeedFragment()));
        tabFriend.setOnClickListener(v -> selectTab(imgFriend, lineFriend, new FriendFragment()));
        tabChat.setOnClickListener(v -> selectTab(imgChat, lineChat, new ChatFragment()));
        tabLibrary.setOnClickListener(v -> selectTab(imgLibrary, lineLibrary, new LibraryFragment()));
        tabNotify.setOnClickListener(v -> selectTab(imgNotify, lineNotify, new NotifyFragment()));
        tabProfile.setOnClickListener(v -> selectTab(imgProfile, lineProfile, new ProfileFragment()));
    }

    private void performLogout() {
        try {
            com.example.frontend.data.socket.ChatSocketManager.INSTANCE.disconnect();
        } catch (Exception e) {
            Log.e("HomeActivity", "Lỗi disconnect socket: " + e.getMessage());
        }

        SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

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

        // Ẩn chấm đỏ ngay khi chuyển vào Tab Thông báo
        if (fragment instanceof NotifyFragment && notifyBadge != null) {
            notifyBadge.setVisible(false);
        }

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