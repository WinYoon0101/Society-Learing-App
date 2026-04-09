package com.example.frontend.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.frontend.R;
import com.example.frontend.ui.auth.LoginActivity;
import com.example.frontend.ui.feed.FeedFragment;
import com.example.frontend.ui.friend.FriendFragment;
import com.example.frontend.ui.chat.ChatFragment;
import com.example.frontend.ui.library.LibraryFragment;
import com.example.frontend.ui.notify.NotifyFragment;
import com.example.frontend.ui.profile.ProfileFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout tabHome, tabFriend, tabChat, tabLibrary, tabNotify, tabProfile;
    private ImageView imgHome, imgFriend, imgChat, imgLibrary, imgNotify, imgProfile;
    private View lineHome, lineFriend, lineChat, lineLibrary, lineNotify, lineProfile;
    private ImageView iconSearch;

    private DrawerLayout drawerLayout;
    private ImageView btnOpenMenu;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Bind view
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



        // 2. Bind view mới cho Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        btnOpenMenu = findViewById(R.id.btnOpenMenu);
        navigationView = findViewById(R.id.nav_view);

        // 3. Sự kiện mở Drawer (Nhấn nút 3 gạch)
        btnOpenMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Xử lý nút Đăng xuất trong Drawer (Nằm dưới đáy)
        // nằm trong NavigationView nên phải dùng navigationView.findViewById
        MaterialButton btnNavLogout = navigationView.findViewById(R.id.btnNavLogout);
        btnNavLogout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START); // Đóng menu trước
            performLogout(); // Gọi hàm đăng xuất
        });

        // 2. Default tab
        selectTab(imgHome, lineHome, new FeedFragment());

        // 3. Click events
        tabHome.setOnClickListener(v -> selectTab(imgHome, lineHome, new FeedFragment()));
        tabFriend.setOnClickListener(v -> selectTab(imgFriend, lineFriend, new FriendFragment()));
        tabChat.setOnClickListener(v -> selectTab(imgChat, lineChat, new ChatFragment()));
        tabLibrary.setOnClickListener(v -> selectTab(imgLibrary, lineLibrary, new LibraryFragment()));
        tabNotify.setOnClickListener(v -> selectTab(imgNotify, lineNotify, new NotifyFragment()));
        tabProfile.setOnClickListener(v -> selectTab(imgProfile, lineProfile, new ProfileFragment()));

        // 4. Search
        iconSearch.setOnClickListener(v ->
                Toast.makeText(this, "Mở tìm kiếm...", Toast.LENGTH_SHORT).show()
        );


    }


    // Hàm thực hiện đăng xuất
    private void performLogout() {
        SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // 👉 Chọn tab
    private void selectTab(ImageView activeImg, View activeLine, Fragment fragment) {
        resetTabs();

        activeImg.setSelected(true);
        activeLine.setVisibility(View.VISIBLE);

        // optional animation (xịn hơn)
        activeImg.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // 👉 Reset tab
    private void resetTabs() {
        imgHome.setSelected(false);
        imgFriend.setSelected(false);
        imgChat.setSelected(false);
        imgLibrary.setSelected(false);
        imgNotify.setSelected(false);
        imgProfile.setSelected(false);

        lineHome.setVisibility(View.INVISIBLE);
        lineFriend.setVisibility(View.INVISIBLE);
        lineChat.setVisibility(View.INVISIBLE);
        lineLibrary.setVisibility(View.INVISIBLE);
        lineNotify.setVisibility(View.INVISIBLE);
        lineProfile.setVisibility(View.INVISIBLE);

        // reset scale (nếu có animation)
        imgHome.setScaleX(1f); imgHome.setScaleY(1f);
        imgFriend.setScaleX(1f); imgFriend.setScaleY(1f);
        imgChat.setScaleX(1f); imgChat.setScaleY(1f);
        imgLibrary.setScaleX(1f); imgLibrary.setScaleY(1f);
        imgNotify.setScaleX(1f); imgNotify.setScaleY(1f);
        imgProfile.setScaleX(1f); imgProfile.setScaleY(1f);
    }
}