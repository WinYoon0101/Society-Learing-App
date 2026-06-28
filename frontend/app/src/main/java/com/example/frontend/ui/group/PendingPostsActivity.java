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
import com.example.frontend.data.model.GroupPost;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.ui.feed.HashtagTextHelper;
import com.example.frontend.ui.feed.PostImageAdapter;
import com.example.frontend.utils.Result;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.text.format.DateUtils;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Màn "Duyệt bài viết" (admin): danh sách bài đang chờ duyệt + Duyệt / Từ chối.
 */
public class PendingPostsActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "groupId";
    public static final String EXTRA_HIGHLIGHT_POST_ID = "highlightPostId";

    private static final int REQ_DETAIL = 301;

    private String groupId;
    private String highlightPostId;

    private RecyclerView rv;
    private TextView tvEmpty;
    private PendingPostAdapter adapter;
    private GroupRepository repository;

    private final MutableLiveData<Result<List<GroupPost>>> pendingLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> actionLive = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_posts);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        highlightPostId = getIntent().getStringExtra(EXTRA_HIGHLIGHT_POST_ID);
        if (groupId == null) { finish(); return; }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvPending);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingPostAdapter();
        rv.setAdapter(adapter);

        repository = new GroupRepository(this);

        pendingLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                adapter.submit(r.data);
                tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                scrollToHighlight();
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        actionLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
                // Đồng bộ lại với server nếu lỗi
                repository.getPendingPosts(groupId, pendingLive);
            }
        });

        repository.getPendingPosts(groupId, pendingLive);
    }

    private void scrollToHighlight() {
        if (highlightPostId == null) return;
        int idx = adapter.indexOfPost(highlightPostId);
        if (idx >= 0) rv.scrollToPosition(idx);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_DETAIL && resultCode == RESULT_OK && data != null) {
            // Bài đã được duyệt/từ chối ở màn chi tiết → gỡ khỏi danh sách
            String pid = data.getStringExtra(PendingPostDetailActivity.RESULT_POST_ID);
            adapter.removeById(pid);
        }
    }

    private void openDetail(GroupPost p) {
        android.content.Intent i = new android.content.Intent(this, PendingPostDetailActivity.class);
        i.putExtra(PendingPostDetailActivity.EXTRA_POST_ID, p.getId());
        if (p.getAuthorId() != null) {
            i.putExtra(PendingPostDetailActivity.EXTRA_AUTHOR_NAME, p.getAuthorId().getUsername());
            i.putExtra(PendingPostDetailActivity.EXTRA_AUTHOR_AVATAR, p.getAuthorId().getAvatar());
        }
        i.putExtra(PendingPostDetailActivity.EXTRA_CONTENT, p.getContent());
        i.putExtra(PendingPostDetailActivity.EXTRA_TIME, formatTime(p.getCreatedAt()));
        if (p.getImages() != null) {
            i.putStringArrayListExtra(PendingPostDetailActivity.EXTRA_IMAGES, new ArrayList<>(p.getImages()));
        }
        startActivityForResult(i, REQ_DETAIL);
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────
    private class PendingPostAdapter extends RecyclerView.Adapter<PendingPostAdapter.VH> {
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

        void removeById(String postId) {
            if (postId == null) return;
            for (int i = 0; i < items.size(); i++) {
                if (postId.equals(items.get(i).getId())) {
                    items.remove(i);
                    notifyItemRemoved(i);
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    return;
                }
            }
        }

        int indexOfPost(String postId) {
            if (postId == null) return -1;
            for (int i = 0; i < items.size(); i++) {
                if (postId.equals(items.get(i).getId())) return i;
            }
            return -1;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_post, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            GroupPost p = items.get(pos);

            // Làm nổi bật bài đến từ thông báo
            boolean highlighted = highlightPostId != null && highlightPostId.equals(p.getId());
            ((androidx.cardview.widget.CardView) h.itemView).setCardBackgroundColor(
                    highlighted ? 0xFFECFDF5 : 0xFFFFFFFF);

            // Click vào khung (ngoài 2 nút) → mở màn chi tiết xét duyệt
            h.itemView.setOnClickListener(v -> openDetail(p));

            if (p.getAuthorId() != null) {
                h.tvAuthor.setText(p.getAuthorId().getUsername());
                if (p.getAuthorId().getAvatar() != null && !p.getAuthorId().getAvatar().isEmpty()) {
                    Glide.with(h.imgAvatar).load(p.getAuthorId().getAvatar())
                            .placeholder(R.drawable.ic_user).into(h.imgAvatar);
                } else {
                    h.imgAvatar.setImageResource(R.drawable.ic_user);
                }
            }

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

            h.btnApprove.setOnClickListener(v -> {
                remove(p);
                Toast.makeText(PendingPostsActivity.this, "Đã duyệt bài viết", Toast.LENGTH_SHORT).show();
                repository.approvePost(p.getId(), actionLive);
                GroupState.feedDirty = true; // bài được duyệt → feed cần cập nhật
            });
            h.btnReject.setOnClickListener(v -> {
                remove(p);
                Toast.makeText(PendingPostsActivity.this, "Đã từ chối bài viết", Toast.LENGTH_SHORT).show();
                repository.rejectPost(p.getId(), actionLive);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView imgAvatar;
            TextView tvAuthor, tvTime, tvContent;
            RecyclerView rvImages;
            Button btnApprove, btnReject;
            VH(@NonNull View v) {
                super(v);
                imgAvatar  = v.findViewById(R.id.imgAvatar);
                tvAuthor   = v.findViewById(R.id.tvAuthorName);
                tvTime     = v.findViewById(R.id.tvTime);
                tvContent  = v.findViewById(R.id.tvContent);
                rvImages   = v.findViewById(R.id.rvPostImages);
                btnApprove = v.findViewById(R.id.btnApprove);
                btnReject  = v.findViewById(R.id.btnReject);
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
