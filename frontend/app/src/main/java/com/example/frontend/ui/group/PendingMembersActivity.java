package com.example.frontend.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.PendingMember;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Màn "Duyệt thành viên" (admin): danh sách yêu cầu tham gia đang chờ + Duyệt / Từ chối.
 */
public class PendingMembersActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "groupId";

    private String groupId;

    private RecyclerView rv;
    private TextView tvEmpty;
    private PendingAdapter adapter;
    private GroupRepository repository;

    private final MutableLiveData<Result<List<PendingMember>>> pendingLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> actionLive = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_members);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (groupId == null) { finish(); return; }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvPending);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingAdapter();
        rv.setAdapter(adapter);

        repository = new GroupRepository(this);

        pendingLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                adapter.submit(r.data);
                tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        actionLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
                // Reload để đồng bộ lại với server nếu thao tác lỗi
                repository.getPendingMembers(groupId, pendingLive);
            }
        });

        repository.getPendingMembers(groupId, pendingLive);
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────
    private class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.VH> {
        private final List<PendingMember> items = new ArrayList<>();

        void submit(List<PendingMember> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        void remove(PendingMember m) {
            int idx = items.indexOf(m);
            if (idx >= 0) {
                items.remove(idx);
                notifyItemRemoved(idx);
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_member, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            PendingMember m = items.get(pos);
            h.tvName.setText(m.getUsername());
            if (m.getAvatar() != null && !m.getAvatar().isEmpty()) {
                Glide.with(h.imgAvatar).load(m.getAvatar())
                        .placeholder(R.drawable.ic_user).into(h.imgAvatar);
            } else {
                h.imgAvatar.setImageResource(R.drawable.ic_user);
            }

            h.btnApprove.setOnClickListener(v -> {
                remove(m);
                Toast.makeText(PendingMembersActivity.this,
                        "Đã duyệt " + m.getUsername(), Toast.LENGTH_SHORT).show();
                repository.approveMember(groupId, m.getUserId(), actionLive);
            });
            h.btnReject.setOnClickListener(v -> {
                remove(m);
                Toast.makeText(PendingMembersActivity.this,
                        "Đã từ chối " + m.getUsername(), Toast.LENGTH_SHORT).show();
                repository.rejectMember(groupId, m.getUserId(), actionLive);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView imgAvatar;
            TextView tvName;
            Button btnApprove, btnReject;
            VH(@NonNull View v) {
                super(v);
                imgAvatar  = v.findViewById(R.id.imgAvatar);
                tvName     = v.findViewById(R.id.tvPendingName);
                btnApprove = v.findViewById(R.id.btnApprove);
                btnReject  = v.findViewById(R.id.btnReject);
            }
        }
    }
}
