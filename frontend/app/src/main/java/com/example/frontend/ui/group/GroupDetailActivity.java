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
import com.example.frontend.data.model.RequestJoinResult;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.ReactionRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID   = "groupId";
    public static final String EXTRA_GROUP_NAME = "groupName";

    private String groupId;

    private ImageView imgCover;
    private CircleImageView imgAvatar, imgMyAvatar;
    private TextView tvToolbarName, tvName, tvSubtitle, tvDescription, tvComposerHint;
    private Button btnJoin, btnInvite;
    private ImageButton btnBack, btnMore, btnSearch;
    private View layoutComposer, composerRow, chipFile, chipPhoto, chipFeeling;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;

    private GroupPostAdapter postAdapter;
    private GroupRepository repository;

    private final MutableLiveData<Result<GroupDetail>> detailLive = new MutableLiveData<>();
    private final MutableLiveData<Result<List<GroupPost>>> postsLive = new MutableLiveData<>();
    private final MutableLiveData<Result<RequestJoinResult>> joinLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> reactLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> leaveLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> deleteLive = new MutableLiveData<>();

    /** false khi xem nhóm Private mà chưa là thành viên → không tải/được xem bài viết. */
    private boolean canViewPosts = true;

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

        btnMore.setOnClickListener(v -> showOptionsSheet());

        btnSearch.setOnClickListener(v ->
                Toast.makeText(this, "Tìm kiếm trong nhóm sắp ra mắt", Toast.LENGTH_SHORT).show());

        btnJoin.setOnClickListener(v -> repository.requestJoinGroup(groupId, joinLive));

        // "Mời" → mở màn hình thành viên và bật hộp thoại mời luôn
        btnInvite.setOnClickListener(v -> openMembers(true));

        // Composer → tạo bài viết trong nhóm
        View.OnClickListener openComposer = v -> {
            Intent i = new Intent(this, com.example.frontend.ui.feed.CreatePostActivity.class);
            i.putExtra("groupId", groupId);
            startActivityForResult(i, 200);
        };
        composerRow.setOnClickListener(openComposer);
        tvComposerHint.setOnClickListener(openComposer);
        chipFile.setOnClickListener(openComposer);
        chipPhoto.setOnClickListener(openComposer);
        chipFeeling.setOnClickListener(openComposer);

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
            // Đăng bài thành công → cập nhật tab "Bài viết"
            if (requestCode == 200) GroupState.onCreatedPost();
            loadAll();
        }
    }

    private void bindViews() {
        btnBack        = findViewById(R.id.btnBack);
        btnMore        = findViewById(R.id.btnMore);
        btnSearch      = findViewById(R.id.btnSearch);
        imgCover       = findViewById(R.id.imgCover);
        imgAvatar      = findViewById(R.id.imgGroupAvatar);
        imgMyAvatar    = findViewById(R.id.imgMyAvatar);

        tvToolbarName  = findViewById(R.id.tvToolbarName);
        tvName         = findViewById(R.id.tvGroupName);
        tvSubtitle     = findViewById(R.id.tvSubtitle);
        tvDescription  = findViewById(R.id.tvGroupDescription);
        tvComposerHint = findViewById(R.id.tvComposerHint);

        btnJoin        = findViewById(R.id.btnJoin);
        btnInvite      = findViewById(R.id.btnInvite);
        layoutComposer = findViewById(R.id.layoutComposer);
        composerRow    = findViewById(R.id.composerRow);
        chipFile       = findViewById(R.id.chipFile);
        chipPhoto      = findViewById(R.id.chipPhoto);
        chipFeeling    = findViewById(R.id.chipFeeling);

        swipeRefresh   = findViewById(R.id.swipeRefresh);
        rv             = findViewById(R.id.rvGroupPosts);

        postAdapter = new GroupPostAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(postAdapter);

        // Avatar của user hiện tại cho composer
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String myAvatar = prefs.getString("USER_AVATAR", "");
        if (myAvatar != null && !myAvatar.isEmpty()) {
            Glide.with(this).load(myAvatar).placeholder(R.drawable.ic_user).into(imgMyAvatar);
        }

        btnJoin.setVisibility(View.GONE);
        btnInvite.setVisibility(View.GONE);
        btnMore.setVisibility(View.GONE);
        layoutComposer.setVisibility(View.GONE);
    }

    private void showOptionsSheet() {
        boolean member = currentDetail != null && currentDetail.isMember();
        boolean admin = currentDetail != null && currentDetail.isAdmin();
        GroupOptionsBottomSheet sheet = GroupOptionsBottomSheet.newInstance(member, admin);
        sheet.setOnOptionSelectedListener(this::handleOption);
        sheet.show(getSupportFragmentManager(), "groupOptions");
    }

    private void handleOption(int optionId) {
        switch (optionId) {
            case GroupOptionsBottomSheet.OPT_MEMBERS:
                openMembers(false);
                break;
            case GroupOptionsBottomSheet.OPT_SETTINGS:
                openSettings();
                break;
            case GroupOptionsBottomSheet.OPT_JOIN:
                repository.requestJoinGroup(groupId, joinLive);
                break;
            case GroupOptionsBottomSheet.OPT_APPROVE_MEMBERS:
                openPendingMembers();
                break;
            case GroupOptionsBottomSheet.OPT_NOT_INTERESTED:
                GroupState.addNotInterested(this, groupId);
                Toast.makeText(this, "Sẽ không gợi ý nhóm này cho bạn nữa", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case GroupOptionsBottomSheet.OPT_LEAVE:
                confirmAction("Rời nhóm", "Bạn có chắc muốn rời khỏi nhóm này?",
                        () -> repository.leaveGroup(groupId, leaveLive));
                break;
            case GroupOptionsBottomSheet.OPT_DELETE:
                confirmAction("Xóa nhóm", "Bạn có chắc muốn xóa nhóm này? Hành động không thể hoàn tác.",
                        () -> repository.deleteGroup(groupId, deleteLive));
                break;
            case GroupOptionsBottomSheet.OPT_MANAGE_CONTENT:
            case GroupOptionsBottomSheet.OPT_MANAGE_NOTIF:
            case GroupOptionsBottomSheet.OPT_APPROVE_POSTS:
            default:
                Toast.makeText(this, "Tính năng đang được phát triển", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void openMembers(boolean openInvite) {
        Intent i = new Intent(this, GroupMembersActivity.class);
        i.putExtra(GroupMembersActivity.EXTRA_GROUP_ID, groupId);
        i.putExtra(GroupMembersActivity.EXTRA_IS_ADMIN,
                currentDetail != null && currentDetail.isAdmin());
        if (openInvite) i.putExtra(GroupMembersActivity.EXTRA_OPEN_INVITE, true);
        startActivity(i);
    }

    private void openPendingMembers() {
        Intent i = new Intent(this, PendingMembersActivity.class);
        i.putExtra(PendingMembersActivity.EXTRA_GROUP_ID, groupId);
        startActivity(i);
    }

    private void openSettings() {
        if (currentDetail == null) return;
        Intent i = new Intent(this, EditGroupActivity.class);
        i.putExtra(EditGroupActivity.EXTRA_GROUP_ID, groupId);
        i.putExtra(EditGroupActivity.EXTRA_GROUP_NAME, currentDetail.getGroupName());
        i.putExtra(EditGroupActivity.EXTRA_DESCRIPTION, currentDetail.getDescription());
        i.putExtra(EditGroupActivity.EXTRA_PRIVACY, currentDetail.getPrivacy());
        i.putExtra(EditGroupActivity.EXTRA_AVATAR_URL, currentDetail.getAvatarUrl());
        startActivityForResult(i, 100);
    }

    private void confirmAction(String title, String message, Runnable onConfirm) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đồng ý", (d, w) -> onConfirm.run())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadAll() {
        page = 1;
        isLastPage = false;
        // Tải chi tiết trước; bài viết chỉ tải sau khi biết quyền xem (renderDetail).
        repository.getGroupDetail(groupId, detailLive);
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
                boolean pending = r.data != null && r.data.isPending();
                if (pending) {
                    Toast.makeText(this, "Đã gửi yêu cầu tham gia, chờ duyệt", Toast.LENGTH_SHORT).show();
                    // Cập nhật trạng thái nút "Đã gửi yêu cầu"
                    repository.getGroupDetail(groupId, detailLive);
                } else {
                    Toast.makeText(this, "Đã tham gia nhóm!", Toast.LENGTH_SHORT).show();
                    GroupState.onJoinedGroup();
                    loadAll();
                }
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        leaveLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                Toast.makeText(this, "Đã rời nhóm", Toast.LENGTH_SHORT).show();
                GroupState.onLeftOrDeletedGroup();
                finish();
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        deleteLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                Toast.makeText(this, "Đã xóa nhóm", Toast.LENGTH_SHORT).show();
                GroupState.onLeftOrDeletedGroup();
                finish();
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDetail(GroupDetail d) {
        currentDetail = d;

        tvName.setText(d.getGroupName());
        tvToolbarName.setText(d.getGroupName());

        String privacyLabel = "Public".equals(d.getPrivacy()) ? "🌐 Công khai" : "🔒 Riêng tư";
        tvSubtitle.setText(privacyLabel + " · " + d.getMemberCount() + " thành viên");

        if (d.getDescription() != null && !d.getDescription().isEmpty()) {
            tvDescription.setVisibility(View.VISIBLE);
            tvDescription.setText(d.getDescription());
        } else {
            tvDescription.setVisibility(View.GONE);
        }

        // Ảnh bìa
        if (d.getCoverUrl() != null && !d.getCoverUrl().isEmpty()) {
            Glide.with(this).load(d.getCoverUrl())
                    .placeholder(R.drawable.bg_group_cover_default).into(imgCover);
        } else {
            imgCover.setImageResource(R.drawable.bg_group_cover_default);
        }

        // Avatar nhóm
        if (d.getAvatarUrl() != null && !d.getAvatarUrl().isEmpty()) {
            Glide.with(this).load(d.getAvatarUrl())
                    .placeholder(R.drawable.ic_group).into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_group);
        }

        boolean isPublic = "Public".equals(d.getPrivacy());

        if (d.isMember()) {
            btnJoin.setVisibility(View.GONE);
            btnInvite.setVisibility(View.VISIBLE);
            layoutComposer.setVisibility(View.VISIBLE);
        } else {
            // Chưa là thành viên: Public → "Tham gia"; Private → "Yêu cầu tham gia"
            // (nếu đã gửi yêu cầu → "Đã gửi yêu cầu", vô hiệu hóa)
            btnJoin.setVisibility(View.VISIBLE);
            if (d.hasPendingRequest()) {
                btnJoin.setText("Đã gửi yêu cầu");
                btnJoin.setEnabled(false);
            } else {
                btnJoin.setText(isPublic ? "Tham gia" : "Yêu cầu tham gia");
                btnJoin.setEnabled(true);
            }
            btnInvite.setVisibility(View.GONE);
            layoutComposer.setVisibility(View.GONE);
        }
        // "..." luôn hiện: thành viên → tùy chọn quản lý; chưa vào → Tham gia / Không quan tâm
        btnMore.setVisibility(View.VISIBLE);

        // Quyền xem bài viết: thành viên, hoặc nhóm Public. Nhóm Private + chưa vào → ẩn feed.
        canViewPosts = d.isMember() || isPublic;
        if (canViewPosts) {
            repository.getPostsByGroup(groupId, page, LIMIT, postsLive);
        } else {
            postAdapter.submit(new ArrayList<>());
            isLastPage = true;
            swipeRefresh.setRefreshing(false);
        }
    }
}