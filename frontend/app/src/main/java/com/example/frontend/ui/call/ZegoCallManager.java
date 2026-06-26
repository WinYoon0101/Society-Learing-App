package com.example.frontend.ui.call;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.Constants;
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService;
import com.zegocloud.uikit.prebuilt.call.core.CallInvitationServiceImpl;
import com.zegocloud.uikit.prebuilt.call.core.invite.ZegoCallInvitationData;
import com.zegocloud.uikit.prebuilt.call.event.ZegoCallEndReason;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig;
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoCallType;
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoCallUser;
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoInvitationCallListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * G7 — Bọc vòng đời ZegoUIKitPrebuiltCallService (Call Kit 1-1 + group).
 *
 * <p>Zego cloud lo signaling + media + UI cuộc gọi. App chỉ cần init sau khi login
 * (userID = Mongo _id, userName = username) và unInit khi logout.
 *
 * <p>Init idempotent: gọi nhiều lần (vd HomeActivity.onCreate mỗi lần mở) chỉ init 1 lần
 * cho tới khi {@link #unInit()}.
 *
 * <p>G7.4 — Ghi cuộc gọi vào chat: chỉ phía <b>caller</b> log (tránh trùng). Caller bấm gọi →
 * {@link #onCallInitiated} lưu context; {@code onOutgoingCallAccepted} đánh dấu đã kết nối;
 * {@code onCallEnd} → POST call-log "Cuộc gọi ... · m:ss"; timeout/declined → "... nhỡ".
 * Callee không log vì {@code connectedAtMs} chỉ set bởi sự kiện outgoing.
 */
public final class ZegoCallManager {

    private static final String TAG = "ZegoCallManager";

    private static boolean initialized = false;
    private static boolean eventsWired = false;
    private static String currentUserId = null;
    private static Application appRef = null;

    // ── Call context (chỉ ý nghĩa ở phía caller) ──
    private static String pendingConvId = null;
    private static boolean pendingIsVideo = false;
    private static long connectedAtMs = 0L; // >0 nghĩa là cuộc gọi đã được nhận

    // ── G7.5 — conversation đã TẮT thông báo cuộc gọi (chặn hiện cuộc gọi đến) ──
    private static final Set<String> mutedCallConvs = new HashSet<>();

    private ZegoCallManager() {}

    /**
     * Khởi tạo Call Kit cho user đang đăng nhập. An toàn khi gọi lặp lại.
     *
     * @param application application context (giữ sống xuyên suốt app)
     * @param userId      Mongo _id của user hiện tại (làm Zego userID)
     * @param userName    tên hiển thị khi gọi
     */
    public static void init(Application application, String userId, String userName) {
        if (application == null || TextUtils.isEmpty(userId)) {
            Log.w(TAG, "Bỏ qua init Call Kit: thiếu application hoặc userId");
            return;
        }
        appRef = application;
        // Nếu đã init cho đúng user này rồi thì thôi.
        if (initialized && userId.equals(currentUserId)) {
            return;
        }
        // Đổi user (vd login tài khoản khác mà chưa logout) → reset trước.
        if (initialized) {
            unInit();
        }

        String displayName = TextUtils.isEmpty(userName) ? "User" : userName;

        // Signaling plugin (ZIM) được Call Kit tự wire qua dependency — không truyền thủ công.
        ZegoUIKitPrebuiltCallInvitationConfig config = new ZegoUIKitPrebuiltCallInvitationConfig();

        ZegoUIKitPrebuiltCallService.init(
                application,
                Constants.ZEGO_APP_ID,
                Constants.ZEGO_APP_SIGN,
                userId,
                displayName,
                config);

        initialized = true;
        currentUserId = userId;
        wireCallEvents();
        refreshMutedCalls();
        Log.d(TAG, "Call Kit initialized cho userId=" + userId);
    }

    /** Gỡ Call Kit (gọi khi logout). An toàn khi chưa init. */
    public static void unInit() {
        if (!initialized) return;
        try {
            ZegoUIKitPrebuiltCallService.unInit();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi unInit Call Kit: " + e.getMessage());
        }
        initialized = false;
        resetCallContext();
        currentUserId = null;
        Log.d(TAG, "Call Kit uninitialized");
    }

    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Caller gọi khi bấm nút gọi (trước khi Zego gửi invitation). Lưu conversation + loại gọi
     * để khi cuộc gọi kết thúc / nhỡ thì ghi call-log đúng đoạn chat.
     */
    public static void onCallInitiated(String conversationId, boolean isVideo) {
        pendingConvId = conversationId;
        pendingIsVideo = isVideo;
        connectedAtMs = 0L;
    }

    private static void resetCallContext() {
        pendingConvId = null;
        pendingIsVideo = false;
        connectedAtMs = 0L;
    }

    // ─────────────────────── Wire Zego call events (1 lần) ───────────────────────

    private static void wireCallEvents() {
        if (eventsWired) return;
        if (ZegoUIKitPrebuiltCallService.events == null) return;

        // Kết thúc cuộc gọi đã kết nối → ghi thời lượng (chỉ caller, vì connectedAtMs do outgoing set).
        ZegoUIKitPrebuiltCallService.events.callEvents.setCallEndListener(
                (ZegoCallEndReason reason, String jsonObject) -> {
                    if (pendingConvId != null) {
                        if (connectedAtMs > 0) {
                            long durationSec = (System.currentTimeMillis() - connectedAtMs) / 1000L;
                            postCallLog(pendingConvId, pendingIsVideo, "ended", durationSec);
                        } else {
                            // Caller cúp khi chưa được nhận → coi như cuộc gọi nhỡ.
                            postCallLog(pendingConvId, pendingIsVideo, "missed", 0L);
                        }
                    }
                    resetCallContext();
                });

        // Sự kiện invitation (caller-side dùng để biết đã kết nối / nhỡ).
        ZegoUIKitPrebuiltCallService.events.invitationEvents.setInvitationListener(
                new ZegoInvitationCallListener() {
                    @Override
                    public void onOutgoingCallAccepted(String callID, ZegoCallUser callee) {
                        if (connectedAtMs == 0L) connectedAtMs = System.currentTimeMillis();
                    }

                    @Override
                    public void onOutgoingCallTimeout(String callID, List<ZegoCallUser> callees) {
                        logMissedAndReset();
                    }

                    @Override
                    public void onOutgoingCallDeclined(String callID, ZegoCallUser callee) {
                        logMissedAndReset();
                    }

                    @Override
                    public void onOutgoingCallRejectedCauseBusy(String callID, ZegoCallUser callee) {
                        logMissedAndReset();
                    }

                    // Callee-side: KHÔNG log call ở đây (caller đã log).
                    // G7.5 — nếu đã tắt thông báo cuộc gọi cho conversation này → không reo, không hiện.
                    @Override
                    public void onIncomingCallReceived(String callID, ZegoCallUser caller,
                                                       ZegoCallType type, List<ZegoCallUser> callees) {
                        suppressIfMuted();
                    }

                    @Override
                    public void onIncomingCallCanceled(String callID, ZegoCallUser caller) { }

                    @Override
                    public void onIncomingCallTimeout(String callID, ZegoCallUser caller) { }
                });

        // A bấm "Huỷ" lúc đang đổ chuông (chưa kết nối) → callEnd KHÔNG bắn (chưa vào room),
        // nên dùng listener riêng này để log "Cuộc gọi nhỡ".
        ZegoUIKitPrebuiltCallService.events.invitationEvents.setOutgoingCallButtonListener(
                ZegoCallManager::logMissedAndReset);

        eventsWired = true;
    }

    // ─────────────────────── G7.5 — Tôn trọng mutedCalls ───────────────────────

    /**
     * Nếu cuộc gọi đến thuộc conversation đã tắt thông báo cuộc gọi → tắt chuông + ẩn màn hình
     * cuộc gọi đến (callee không bị làm phiền). conversationId lấy từ customData của invitation.
     */
    private static void suppressIfMuted() {
        // Chỉ ẩn form cuộc gọi đến phía mình — KHÔNG reject, để caller vẫn nghe chuông chờ và
        // cuộc gọi timeout/cúp tự nhiên (→ log "nhỡ"). doSuppress tự kiểm tra muted nên gọi dư vô hại.
        // Thử nhiều nhịp vì form có thể hiện trễ sau callback (customData/dialog chưa sẵn ngay).
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        doSuppress();
        h.postDelayed(ZegoCallManager::doSuppress, 400);
        h.postDelayed(ZegoCallManager::doSuppress, 900);
    }

    /** conversationId của cuộc gọi đến (customData) có nằm trong tập đã-mute không. */
    private static boolean isMutedIncoming() {
        try {
            if (mutedCallConvs.isEmpty()) return false;
            ZegoCallInvitationData data = CallInvitationServiceImpl.getInstance().getCallInvitationData();
            String convId = (data != null) ? data.customData : null;
            return convId != null && mutedCallConvs.contains(convId);
        } catch (Exception e) {
            return false;
        }
    }

    private static void doSuppress() {
        if (!isMutedIncoming()) return;
        try {
            // CHỈ ẩn form + tắt chuông phía mình (idempotent, an toàn khi gọi lặp). KHÔNG reject
            // để caller vẫn nghe chuông chờ → cuộc gọi tự timeout (caller log "nhỡ").
            CallInvitationServiceImpl.getInstance().stopRingTone();
            CallInvitationServiceImpl.getInstance().hideIncomingCallDialog();
            Log.d(TAG, "Ẩn cuộc gọi đến (đã mute)");
        } catch (Exception e) {
            Log.e(TAG, "doSuppress error: " + e.getMessage());
        }
    }

    /** Tải lại danh sách conversation đã tắt thông báo cuộc gọi từ BE (gọi lúc init + sau khi đổi mute). */
    public static void refreshMutedCalls() {
        if (appRef == null) return;
        ApiService api = ApiClient.getApiService(appRef);
        api.getMutedCalls().enqueue(new Callback<ApiResponse<List<String>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<String>>> call,
                                   @NonNull Response<ApiResponse<List<String>>> response) {
                ApiResponse<List<String>> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess() && b.getData() != null) {
                    mutedCallConvs.clear();
                    mutedCallConvs.addAll(b.getData());
                    Log.d(TAG, "muted-calls refreshed: " + mutedCallConvs.size());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<String>>> call, @NonNull Throwable t) {
                Log.e(TAG, "refreshMutedCalls failed: " + t.getMessage());
            }
        });
    }

    /** Cập nhật tức thì khi user bật/tắt mute cuộc gọi trong 1 conversation (khỏi chờ refresh). */
    public static void setMutedCallConversation(String conversationId, boolean muted) {
        if (TextUtils.isEmpty(conversationId)) return;
        if (muted) mutedCallConvs.add(conversationId);
        else mutedCallConvs.remove(conversationId);
    }

    private static void logMissedAndReset() {
        if (connectedAtMs == 0L && pendingConvId != null) {
            postCallLog(pendingConvId, pendingIsVideo, "missed", 0L);
        }
        resetCallContext();
    }

    // ─────────────────────── POST call-log → system message ───────────────────────

    private static void postCallLog(String conversationId, boolean isVideo,
                                    String status, long durationSec) {
        if (appRef == null || TextUtils.isEmpty(conversationId)) return;
        Map<String, Object> body = new HashMap<>();
        body.put("callType", isVideo ? "video" : "audio");
        body.put("status", status);
        body.put("duration", durationSec);

        ApiService api = ApiClient.getApiService(appRef);
        api.callLog(conversationId, body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                Log.d(TAG, "call-log posted: " + status + " " + durationSec + "s");
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "call-log failed: " + t.getMessage());
            }
        });
    }
}
