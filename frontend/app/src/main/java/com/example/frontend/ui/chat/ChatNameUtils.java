package com.example.frontend.ui.chat;

import com.example.frontend.data.model.User;

import java.util.ArrayList;
import java.util.List;

/** Tiện ích tính tên hiển thị cho group chat (dùng chung list + header). */
public final class ChatNameUtils {

    private ChatNameUtils() {}

    private static final int MAX_NAMES = 3;

    /**
     * Tên group: ưu tiên {@code name} thủ công; rỗng → ghép tên thành viên (trừ mình),
     * tối đa 3 tên, dư thì "+N". Vd "An, Bình, Cường +2".
     */
    public static String groupDisplayName(String name, List<User> members, String currentUserId) {
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (members == null || members.isEmpty()) {
            return "Nhóm";
        }
        List<String> names = new ArrayList<>();
        for (User m : members) {
            if (m == null) continue;
            if (m.getId() != null && m.getId().equals(currentUserId)) continue;
            String u = m.getUsername();
            names.add(u != null ? u : "Ẩn danh");
        }
        if (names.isEmpty()) {
            return "Nhóm";
        }
        int show = Math.min(MAX_NAMES, names.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < show; i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        if (names.size() > show) {
            sb.append(" +").append(names.size() - show);
        }
        return sb.toString();
    }
}
