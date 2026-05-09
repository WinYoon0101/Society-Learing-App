package com.example.frontend.ui.live;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback; // Import mới
import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.Constants;
import com.zegocloud.uikit.prebuilt.livestreaming.ZegoUIKitPrebuiltLiveStreamingConfig;
import com.zegocloud.uikit.prebuilt.livestreaming.ZegoUIKitPrebuiltLiveStreamingFragment;
import com.zegocloud.uikit.prebuilt.livestreaming.core.ZegoDialogInfo;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveActivity extends AppCompatActivity {

    private String currentLiveID;
    private boolean isHostUser;
    private boolean isEndedCalled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_container);

        User user = (User) getIntent().getSerializableExtra("USER_DATA");
        isHostUser = getIntent().getBooleanExtra("IS_HOST", false);
        currentLiveID = getIntent().getStringExtra("LIVE_ID");


        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d("LIVE_DEBUG", "User thực hiện back gesture hoặc bấm nút Back");
                handleExit();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        // ---------------------------------------------

        if (user != null && currentLiveID != null) {
            setupZegoUIKit(user.getId(), user.getUsername(), currentLiveID, isHostUser);
        } else {
            Toast.makeText(this, "Lỗi dữ liệu Live!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupZegoUIKit(String userID, String userName, String liveID, boolean isHost) {
        ZegoUIKitPrebuiltLiveStreamingConfig config;
        if (isHost) {
            config = ZegoUIKitPrebuiltLiveStreamingConfig.host(true);
        } else {
            config = ZegoUIKitPrebuiltLiveStreamingConfig.audience(true);
        }

        // Bắt sự kiện khi nhấn nút thoát (X) trên giao diện
        config.leaveLiveStreamingListener = () -> {
            Log.d("LIVE_DEBUG", "Thoát bằng nút Zego");
            handleExit();
        };

        config.translationText.startLiveStreamingButton = "Bắt đầu phát ngay";
        config.translationText.noHostOnline = "Chủ phòng hiện đang ngoại tuyến";

        if (isHost) {
            config.confirmDialogInfo = new ZegoDialogInfo();
            config.confirmDialogInfo.title = "Kết thúc Livestream";
            config.confirmDialogInfo.message = "Bạn có chắc chắn muốn dừng buổi phát trực tiếp này không?";
            config.confirmDialogInfo.cancelButtonName = "Hủy";
            config.confirmDialogInfo.confirmButtonName = "Xác nhận";
        }

        ZegoUIKitPrebuiltLiveStreamingFragment fragment = ZegoUIKitPrebuiltLiveStreamingFragment.newInstance(
                Constants.ZEGO_APP_ID, Constants.ZEGO_APP_SIGN, userID, userName, liveID, config
        );

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitNow();
    }

    /**
     * Hàm xử lý thoát chung cho cả Back gesture và nút UI
     */
    private void handleExit() {
        if (isHostUser) {
            notifyBackendLiveEnded(currentLiveID);
        } else {
            finish();
        }
    }

    private void notifyBackendLiveEnded(String liveID) {
        Log.d("LIVE_DEBUG", "Gửi ID lên Server: [" + liveID + "]");

        if (isEndedCalled) return;
        isEndedCalled = true;

        Log.d("LIVE_DEBUG", "Đang gửi API endLive...");

        ApiService api = ApiClient.getApiService(this);
        api.endLive(liveID).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Log.d("LIVE_DEBUG", "Server đã phản hồi!");
                if (response.isSuccessful()) {
                    Log.d("LIVE_DEBUG", "Status đã cập nhật!");
                }
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("LIVE_DEBUG", "Lỗi: " + t.getMessage());
                finish();
            }
        });


    }


}