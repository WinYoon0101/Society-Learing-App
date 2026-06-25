package com.example.frontend.ui.chat;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.UnreadCount;
import com.example.frontend.data.remote.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Badge "số tin nhắn chưa xem" cho tab Tin nhắn ở bottom nav.
 * Quy ước: 0 → ẩn; 1..10 → hiện số; >10 → dấu chấm đỏ (không số).
 */
public final class ChatUnreadBadge {

    private ChatUnreadBadge() {}

    private static final int DOT_THRESHOLD = 10;

    public static void apply(TextView badge, int unread) {
        if (badge == null) return;
        if (unread <= 0) {
            badge.setVisibility(View.GONE);
        } else if (unread <= DOT_THRESHOLD) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(String.valueOf(unread));
        } else {
            // > 10 → chấm đỏ (ô badge tròn, không hiển thị số)
            badge.setVisibility(View.VISIBLE);
            badge.setText("");
        }
    }

    /** Lấy tổng tin chưa xem từ server rồi cập nhật badge (no-op khi lỗi). */
    public static void refresh(Context context, TextView badge) {
        if (context == null || badge == null) return;
        ApiClient.getApiService(context).getUnreadCount()
                .enqueue(new Callback<ApiResponse<UnreadCount>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UnreadCount>> call,
                                           Response<ApiResponse<UnreadCount>> response) {
                        ApiResponse<UnreadCount> b = response.body();
                        if (response.isSuccessful() && b != null && b.isSuccess() && b.getData() != null) {
                            apply(badge, b.getData().getCount());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UnreadCount>> call, Throwable t) {
                        // Bỏ qua: giữ nguyên badge hiện tại
                    }
                });
    }
}
