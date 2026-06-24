package com.example.frontend.ui.search;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.SearchResponseData;
import com.example.frontend.data.model.TrendingTopic;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText edtSearch;
    private NestedScrollView layoutTrendingContainer;
    private RecyclerView rvTrending, rvResults;
    private TextView tvEmptyState;

    private ApiService apiService;
    private SearchAdapter searchAdapter;
    private TrendingAdapter trendingAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Ánh xạ các thành phần giao diện
        edtSearch = findViewById(R.id.edtSearch);
        layoutTrendingContainer = findViewById(R.id.layoutTrendingContainer);
        rvTrending = findViewById(R.id.rvTrending);
        rvResults = findViewById(R.id.rvResults);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(this);

        // Cấu hình danh sách Xu hướng (Trending)
        trendingAdapter = new TrendingAdapter();
        rvTrending.setLayoutManager(new LinearLayoutManager(this));
        rvTrending.setAdapter(trendingAdapter);

        // Cấu hình danh sách Kết quả tìm kiếm (Search)
        searchAdapter = new SearchAdapter();
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(searchAdapter);

        // Tự động tải danh sách xu hướng khi vừa vào màn hình
        loadTrendingTopics();

        // Xử lý sự kiện gõ phím trên thanh tìm kiếm
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);

                String query = s.toString().trim();

                // Trở về trạng thái mặc định nếu ô nhập liệu trống
                if (query.isEmpty()) {
                    rvResults.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.GONE);
                    layoutTrendingContainer.setVisibility(View.VISIBLE);
                    return;
                }

                // Cơ chế hoãn kích hoạt (Debounce) nhằm tránh việc gửi yêu cầu API liên tục khi gõ phím
                searchRunnable = () -> performSearch(query);
                handler.postDelayed(searchRunnable, 400);
            }
        });

        edtSearch.requestFocus();
    }

    private void loadTrendingTopics() {
        apiService.getTrendingTopics().enqueue(new Callback<ApiResponse<List<TrendingTopic>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<TrendingTopic>>> call, @NonNull Response<ApiResponse<List<TrendingTopic>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    trendingAdapter.submit(response.body().getData());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<TrendingTopic>>> call, @NonNull Throwable t) {
                Toast.makeText(SearchActivity.this, "Không thể tải danh sách xu hướng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSearch(String query) {
        layoutTrendingContainer.setVisibility(View.GONE);

        apiService.searchEverything(query, null).enqueue(new Callback<ApiResponse<SearchResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SearchResponseData>> call, @NonNull Response<ApiResponse<SearchResponseData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SearchResponseData data = response.body().getData();
                    List<User> userResults = data.getUsers();

                    searchAdapter.submit(userResults);

                    boolean isEmpty = (userResults == null || userResults.isEmpty());
                    tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvResults.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SearchResponseData>> call, @NonNull Throwable t) {
                Toast.makeText(SearchActivity.this, "Lỗi kết nối tìm kiếm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── TẬP HỢP ĐIỀU KHIỂN: TRENDING ADAPTER ─────────────────────────────────
    class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.VH> {
        private final List<TrendingTopic> items = new ArrayList<>();

        void submit(List<TrendingTopic> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending, parent, false);
            return new VH(v);
        }


        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            TrendingTopic topic = items.get(pos);

            h.tvRank.setText(String.format("%02d", pos + 1));
            h.tvTopicName.setText(topic.getName());
            h.tvMentions.setText(String.format("%,d đề cập", topic.getMentions()));

            float trend = topic.getTrendPercentage();

            if (trend == 9999) {
                // Xu hướng MỚI (Tuần trước chưa ai nhắc đến)
                h.tvTrendPercentage.setText("MỚI");
                h.cardTrendBadge.setCardBackgroundColor(Color.parseColor("#E6F4EA"));
                h.tvTrendPercentage.setTextColor(Color.parseColor("#10B981"));
                h.imgTrendLine.setImageResource(R.drawable.ic_trend_line_up);
            } else if (trend >= 0) {
                // Xu hướng TĂNG bình thường
                h.tvTrendPercentage.setText(String.format("↑ %.1f%%", trend));
                h.cardTrendBadge.setCardBackgroundColor(Color.parseColor("#E6F4EA"));
                h.tvTrendPercentage.setTextColor(Color.parseColor("#10B981"));
                h.imgTrendLine.setImageResource(R.drawable.ic_trend_line_up);
            } else {
                // Xu hướng GIẢM
                h.tvTrendPercentage.setText(String.format("↓ %.1f%%", Math.abs(trend)));
                h.cardTrendBadge.setCardBackgroundColor(Color.parseColor("#FCE8E6"));
                h.tvTrendPercentage.setTextColor(Color.parseColor("#EA4335"));
                h.imgTrendLine.setImageResource(R.drawable.ic_trend_line_down);
            }

            h.itemView.setOnClickListener(v -> {
                edtSearch.setText(topic.getName());
                edtSearch.setSelection(topic.getName().length());
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvRank, tvTopicName, tvMentions, tvTrendPercentage;
            MaterialCardView cardTrendBadge;
            ImageView imgTrendLine;

            VH(@NonNull View v) {
                super(v);
                tvRank = v.findViewById(R.id.tvRank);
                tvTopicName = v.findViewById(R.id.tvTopicName);
                tvMentions = v.findViewById(R.id.tvMentions);
                tvTrendPercentage = v.findViewById(R.id.tvTrendPercentage);
                cardTrendBadge = v.findViewById(R.id.cardTrendBadge);
                imgTrendLine = v.findViewById(R.id.imgTrendLine);
            }
        }
    }
}