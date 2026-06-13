package com.example.frontend.ui.group;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.GroupMember;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupMembersActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "groupId";
    public static final String EXTRA_IS_ADMIN = "isAdmin";
    public static final String EXTRA_OPEN_INVITE = "openInvite";

    private String groupId;
    private boolean isAdmin;
    private boolean pendingOpenInvite;
    private String myUserId;

    private RecyclerView rv;
    private MemberAdapter adapter;
    private GroupRepository repository;
    private ApiService apiService;

    // Lưu danh sách thành viên hiện tại để lọc khi mời
    private List<GroupMember> currentMembers = new ArrayList<>();

    private final MutableLiveData<Result<List<GroupMember>>> membersLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> kickLive = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_members);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        isAdmin = getIntent().getBooleanExtra(EXTRA_IS_ADMIN, false);
        pendingOpenInvite = getIntent().getBooleanExtra(EXTRA_OPEN_INVITE, false);
        if (groupId == null) { finish(); return; }

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        myUserId = prefs.getString("USER_ID", "");

        apiService = ApiClient.getApiService(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Nút Mời – chỉ hiện với admin
        MaterialButton btnInvite = findViewById(R.id.btnInvite);
        if (isAdmin) {
            btnInvite.setVisibility(View.VISIBLE);
            btnInvite.setOnClickListener(v -> openInviteDialog());
        }

        rv = findViewById(R.id.rvMembers);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberAdapter();
        rv.setAdapter(adapter);

        repository = new GroupRepository(this);

        membersLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS && r.data != null) {
                currentMembers = r.data;
                adapter.submit(r.data);
                if (pendingOpenInvite) {
                    pendingOpenInvite = false;
                    openInviteDialog();
                }
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        kickLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                Toast.makeText(this, "Đã xóa thành viên", Toast.LENGTH_SHORT).show();
                repository.getGroupMembers(groupId, membersLive);
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        repository.getGroupMembers(groupId, membersLive);
    }

    // ─── Dialog mời bạn bè ───────────────────────────────────────────────────
    private void openInviteDialog() {
        // Gọi API lấy danh sách bạn bè
        apiService.getFriends().enqueue(new Callback<ApiResponse<List<Friend>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Friend>>> call,
                                   Response<ApiResponse<List<Friend>>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !response.body().isSuccess()) {
                    Toast.makeText(GroupMembersActivity.this,
                            "Không tải được danh sách bạn bè", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<Friend> allFriends = response.body().getData();
                if (allFriends == null) allFriends = new ArrayList<>();

                // Lọc những bạn chưa là thành viên nhóm
                List<String> memberIds = new ArrayList<>();
                for (GroupMember m : currentMembers) memberIds.add(m.getUserId());

                List<Friend> eligible = new ArrayList<>();
                for (Friend f : allFriends) {
                    if (!memberIds.contains(f.getId())) eligible.add(f);
                }

                if (eligible.isEmpty()) {
                    Toast.makeText(GroupMembersActivity.this,
                            "Tất cả bạn bè đã là thành viên nhóm", Toast.LENGTH_SHORT).show();
                    return;
                }

                showFriendPickerDialog(eligible);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Friend>>> call, Throwable t) {
                Toast.makeText(GroupMembersActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFriendPickerDialog(List<Friend> friends) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_invite_friends, null);

        RecyclerView rvFriends = dialogView.findViewById(R.id.rvFriendPick);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        FriendPickAdapter pickAdapter = new FriendPickAdapter(friends);
        rvFriends.setAdapter(pickAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Mời bạn bè vào nhóm")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi lời mời", (d, w) -> {
                    List<Friend> selected = pickAdapter.getSelected();
                    if (selected.isEmpty()) {
                        Toast.makeText(this, "Chưa chọn ai", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendInvitations(selected);
                })
                .create();
        dialog.show();
    }

    private void sendInvitations(List<Friend> friends) {
        final int[] sent = {0};
        final int total = friends.size();
        for (Friend f : friends) {
            Map<String, String> body = new HashMap<>();
            body.put("groupId", groupId);
            body.put("inviteeId", f.getId());
            apiService.sendGroupInvitation(body).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call,
                                       Response<ApiResponse<Object>> response) {
                    sent[0]++;
                    if (sent[0] == total) {
                        Toast.makeText(GroupMembersActivity.this,
                                "Đã gửi " + total + " lời mời!", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    sent[0]++;
                    if (sent[0] == total) {
                        Toast.makeText(GroupMembersActivity.this,
                                "Một số lời mời gửi thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // ─── Adapter chọn bạn bè (checkbox-style) ────────────────────────────────
    class FriendPickAdapter extends RecyclerView.Adapter<FriendPickAdapter.VH> {
        private final List<Friend> items;
        private final boolean[] checked;

        FriendPickAdapter(List<Friend> items) {
            this.items   = items;
            this.checked = new boolean[items.size()];
        }

        List<Friend> getSelected() {
            List<Friend> sel = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                if (checked[i]) sel.add(items.get(i));
            }
            return sel;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_pick, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Friend f = items.get(pos);
            h.tvName.setText(f.getUsername());
            if (f.getAvatar() != null && !f.getAvatar().isEmpty()) {
                Glide.with(h.imgAvatar).load(f.getAvatar())
                        .placeholder(R.drawable.ic_user).into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_user);
            }
            // Đánh dấu màu nền khi được chọn
            h.itemView.setBackgroundColor(checked[pos] ? 0xFFD1FAE5 : 0xFFFFFFFF);
            h.itemView.setOnClickListener(v -> {
                checked[pos] = !checked[pos];
                h.itemView.setBackgroundColor(checked[pos] ? 0xFFD1FAE5 : 0xFFFFFFFF);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView tvName;
            VH(@NonNull View v) {
                super(v);
                imgAvatar = v.findViewById(R.id.imgFriendPickAvatar);
                tvName    = v.findViewById(R.id.tvFriendPickName);
            }
        }
    }

    // ─── Adapter thành viên hiện tại ─────────────────────────────────────────
    class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        private final List<GroupMember> items = new ArrayList<>();

        void submit(List<GroupMember> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_group_member, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            GroupMember m = items.get(pos);
            h.tvName.setText(m.getUsername());
            h.tvRole.setText("admin".equals(m.getRole()) ? "👑 Admin" : "Thành viên");
            if (m.getAvatar() != null && !m.getAvatar().isEmpty()) {
                Glide.with(h.imgAvatar).load(m.getAvatar())
                        .placeholder(R.drawable.ic_user).into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_user);
            }

            boolean canKick = isAdmin && !m.getUserId().equals(myUserId)
                    && !"admin".equals(m.getRole());
            h.btnKick.setVisibility(canKick ? View.VISIBLE : View.GONE);
            h.btnKick.setOnClickListener(v -> {
                new AlertDialog.Builder(GroupMembersActivity.this)
                        .setTitle("Xóa thành viên")
                        .setMessage("Bạn có chắc muốn xóa " + m.getUsername() + " khỏi nhóm?")
                        .setPositiveButton("Xóa", (d, w) ->
                                repository.kickMember(groupId, m.getUserId(), kickLive))
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView imgAvatar;
            TextView tvName, tvRole;
            Button btnKick;
            VH(@NonNull View v) {
                super(v);
                imgAvatar = v.findViewById(R.id.imgAvatar);
                tvName    = v.findViewById(R.id.tvMemberName);
                tvRole    = v.findViewById(R.id.tvRole);
                btnKick   = v.findViewById(R.id.btnKick);
            }
        }
    }
}