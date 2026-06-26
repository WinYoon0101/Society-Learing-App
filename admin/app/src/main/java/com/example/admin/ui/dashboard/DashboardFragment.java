package com.example.admin.ui.dashboard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.admin.R;

public class DashboardFragment extends Fragment {

    private TextView tvTotalUsers, tvNewUsers, tvTotalPosts, tvPendingReports, tvInteractionStats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Ánh xạ View
        initViews(view);

        // Load dữ liệu hiển thị
        loadDashboardData();

        return view;
    }

    private void initViews(View view) {
        tvTotalUsers = view.findViewById(R.id.tv_total_users);
        tvNewUsers = view.findViewById(R.id.tv_new_users);
        tvTotalPosts = view.findViewById(R.id.tv_total_posts);
        tvPendingReports = view.findViewById(R.id.tv_pending_reports);
        tvInteractionStats = view.findViewById(R.id.tv_interaction_stats);
    }

    private void loadDashboardData() {
        // Giả lập độ trễ khi lấy dữ liệu từ server (1 giây)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // (Mock data)
            tvTotalUsers.setText("12,450");
            tvNewUsers.setText("+324");
            tvTotalPosts.setText("8,930");
            tvPendingReports.setText("42");

            tvInteractionStats.setText("👍 Like: 15.2K   |   💬 Comment: 3.4K   |   🔗 Share: 1.2K");

        }, 1000);
    }
}