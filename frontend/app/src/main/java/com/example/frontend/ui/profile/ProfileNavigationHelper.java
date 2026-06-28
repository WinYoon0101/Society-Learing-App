package com.example.frontend.ui.profile;

import android.content.Context;
import android.content.Intent;

import com.example.frontend.ui.main.HomeActivity;

public final class ProfileNavigationHelper {

    private ProfileNavigationHelper() {}

    public static void openProfile(Context context, String userId, String userName, String avatar) {
        if (context == null || userId == null || userId.trim().isEmpty()) return;

        String myUserId = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                .getString("USER_ID", "");

        if (userId.equals(myUserId)) {
            if (context instanceof HomeActivity) {
                ((HomeActivity) context).openProfileTab();
            } else {
                Intent intent = new Intent(context, HomeActivity.class);
                intent.putExtra("SELECT_TAB", 5);
                context.startActivity(intent);
            }
            return;
        }

        Intent intent = new Intent(context, FriendProfileActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("FRIEND_ID", userId);
        intent.putExtra("FRIEND_NAME", userName);
        intent.putExtra("FRIEND_AVATAR", avatar);
        context.startActivity(intent);
    }
}
