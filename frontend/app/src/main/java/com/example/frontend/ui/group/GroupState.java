package com.example.frontend.ui.group;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * State chia sẻ giữa các tab Nhóm (process-scoped).
 *
 * - Cờ "dirty": đánh dấu tab nào cần reload sau một hành động (join / đăng bài / rời nhóm).
 *   Mỗi fragment kiểm tra cờ tương ứng trong onResume rồi tự reload (tránh reload thừa).
 * - "Không quan tâm": lưu local các groupId mà user không muốn được gợi ý ở tab Khám phá.
 */
public final class GroupState {

    private GroupState() {}

    // ── Dirty flags ──────────────────────────────────────────────
    public static volatile boolean feedDirty = false;
    public static volatile boolean discoverDirty = false;
    public static volatile boolean myGroupsDirty = false;

    /** Sau khi tham gia nhóm: feed, khám phá và "nhóm của bạn" đều cần cập nhật. */
    public static void onJoinedGroup() {
        feedDirty = true;
        discoverDirty = true;
        myGroupsDirty = true;
    }

    /** Sau khi đăng bài trong nhóm: feed cần cập nhật. */
    public static void onCreatedPost() {
        feedDirty = true;
    }

    /** Sau khi rời / xóa nhóm: cập nhật tất cả. */
    public static void onLeftOrDeletedGroup() {
        feedDirty = true;
        discoverDirty = true;
        myGroupsDirty = true;
    }

    // ── "Không quan tâm" (local) ─────────────────────────────────
    private static final String PREFS = "MyAppPrefs";
    private static final String KEY_NOT_INTERESTED = "GROUP_NOT_INTERESTED";

    public static Set<String> getNotInterested(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // Trả về bản sao — không sửa trực tiếp set do SharedPreferences quản lý.
        return new HashSet<>(p.getStringSet(KEY_NOT_INTERESTED, new HashSet<>()));
    }

    public static void addNotInterested(Context c, String groupId) {
        if (groupId == null) return;
        Set<String> set = getNotInterested(c);
        set.add(groupId);
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_NOT_INTERESTED, set)
                .apply();
        discoverDirty = true;
    }

    // ── Badge "bài viết mới" — đánh dấu đã xem (local) ───────────
    // Lưu mốc lastUpdated mà user đã xem cho từng nhóm. Badge chỉ hiện lại
    // khi nhóm có hoạt động mới hơn (lastUpdated khác mốc đã lưu).
    private static final String KEY_SEEN_PREFIX = "GROUP_SEEN_LU_";

    public static void markGroupSeen(Context c, String groupId, String lastUpdated) {
        if (groupId == null) return;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SEEN_PREFIX + groupId, lastUpdated == null ? "" : lastUpdated)
                .apply();
        myGroupsDirty = true;
    }

    /** true nếu user đã xem đúng mốc hoạt động mới nhất hiện tại của nhóm. */
    public static boolean isGroupSeen(Context c, String groupId, String lastUpdated) {
        if (groupId == null) return false;
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = KEY_SEEN_PREFIX + groupId;
        if (!p.contains(key)) return false;
        String cur = lastUpdated == null ? "" : lastUpdated;
        return cur.equals(p.getString(key, null));
    }

    // ── Mức nhận thông báo của nhóm (local, G-B4) ────────────────
    // Hiện chỉ lưu lựa chọn (chưa lọc thông báo in-app vì noti là list chung).
    private static final String KEY_NOTIF_PREFIX = "GROUP_NOTIF_";
    public static final String NOTIF_ALL = "all";          // Tất cả bài viết
    public static final String NOTIF_HIGHLIGHT = "highlight"; // Chỉ nổi bật
    public static final String NOTIF_OFF = "off";          // Tắt thông báo

    public static String getGroupNotifLevel(Context c, String groupId) {
        if (groupId == null) return NOTIF_ALL;
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_NOTIF_PREFIX + groupId, NOTIF_ALL);
    }

    public static void setGroupNotifLevel(Context c, String groupId, String level) {
        if (groupId == null) return;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NOTIF_PREFIX + groupId, level)
                .apply();
    }
}
