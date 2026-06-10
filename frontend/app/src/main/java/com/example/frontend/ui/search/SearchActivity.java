package com.example.frontend.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText edtSearch;
    private RecyclerView rvResults;
    private TextView tvHint;
    private ApiService apiService;
    private SearchAdapter adapter;

    private final Handler handler = new Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        edtSearch = findViewById(R.id.edtSearch);
        rvResults = findViewById(R.id.rvResults);
        tvHint    = findViewById(R.id.tvHint);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(this);
        adapter = new SearchAdapter();
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    rvResults.setVisibility(View.GONE);
                    tvHint.setText("Nhập tên để tìm kiếm người dùng");
                    tvHint.setVisibility(View.VISIBLE);
                    return;
                }
                searchRunnable = () -> searchUsers(query);
                handler.postDelayed(searchRunnable, 400);
            }
        });

        edtSearch.requestFocus();
    }

    private void searchUsers(String query) {
        apiService.searchUsers(query).enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call,
                                   Response<ApiResponse<List<User>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<User> results = response.body().getData();
                    adapter.submit(results);
                    boolean empty = results == null || results.isEmpty();
                    tvHint.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rvResults.setVisibility(empty ? View.GONE : View.VISIBLE);
                    if (empty) tvHint.setText("Không tìm thấy người dùng nào");
                } else {
                    Toast.makeText(SearchActivity.this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Adapter ──────────────────────────────────────────────────────────────
    class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.VH> {
        private final List<User> items = new ArrayList<>();

        void submit(List<User> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            User user = items.get(pos);
            h.tvUsername.setText(user.getUsername());
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                Glide.with(h.imgAvatar).load(user.getAvatar())
                        .placeholder(R.drawable.ic_user).into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_user);
            }
            h.itemView.setOnClickListener(v ->
                    Toast.makeText(SearchActivity.this,
                            "Xem hồ sơ: " + user.getUsername(), Toast.LENGTH_SHORT).show()
            );
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView imgAvatar;
            TextView tvUsername;
            VH(@NonNull View v) {
                super(v);
                imgAvatar  = v.findViewById(R.id.imgAvatar);
                tvUsername = v.findViewById(R.id.tvUsername);
            }
        }
    }
}