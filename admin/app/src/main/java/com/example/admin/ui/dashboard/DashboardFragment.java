package com.example.admin.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.admin.R;
import com.example.admin.data.model.DashboardResponse;
import com.example.admin.data.model.DashboardStats;
import com.example.admin.data.remote.RetrofitClient;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private TextView tvTotalUsers, tvNewUsers, tvTotalPosts, tvPendingReports;
    private PieChart pieChartInteractions;
    private LineChart lineChartGrowth;
    private BarChart barChartReactions;

    // 1. Tạo Formatter để xóa số thập phân (.00)
    private final ValueFormatter intFormatter = new ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {
            return String.valueOf((int) value);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        initViews(view);
        fetchDashboardData();
        return view;
    }

    private void initViews(View view) {
        tvTotalUsers = view.findViewById(R.id.tv_total_users);
        tvNewUsers = view.findViewById(R.id.tv_new_users);
        tvTotalPosts = view.findViewById(R.id.tv_total_posts);
        tvPendingReports = view.findViewById(R.id.tv_pending_reports);

        pieChartInteractions = view.findViewById(R.id.pieChartInteractions);
        lineChartGrowth = view.findViewById(R.id.lineChartGrowth);
        barChartReactions = view.findViewById(R.id.barChartReactions);
    }

    private void fetchDashboardData() {
        tvTotalUsers.setText("...");

        RetrofitClient.getApi().getDashboardStats().enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    DashboardStats stats = response.body().data;

                    if (stats.overview != null) {
                        tvTotalUsers.setText(String.valueOf(stats.overview.totalUsers));
                        tvNewUsers.setText(String.valueOf(stats.overview.newUsersToday));
                        tvTotalPosts.setText(String.valueOf(stats.overview.totalPosts));
                        tvPendingReports.setText(String.valueOf(stats.overview.pendingReports));
                    }

                    if (stats.interactionsPieChart != null) {
                        setupPieChart(stats.interactionsPieChart);
                    }

                    if (stats.growth7DaysChart != null && !stats.growth7DaysChart.isEmpty()) {
                        setupLineChart(stats.growth7DaysChart);
                    }

                    if (stats.reactionBarChart != null) {
                        setupBarChart(stats.reactionBarChart);
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi lấy dữ liệu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                Log.e("API_ERROR", "Lỗi kết nối", t);
                Toast.makeText(getContext(), "Không thể kết nối đến server!", Toast.LENGTH_LONG).show();
                tvTotalUsers.setText("Lỗi");
            }
        });
    }

    // ==========================================
    // 1. BIỂU ĐỒ TRÒN
    // ==========================================
    private void setupPieChart(DashboardStats.InteractionsPieChart data) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        // Việt hóa Text
        if (data.reactions > 0) entries.add(new PieEntry(data.reactions, "Cảm xúc"));
        if (data.comments > 0) entries.add(new PieEntry(data.comments, "Bình luận"));

        if (entries.isEmpty()) entries.add(new PieEntry(1, "Trống"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#EF4444"), Color.parseColor("#3B82F6"));

        // Ẩn chữ trên biểu đồ để tránh đè nhau, chỉ để lại số
        pieChartInteractions.setDrawEntryLabels(false);

        // Gắn Formatter xóa số thập phân
        dataSet.setValueFormatter(intFormatter);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieChartInteractions.setData(pieData);

        Description desc = new Description(); desc.setText("");
        pieChartInteractions.setDescription(desc);
        pieChartInteractions.setCenterText("Tương tác");
        pieChartInteractions.setCenterTextSize(16f);


        Legend legend = pieChartInteractions.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        pieChartInteractions.animateY(1000);
        pieChartInteractions.invalidate();
    }

    // ==========================================
    // 2. BIỂU ĐỒ ĐƯỜNG
    // ==========================================
    private void setupLineChart(List<DashboardStats.Growth7Days> growthList) {
        ArrayList<Entry> userEntries = new ArrayList<>();
        ArrayList<Entry> postEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < growthList.size(); i++) {
            userEntries.add(new Entry(i, growthList.get(i).newUsers));
            postEntries.add(new Entry(i, growthList.get(i).newPosts));
            labels.add(growthList.get(i).date);
        }

        LineDataSet userDataSet = new LineDataSet(userEntries, "User Mới");
        userDataSet.setColor(Color.parseColor("#10B981"));
        userDataSet.setCircleColor(Color.parseColor("#064E3B"));
        userDataSet.setLineWidth(2.5f);
        userDataSet.setDrawValues(false);
        userDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Làm cong đường vẽ

        LineDataSet postDataSet = new LineDataSet(postEntries, "Bài viết Mới");
        postDataSet.setColor(Color.parseColor("#3B82F6"));
        postDataSet.setCircleColor(Color.parseColor("#1E3A8A"));
        postDataSet.setLineWidth(2.5f);
        postDataSet.setDrawValues(false);
        postDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(userDataSet, postDataSet);
        lineChartGrowth.setData(data);

        // Định dạng trục X
        lineChartGrowth.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChartGrowth.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChartGrowth.getXAxis().setGranularity(1f);
        lineChartGrowth.getXAxis().setDrawGridLines(false);

        // Định dạng trục Y (Xóa số thập phân)
        lineChartGrowth.getAxisLeft().setValueFormatter(intFormatter);
        lineChartGrowth.getAxisRight().setEnabled(false);

        Description desc = new Description(); desc.setText("");
        lineChartGrowth.setDescription(desc);
        lineChartGrowth.animateX(1200);
        lineChartGrowth.invalidate();
    }

    // ==========================================
    // 3. BIỂU ĐỒ CỘT
    // ==========================================
    private void setupBarChart(DashboardStats.ReactionBarChart data) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();


        entries.add(new BarEntry(0, data.like)); labels.add("Thích");
        entries.add(new BarEntry(1, data.love)); labels.add("Yêu");
        entries.add(new BarEntry(2, data.haha)); labels.add("Haha");
        entries.add(new BarEntry(3, data.wow));  labels.add("Wow");
        entries.add(new BarEntry(4, data.sad));  labels.add("Buồn");
        entries.add(new BarEntry(5, data.angry));labels.add("Phẫn nộ");

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);

        // Xóa số thập phân trên đầu cột
        dataSet.setValueFormatter(intFormatter);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barChartReactions.setData(barData);

        // Định dạng trục X
        barChartReactions.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartReactions.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartReactions.getXAxis().setGranularity(1f);
        barChartReactions.getXAxis().setDrawGridLines(false);

        // Xóa số thập phân trục Y
        barChartReactions.getAxisLeft().setValueFormatter(intFormatter);
        barChartReactions.getAxisRight().setEnabled(false);

        // Ẩn bảng chú thích (Legend) bên dưới
        barChartReactions.getLegend().setEnabled(false);

        Description desc = new Description(); desc.setText("");
        barChartReactions.setDescription(desc);
        barChartReactions.animateY(1200);
        barChartReactions.invalidate();
    }
}