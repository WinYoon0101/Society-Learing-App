package com.example.frontend.ui.group;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.GroupDetail;
import com.example.frontend.data.model.GroupPost;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.ReactionRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID   = "groupId";
    public static final String EXTRA_GROUP_NAME = "groupName";

    private String groupId;

    private CircleImageView imgAvatar;
    private TextView tvName, tvMeta, tvDescription, tvMemberCount;
    private Button btnJoin, btnPost, btnMembers;
    private ImageButton btnEdit, btnBack;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;

    private GroupPostAdapter postAdapter;
    private GroupRepository repository;

    private final MutableLiveData<Result<GroupDetail>> detailLive = new MutableLiveData<>();
    private final MutableLiveData<Result<List<GroupPost>>> postsLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> joinLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> reactLive = new MutableLiveData<>();

    private GroupDetail currentDetail;
    private static final int LIMIT = 15;
    private int page = 1;
    private boolean isLastPage = false;
    private boolean isLoadingMore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (groupId == null) { finish(); return; }

        bindViews();
        repository = new GroupRepository(this);

        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            if (currentDetail == null) return;
            Intent i = new Intent(this, EditGroupActivity.class);
            i.putExtra(EditGroupActivity.EXTRA_GROUP_ID, groupId);
            i.putExtra(EditGroupActivity.EXTRA_GROUP_NAME, currentDetail.getGroupName());
            i.putExtra(EditGroupActivity.EXTRA_DESCRIPTION, currentDetail.getDescription());
            i.putExtra(EditGroupActivity.EXTRA_PRIVACY, currentDetail.getPrivacy());
            i.putExtra(EditGroupActivity.EXTRA_AVATAR_URL, currentDetail.getAvatarUrl());
            startActivityForResult(i, 100);
        });

        btnJoin.setOnClickListener(v -> repository.joinGroup(groupId, joinLive));

        btnPost.setOnClickListener(v -> {
            Intent i = new Intent(this, com.example.frontend.ui.feed.CreatePostActivity.class);
            i.putExtra("groupId", groupId);
            startActivityForResult(i, 200);
        });

        btnMembers.setOnClickListener(v -> {
            Intent i = new Intent(this, GroupMembersActivity.class);
            i.putExtra(GroupMembersActivity.EXTRA_GROUP_ID, groupId);
            i.putExtra(GroupMembersActivity.EXTRA_IS_ADMIN, currentDetail != null && currentDetail.isAdmin());
            startActivity(i);
        });

        // Reaction listener — gọi API thực sự
        ApiService apiService = ApiClient.getApiService(this);
        postAdapter.setOnReactionListener((postId, type) -> {
            ReactionRequest req = new ReactionRequest(postId, "Post", type != null ? type : "Like");
            apiService.toggleReaction(req).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    // UI đã cập nhật optimistically trong adapter
                }
                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    Toast.makeText(GroupDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Infinite scroll
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (!isLastPage && !isLoadingMore && lm != null
                        && lm.findLastVisibleItemPosition() >= postAdapter.getItemCount() - 3) {
                    page++;
                    isLoadingMore = true;
                    repository.getPostsByGroup(groupId, page, LIMIT, postsLive);
                }
            }
        });

        swipeRefresh.setOnRefreshListener(this::loadAll);

        observeLiveData();
        loadAll();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && (requestCode == 100 || requestCode == 200)) {
            loadAll();
        }
    }

    private void bindViews() {
        btnBack       = findViewById(R.id.btnBack);
        btnEdit       = findViewById(R.id.btnEdit);
        imgAvatar     = findViewById(R.id.imgGroupAvatar);

        tvName        = findViewById(R.id.tvGroupName);
        tvMeta        = findViewById(R.id.tvMeta);
        tvDescription = findViewById(R.id.tvGroupDescription);
        tvMemberCount = findViewById(R.id.tvMemberCount);

        btnJoin       = findViewById(R.id.btnJoin);
        btnPost       = findViewById(R.id.btnPost);
        btnMembers    = findViewById(R.id.btnMembers);
        swipeRefresh  = findViewById(R.id.swipeRefresh);
        rv            = findViewById(R.id.rvGroupPosts);

        postAdapter = new GroupPostAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(postAdapter);

        btnJoin.setVisibility(View.GONE);
        btnPost.setVisibility(View.GONE);
        btnEdit.setVisibility(View.GONE);
        btnMembers.setVisibility(View.GONE);
    }

    private void loadAll() {
        page = 1;
        isLastPage = false;
        repository.getGroupDetail(groupId, detailLive);
        repository.getPostsByGroup(groupId, page, LIMIT, postsLive);
    }

    private void observeLiveData() {
        detailLive.observe(this, r -> {
            if (r == null) return;
            swipeRefresh.setRefreshing(r.status == Result.Status.LOADING);
            if (r.status == Result.Status.SUCCESS && r.data != null) {
                renderDetail(r.data);
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        postsLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                isLoadingMore = false;
                if (page == 1) postAdapter.submit(r.data);
                else postAdapter.appendAll(r.data);
                if (r.data == null || r.data.size() < LIMIT) isLastPage = true;
            } else if (r.status == Result.Status.ERROR) {
                isLoadingMore = false;
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        joinLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                Toast.makeText(this, "Đã tham gia nhóm!", Toast.LENGTH_SHORT).show();
                loadAll();
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDetail(GroupDetail d) {
        currentDetail = d;

        tvName.setText(d.getGroupName());
        tvDescription.setText(d.getDescription() != null && !d.getDescription().isEmpty()
                ? d.getDescription() : "Không có mô tả");
        tvMeta.setText("Public".equals(d.getPrivacy()) ? "🔓 Công khai" : "🔒 Riêng tư");
        tvMemberCount.setText(d.getMemberCount() + " thành viên");

        if (d.getAvatarUrl() != null && !d.getAvatarUrl().isEmpty()) {
            Glide.with(this).load(d.getAvatarUrl())
                    .placeholder(R.drawable.ic_group).into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_group);
        }

        if (d.isMember()) {
            btnJoin.setVisibility(View.GONE);
            btnPost.setVisibility(View.VISIBLE);
            btnMembers.setVisibility(View.VISIBLE);
        } else {
            btnJoin.setVisibility("Public".equals(d.getPrivacy()) ? View.VISIBLE : View.GONE);
            btnPost.setVisibility(View.GONE);
            btnMembers.setVisibility(View.GONE);
        }

        btnEdit.setVisibility(d.isAdmin() ? View.VISIBLE : View.GONE);
    }
}