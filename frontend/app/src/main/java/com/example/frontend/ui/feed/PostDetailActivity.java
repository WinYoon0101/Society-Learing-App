package com.example.frontend.ui.feed; // Bạn nhớ chỉnh lại package cho đúng thư mục của mình

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Comment;
import com.example.frontend.ui.feed.CommentAdapter;
import com.example.frontend.ui.feed.PostDetailViewModel;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private PostDetailViewModel viewModel;
    private CommentAdapter commentAdapter;
    private List<Comment> currentCommentList = new ArrayList<>();

    // Các thành phần giao diện
    private EditText edtComment;
    private ImageView btnSendComment, imgPost, imgAvatar, btnBack;
    private TextView tvAuthorName, tvContent;
    private RecyclerView rvComments;

    // Dữ liệu quản lý trạng thái
    private String currentPostId;
    private String replyingToId = null; // null là cmt gốc, có giá trị là đang reply

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // 1. Khởi tạo ViewModel (Bộ não)
        viewModel = new ViewModelProvider(this).get(PostDetailViewModel.class);

        // 2. Ánh xạ View và Setup sự kiện
        initViews();

        // 3. Tiếp nhận "Hành lý" gửi từ PostAdapter sang
        receiveDataFromIntent();

        // 4. Cài đặt danh sách bình luận
        setupRecyclerView();

        // 5. Đăng ký lắng nghe sự thay đổi từ ViewModel (LiveData)
        observeViewModel();

        // 6. Lệnh cho ViewModel đi lấy dữ liệu bình luận từ Server
        if (currentPostId != null) {
            viewModel.fetchComments(currentPostId);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtComment = findViewById(R.id.edtComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        rvComments = findViewById(R.id.rvComments);

        // Các View nằm trong phần <include layout="@layout/item_home_posts" />
        tvAuthorName = findViewById(R.id.tvAuthorName);
        tvContent = findViewById(R.id.tvContent);
        imgAvatar = findViewById(R.id.imgAvatar);
        imgPost = findViewById(R.id.imgPost);

        // Nút quay lại màn hình chính
        btnBack.setOnClickListener(v -> finish());

        // Xử lý nút Gửi bình luận
        btnSendComment.setOnClickListener(v -> {
            String text = edtComment.getText().toString().trim();
            if (!text.isEmpty()) {
                String token = "Bearer " + getSavedToken();
                android.util.Log.d("KIEM_TRA_ID", "ID bài viết gửi đi là: [" + currentPostId + "]");
                // ViewModel sẽ lo việc gọi API
                viewModel.postComment(token, currentPostId, text, replyingToId);
            }
        });
    }

    private void receiveDataFromIntent() {
        currentPostId = getIntent().getStringExtra("POST_ID");

        // Hứng toàn bộ dữ liệu
        String content = getIntent().getStringExtra("POST_CONTENT");
        String authorName = getIntent().getStringExtra("AUTHOR_NAME");
        String authorAvatar = getIntent().getStringExtra("AUTHOR_AVATAR");
        String postImage = getIntent().getStringExtra("POST_IMAGE");

        // 1. Đổ Chữ
        tvContent.setText(content);
        if (authorName != null) tvAuthorName.setText(authorName);

        // 2. Đổ Avatar
        if (authorAvatar != null) {
            Glide.with(this).load(authorAvatar).placeholder(R.drawable.ic_user).into(imgAvatar);
        }

        // 3. Đổ Ảnh bài viết (Có thì hiện, không thì giấu)
        if (postImage != null && !postImage.isEmpty()) {
            imgPost.setVisibility(View.VISIBLE);
            Glide.with(this).load(postImage).into(imgPost);
        } else {
            imgPost.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        rvComments.setLayoutManager(new LinearLayoutManager(this));

        // Lấy ID của bạn từ SharedPreferences để Adapter biết hiện nút Xóa cho đúng bài
        String myUserId = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("USER_ID", "");

        commentAdapter = new CommentAdapter(currentCommentList, myUserId);
        rvComments.setAdapter(commentAdapter);

        // Lắng nghe sự kiện Phản hồi từ Adapter
        commentAdapter.setOnReplyClickListener((commentId, userName) -> {
            replyingToId = commentId;
            edtComment.setHint("Đang trả lời " + userName + "...");
            edtComment.requestFocus();
            showKeyboard();
        });

        // Lắng nghe sự kiện Xóa từ Adapter
        commentAdapter.setOnDeleteClickListener((commentId, position) -> {
            String token = "Bearer " + getSavedToken();
            viewModel.deleteComment(token, currentPostId, commentId);
        });
    }

    private void observeViewModel() {
        // Khi danh sách bình luận thay đổi (sau khi load hoặc thêm/xóa)
        viewModel.getCommentsLiveData().observe(this, comments -> {
            currentCommentList.clear();
            currentCommentList.addAll(comments);
            commentAdapter.notifyDataSetChanged();
        });

        // Khi có thông báo lỗi hoặc thành công từ Server
        viewModel.getMessageLiveData().observe(this, msg -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Khi đăng bình luận thành công -> Reset ô nhập liệu
        viewModel.getActionSuccessLiveData().observe(this, isSuccess -> {
            if (isSuccess) {
                edtComment.setText("");
                edtComment.setHint("Viết bình luận...");
                replyingToId = null;
                hideKeyboard();
            }
        });
    }

    // --- Các hàm hỗ trợ ---
    private String getSavedToken() {
        return getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("JWT_TOKEN", "");
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(edtComment, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) view = new View(this);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}