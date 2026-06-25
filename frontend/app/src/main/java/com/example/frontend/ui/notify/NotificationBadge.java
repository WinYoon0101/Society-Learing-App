package com.example.frontend.ui.notify;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.example.frontend.data.model.NotificationListResponse;
import com.example.frontend.data.remote.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tiện ích badge "số thông báo chưa đọc" dùng chung cho chuông ở nhiều màn (tab Nhóm, Home...).
 * - {@link #refresh} gọi API lấy unreadCount rồi cập nhật badge TextView.
 * - {@link #apply} chỉ cập nhật giao diện badge theo số đã biết.
 */
public final class NotificationBadge {

    private NotificationBadge() {}

    public static void apply(TextView badge, int unread) {
        if (badge == null) return;
        if (unread <= 0) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.VISIBLE);
            badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        }
    }

    /** Lấy số thông báo chưa đọc từ server rồi cập nhật badge (no-op khi lỗi). */
    public static void refresh(Context context, TextView badge) {
        if (context == null || badge == null) return;
        ApiClient.getApiService(context).getNotifications(1, 1)
                .enqueue(new Callback<NotificationListResponse>() {
                    @Override
                    public void onResponse(Call<NotificationListResponse> call,
                                           Response<NotificationListResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            apply(badge, response.body().getUnreadCount());
                        }
                    }
                    @Override
                    public void onFailure(Call<NotificationListResponse> call, Throwable t) {
                        // Bỏ qua: giữ nguyên badge hiện tại
                    }
                });
    }
}
