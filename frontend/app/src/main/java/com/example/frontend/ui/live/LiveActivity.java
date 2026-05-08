package com.example.frontend.ui.live;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.frontend.R;
import com.example.frontend.data.model.User;
import com.example.frontend.utils.Constants;
import com.zegocloud.uikit.prebuilt.livestreaming.ZegoUIKitPrebuiltLiveStreamingConfig;
import com.zegocloud.uikit.prebuilt.livestreaming.ZegoUIKitPrebuiltLiveStreamingFragment;
import com.zegocloud.uikit.prebuilt.livestreaming.core.ZegoDialogInfo;

public class LiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_container);

        // Nhận data thật từ Intent
        User user = (User) getIntent().getSerializableExtra("USER_DATA");
        boolean isHost = getIntent().getBooleanExtra("IS_HOST", false);
        String liveID = getIntent().getStringExtra("LIVE_ID");

        if (user != null) {
            setupZegoUIKit(user.getId(), user.getUsername(), liveID, isHost);
        }
    }

    private void setupZegoUIKit(String userID, String userName, String liveID, boolean isHost) {
        ZegoUIKitPrebuiltLiveStreamingConfig config;
        if (isHost) {
            config = ZegoUIKitPrebuiltLiveStreamingConfig.host(true);
        } else {
            config = ZegoUIKitPrebuiltLiveStreamingConfig.audience(true);
        }


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
                Constants.ZEGO_APP_ID,
                Constants.ZEGO_APP_SIGN,
                userID,
                userName,
                liveID,
                config
        );

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitNow();
    }
}