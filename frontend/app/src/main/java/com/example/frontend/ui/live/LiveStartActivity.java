package com.example.frontend.ui.live;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.frontend.R;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.model.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveStartActivity extends AppCompatActivity {

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_start);

        // Lấy thông tin User thật từ API trước khi bắt đầu
        fetchProfile();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnStartLive).setOnClickListener(v -> {
            if (currentUser != null) {
                navigateToLive(true);
            }
        });

        findViewById(R.id.btnWatchLive).setOnClickListener(v -> {
            if (currentUser != null) {
                navigateToLive(false);
            }
        });
    }

    private void fetchProfile() {
        ApiService api = ApiClient.getApiService(this);
        api.getMyProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body().getData();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Toast.makeText(LiveStartActivity.this, "Không thể lấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToLive(boolean isHost) {
        Intent intent = new Intent(this, LiveActivity.class);
        intent.putExtra("IS_HOST", isHost);
        // Truyền Object User sang LiveActivity
        intent.putExtra("USER_DATA", currentUser);
        // ID phòng: Host dùng ID của mình, Audience nhập ID
        intent.putExtra("LIVE_ID", isHost ? "live_" + currentUser.getId() : "live_test_room");
        startActivity(intent);
    }
}