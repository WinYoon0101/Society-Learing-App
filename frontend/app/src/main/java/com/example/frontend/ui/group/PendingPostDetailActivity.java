package com.example.frontend.ui.group;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.ui.feed.PostImageAdapter;
import com.example.frontend.utils.Result;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Màn chi tiết 1 bài viết đang chờ duyệt (admin): nội dung đầy đủ + ảnh + nút Duyệt/Từ chối.
 * Mở từ PendingPostsActivity (click vào 1 bài). Trả kết quả về để list tự gỡ bài.
 */
public class PendingPostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "postId";
    public static final String EXTRA_AUTHOR_NAME = "authorName";
    public static final String EXTRA_AUTHOR_AVATAR = "authorAvatar";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_IMAGES = "images";

    // Kết quả trả về
    public static final String RESULT_POST_ID = "resultPostId";
    public static final String RESULT_ACTION = "resultAction"; // "approved" | "rejected"

    private String postId;
    private GroupRepository repository;

    private final MutableLiveData<Result<Object>> actionLive = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_post_detail);

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null) { finish(); return; }

        repository = new GroupRepository(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        CircleImageView imgAvatar = findViewById(R.id.imgAvatar);
        TextView tvAuthor = findViewById(R.id.tvAuthorName);
        TextView tvTime = findViewById(R.id.tvTime);
        TextView tvContent = findViewById(R.id.tvContent);
        RecyclerView rvImages = findViewById(R.id.rvPostImages);
        Button btnApprove = findViewById(R.id.btnApprove);
        Button btnReject = findViewById(R.id.btnReject);

        String authorName = getIntent().getStringExtra(EXTRA_AUTHOR_NAME);
        String authorAvatar = getIntent().getStringExtra(EXTRA_AUTHOR_AVATAR);
        String content = getIntent().getStringExtra(EXTRA_CONTENT);
        String time = getIntent().getStringExtra(EXTRA_TIME);
        ArrayList<String> images = getIntent().getStringArrayListExtra(EXTRA_IMAGES);

        tvAuthor.setText(authorName != null ? authorName : "");
        tvTime.setText(time != null ? time : "");
        tvContent.setText(content);
        tvContent.setVisibility(content != null && !content.isEmpty() ? View.VISIBLE : View.GONE);

        if (authorAvatar != null && !authorAvatar.isEmpty()) {
            Glide.with(this).load(authorAvatar).placeholder(R.drawable.ic_user).into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_user);
        }

        if (images != null && !images.isEmpty()) {
            rvImages.setVisibility(View.VISIBLE);
            rvImages.setLayoutManager(new LinearLayoutManager(this));
            rvImages.setAdapter(new PostImageAdapter(this, images));
        } else {
            rvImages.setVisibility(View.GONE);
        }

        actionLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
            }
        });

        btnApprove.setOnClickListener(v -> {
            repository.approvePost(postId, actionLive);
            GroupState.feedDirty = true; // bài được duyệt → feed cần cập nhật
            finishWithResult("approved", "Đã duyệt bài viết");
        });
        btnReject.setOnClickListener(v -> {
            repository.rejectPost(postId, actionLive);
            finishWithResult("rejected", "Đã từ chối bài viết");
        });
    }

    private void finishWithResult(String action, String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
        Intent data = new Intent();
        data.putExtra(RESULT_POST_ID, postId);
        data.putExtra(RESULT_ACTION, action);
        setResult(RESULT_OK, data);
        finish();
    }
}
