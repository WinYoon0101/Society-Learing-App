package com.example.frontend.ui.group;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.GroupPost;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.ui.feed.HashtagTextHelper;
import com.example.frontend.ui.feed.PostImageAdapter;
import com.example.frontend.utils.Result;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn "Bài viết của bạn trong nhóm" (G-B4): list bài của chính user trong nhóm + xoá bài của mình.
 * Tái dùng getPostsByGroup (lọc client-side theo authorId) + deletePost sẵn có.
 */
public class MyGroupPostsActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "groupId";
    private static final int LIMIT = 50;

    private String groupId;
    private String myUserId;
    private String token;

    private RecyclerView rv;
    private TextView tvEmpty;
    private MyPostAdapter adapter;
    private GroupRepository repository;
    private ApiService apiService;

    private final MutableLiveData<Result<List<GroupPost>>> postsLive = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_group_posts);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (groupId == null) { finish(); return; }

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        myUserId = prefs.getString("USER_ID", "");
        token = "Bearer " + prefs.getString("JWT_TOKEN", "");

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvMyPosts);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyPostAdapter();
        rv.setAdapter(adapter);

        repository = new GroupRepository(this);
        apiService = ApiClient.getApiService(this);

        postsLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                adapter.submit(filterMine(r.data));
                tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        repository.getPostsByGroup(groupId, 1, LIMIT, postsLive);
    }

    /** Chỉ giữ bài của chính user hiện tại. */
    private List<GroupPost> filterMine(List<GroupPost> data) {
        List<GroupPost> out = new ArrayList<>();
        if (data == null) return out;
        for (GroupPost p : data) {
            if (p.getAuthorId() != null && myUserId.equals(p.getAuthorId().getId())) out.add(p);
        }
        return out;
    }

    private void confirmDelete(GroupPost p) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn có chắc muốn xóa bài viết này khỏi nhóm?")
                .setPositiveButton("Xóa", (d, w) -> doDelete(p))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void doDelete(GroupPost p) {
        adapter.remove(p);
        GroupState.feedDirty = true;
        apiService.deletePost(token, p.getId()).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(MyGroupPostsActivity.this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MyGroupPostsActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                    repository.getPostsByGroup(groupId, 1, LIMIT, postsLive); // đồng bộ lại
                }
            }
            @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(MyGroupPostsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                repository.getPostsByGroup(groupId, 1, LIMIT, postsLive);
            }
        });
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────
    private class MyPostAdapter extends RecyclerView.Adapter<MyPostAdapter.VH> {
        private final List<GroupPost> items = new ArrayList<>();

        void submit(List<GroupPost> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        void remove(GroupPost p) {
            int idx = items.indexOf(p);
            if (idx >= 0) {
                items.remove(idx);
                notifyItemRemoved(idx);
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_group_post, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            GroupPost p = items.get(pos);
            h.tvTime.setText(formatTime(p.getCreatedAt()));
            h.tvContent.setText(HashtagTextHelper.highlight(p.getContent()));
            h.tvContent.setVisibility(
                    p.getContent() != null && !p.getContent().isEmpty() ? View.VISIBLE : View.GONE);

            if (p.getImages() != null && !p.getImages().isEmpty()) {
                h.rvImages.setVisibility(View.VISIBLE);
                h.rvImages.setLayoutManager(new LinearLayoutManager(
                        h.rvImages.getContext(), LinearLayoutManager.HORIZONTAL, false));
                h.rvImages.setAdapter(new PostImageAdapter(h.rvImages.getContext(), p.getImages()));
            } else {
                h.rvImages.setVisibility(View.GONE);
            }

            h.btnDelete.setOnClickListener(v -> confirmDelete(p));
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTime, tvContent;
            RecyclerView rvImages;
            Button btnDelete;
            VH(@NonNull View v) {
                super(v);
                tvTime    = v.findViewById(R.id.tvTime);
                tvContent = v.findViewById(R.id.tvContent);
                rvImages  = v.findViewById(R.id.rvPostImages);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }

    private String formatTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            long ts = fmt.parse(iso).getTime();
            return DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        } catch (Exception e) {
            return "";
        }
    }
}
