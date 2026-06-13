package com.example.frontend.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.example.frontend.ui.friend.FriendFragment;
import com.example.frontend.ui.group.GroupActivity;
import com.example.frontend.ui.library.LibraryFragment;
import com.example.frontend.ui.meeting.MeetingActivity;
import com.example.frontend.ui.notify.NotifyFragment;
import com.example.frontend.ui.profile.ProfileFragment;
import com.example.frontend.ui.saved.SavedActivity;
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
    private BadgeDrawable notifyBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupDrawer();
        setupBottomTabs();
        setupNotifyBadge();

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

    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadNotifications();
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
            Intent intent = null;
            if (id == R.id.nav_saved) intent = new Intent(this, SavedActivity.class);
            else if (id == R.id.nav_docs) intent = new Intent(this, DocsActivity.class);
            else if (id == R.id.nav_calendar) intent = new Intent(this, CalendarActivity.class);
            else if (id == R.id.nav_group) intent = new Intent(this, GroupActivity.class);
            else if (id == R.id.nav_meeting) intent = new Intent(this, MeetingActivity.class);

            if (intent != null) startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        ImageView imgNavAvatar = headerView.findViewById(R.id.imgNavAvatar);
        TextView tvNavName = headerView.findViewById(R.id.tvNavName);

        ApiService api = ApiClient.getApiService(this);
        api.getMyProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call, @NonNull Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    User user = response.body().getData();
                    tvNavName.setText(user.getUsername());
                    Glide.with(HomeActivity.this).load(user.getAvatar()).placeholder(R.drawable.ic_profile).into(imgNavAvatar);
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) { Log.e("HomeActivity", "Lỗi load header"); }
        });
    }

    private void setupNotifyBadge() {
        imgNotify.post(() -> {
            notifyBadge = BadgeDrawable.create(this);
            notifyBadge.setVisible(false);
            BadgeUtils.attachBadgeDrawable(notifyBadge, imgNotify);
        });
    }

    private void checkUnreadNotifications() {
        ApiService api = ApiClient.getApiService(this);
        api.getNotifications().enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Response<ApiResponse<List<Notification>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    int unreadCount = 0;
                    if (response.body().getData() != null) {
                        for (Notification n : response.body().getData()) if (!n.isRead()) unreadCount++;
                    }
                    if (notifyBadge != null) {
                        notifyBadge.setVisible(unreadCount > 0);
                        if (unreadCount > 0) notifyBadge.setNumber(unreadCount);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Throwable t) { }
        });
    }

    private void setupBottomTabs() {
        tabHome.setOnClickListener(v -> selectTab(imgHome, lineHome, new FeedFragment()));
        tabFriend.setOnClickListener(v -> selectTab(imgFriend, lineFriend, new FriendFragment()));
        tabChat.setOnClickListener(v -> selectTab(imgChat, lineChat, new ChatFragment()));
        tabLibrary.setOnClickListener(v -> selectTab(imgLibrary, lineLibrary, new LibraryFragment()));
        tabNotify.setOnClickListener(v -> selectTab(imgNotify, lineNotify, new NotifyFragment()));
        tabProfile.setOnClickListener(v -> selectTab(imgProfile, lineProfile, new ProfileFragment()));
        selectTab(imgHome, lineHome, new FeedFragment());
    }

    private void selectTab(ImageView activeImg, View activeLine, Fragment fragment) {
        resetTabs();
        activeImg.setSelected(true);
        activeLine.setVisibility(View.VISIBLE);
        if (fragment instanceof NotifyFragment && notifyBadge != null) notifyBadge.setVisible(false);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void resetTabs() {
        imgHome.setSelected(false); imgFriend.setSelected(false); imgChat.setSelected(false);
        imgLibrary.setSelected(false); imgNotify.setSelected(false); imgProfile.setSelected(false);
        lineHome.setVisibility(View.INVISIBLE); lineFriend.setVisibility(View.INVISIBLE);
        lineChat.setVisibility(View.INVISIBLE); lineLibrary.setVisibility(View.INVISIBLE);
        lineNotify.setVisibility(View.INVISIBLE); lineProfile.setVisibility(View.INVISIBLE);
    }

    private void performLogout() {
        SharedPreferences pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        pref.edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}