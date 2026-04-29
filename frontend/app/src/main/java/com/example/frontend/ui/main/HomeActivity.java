package com.example.frontend.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.frontend.R;
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

        // Xóa dữ liệu SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

        // Chuyển về màn hình đăng nhập và xóa sạch stack các activity cũ
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