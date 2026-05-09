package com.example.frontend.ui.live;

import android.app.ProgressDialog; // Dùng cái này để hiện loading
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.LiveModel;
import com.example.frontend.data.model.User;
import com.example.frontend.utils.Result; // Import class Result của ông

import java.util.ArrayList;
import java.util.List;

public class LiveStartActivity extends AppCompatActivity {

    private LiveViewModel viewModel;
    private LiveAdapter adapter;
    private List<LiveModel> liveList = new ArrayList<>();
    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_start);

        viewModel = new ViewModelProvider(this).get(LiveViewModel.class);
        viewModel.init(this);

        // Khởi tạo cái vòng xoay loading
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Đang thiết lập phòng live...");
        loadingDialog.setCancelable(false); // Không cho người dùng bấm thoát khi đang load

        setupRecyclerView();
        observeViewModel();

        viewModel.loadData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnStartLive).setOnClickListener(v -> {
            User user = viewModel.currentUser.getValue();
            if (user != null) {
                // THÊM System.currentTimeMillis() ĐỂ ID LUÔN DUY NHẤT
                String uniqueLiveId = "live_" + user.getId() + "_" + System.currentTimeMillis();
                viewModel.createLive(uniqueLiveId, "Live của " + user.getUsername());
            }
        });
    }

    private void observeViewModel() {
        // 1. Quan sát danh sách Live
        viewModel.liveListResult.observe(this, result -> {
            if (result == null) return;

            // Check bằng enum Status
            if (result.status == Result.Status.SUCCESS) {
                if (result.data != null) {
                    liveList.clear();
                    liveList.addAll(result.data);
                    adapter.notifyDataSetChanged();
                }
            } else if (result.status == Result.Status.ERROR) {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Quan sát trạng thái tạo Live
        viewModel.createLiveResult.observe(this, result -> {
            if (result == null) return;

            // Check theo Enum Status của ông
            switch (result.status) {
                case LOADING:
                    loadingDialog.show();
                    break;

                case SUCCESS:
                    loadingDialog.dismiss();
                    if (result.data != null) {
                        navigateToLive(true, result.data.getLiveId());
                    }
                    // Reset trạng thái
                    viewModel.createLiveResult.setValue(null);
                    break;

                case ERROR:
                    loadingDialog.dismiss();
                    Log.e("LIVE_ERR", "Lỗi tạo Live: " + result.message);

                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
                    viewModel.createLiveResult.setValue(null);
                    break;
            }
        });
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvActiveLives);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LiveAdapter(liveList, live -> navigateToLive(false, live.getLiveId()));
        rv.setAdapter(adapter);
    }

    private void navigateToLive(boolean isHost, String liveID) {
        User user = viewModel.currentUser.getValue();
        if (user == null) return;

        Intent intent = new Intent(this, LiveActivity.class);
        intent.putExtra("IS_HOST", isHost);
        intent.putExtra("USER_DATA", user);
        intent.putExtra("LIVE_ID", liveID);
        startActivity(intent);
    }
}