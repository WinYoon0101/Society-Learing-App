package com.example.frontend.ui.feed;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper; // Thêm import này cho mượt
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Comment;
import com.example.frontend.ui.feed.CommentAdapter;
import com.example.frontend.ui.feed.PostDetailViewModel;
// Nhớ đảm bảo có import PostImageAdapter nếu nó nằm khác package

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private PostDetailViewModel viewModel;
    private CommentAdapter commentAdapter;
    private List<Comment> currentCommentList = new ArrayList<>();

    // Các thành phần giao diện
    private EditText edtComment;
    private ImageView btnSendComment, imgAvatar, btnBack; // ĐÃ XÓA imgPost ở đây
    private TextView tvAuthorName, tvContent, tvCommentCount;
    private RecyclerView rvComments;

    // ĐÃ THÊM: RecyclerView để chứa nhiều ảnh
    private RecyclerView rvPostImagesFeed;

    // View liên quan đến Reaction
    private LinearLayout layoutTopReactions, btnLikeContainer;
    private TextView tvReactionCount, tvLikeLabel;
    private ImageView imgReact1, imgReact2, imgLikeIcon;

    // Dữ liệu quản lý trạng thái
    private String currentPostId;
    private String replyingToId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // 1. Khởi tạo ViewModel (Bộ não)
        viewModel = new ViewModelProvider(this).get(PostDetailViewModel.class);

        // 2. Ánh xạ View và Setup sự kiện
        initViews();

        // 3. Tiếp nhận "Hành lý" gửi từ PostAdapter sang (Đã bao gồm Cảm xúc)
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
        tvCommentCount = findViewById(R.id.tvCommentCount);

        // ĐÃ SỬA: Ánh xạ chuẩn thành RecyclerView cho nhiều ảnh
        rvPostImagesFeed = findViewById(R.id.rvPostImages);
        if (rvPostImagesFeed != null) {
            rvPostImagesFeed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPostImagesFeed.setOnFlingListener(null);
            PagerSnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(rvPostImagesFeed);
        }

        // Ánh xạ các View của Reaction
        layoutTopReactions = findViewById(R.id.layoutTopReactions);
        tvReactionCount = findViewById(R.id.tvReactionCount);
        imgReact1 = findViewById(R.id.imgReact1);
        imgReact2 = findViewById(R.id.imgReact2);

        btnLikeContainer = findViewById(R.id.btnLike);
        imgLikeIcon = findViewById(R.id.imgLike);
        tvLikeLabel = findViewById(R.id.tvLikeCount);

        // Nút quay lại màn hình chính
        btnBack.setOnClickListener(v -> finish());

        // Mở BottomSheet khi nhấn vào số lượng reaction trong màn hình chi tiết
        if (layoutTopReactions != null) {
            layoutTopReactions.setOnClickListener(v -> {
                ReactionListBottomSheet bottomSheet = ReactionListBottomSheet.newInstance(currentPostId);
                bottomSheet.show(getSupportFragmentManager(), "ReactionBottomSheet");
            });
        }

        // Cảnh báo nhẹ khi người dùng cố gắng đổi cảm xúc trong trang chi tiết
        if (btnLikeContainer != null) {
            btnLikeContainer.setOnClickListener(v -> {
                Toast.makeText(this, "Hãy trở ra màn hình chính để thay đổi cảm xúc nhé!", Toast.LENGTH_SHORT).show();
            });
        }

        // Xử lý nút Gửi bình luận
        btnSendComment.setOnClickListener(v -> {
            String text = edtComment.getText().toString().trim();
            if (!text.isEmpty()) {
                String token = "Bearer " + getSavedToken();
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

        // ĐÃ SỬA: Hứng MẢNG ẢNH thay vì 1 string đơn lẻ
        ArrayList<String> postImages = getIntent().getStringArrayListExtra("POST_IMAGES");

        // Nhận dữ liệu Reaction & Comment
        int commentCount = getIntent().getIntExtra("COMMENT_COUNT", 0);
        int reactionCount = getIntent().getIntExtra("REACTION_COUNT", 0);
        String myReaction = getIntent().getStringExtra("MY_REACTION");
        ArrayList<String> topReactions = getIntent().getStringArrayListExtra("TOP_REACTIONS");

        // 1. Đổ Chữ và Ảnh cơ bản
        if (tvContent != null) tvContent.setText(content);
        if (authorName != null && tvAuthorName != null) tvAuthorName.setText(authorName);
        if (authorAvatar != null && imgAvatar != null) {
            Glide.with(this).load(authorAvatar).placeholder(R.drawable.ic_user).into(imgAvatar);
        }

        // ĐÃ SỬA: Set Adapter để hiển thị nhiều ảnh y như ngoài bảng tin
        if (postImages != null && !postImages.isEmpty() && rvPostImagesFeed != null) {
            rvPostImagesFeed.setVisibility(View.VISIBLE);
            PostImageAdapter imageAdapter = new PostImageAdapter(this, postImages);
            rvPostImagesFeed.setAdapter(imageAdapter);
        } else if (rvPostImagesFeed != null) {
            rvPostImagesFeed.setVisibility(View.GONE);
        }

        // 2. Đổ số lượng Comment
        if (tvCommentCount != null) {
            tvCommentCount.setText(String.valueOf(commentCount));
        }

        // 3. Đổ Nút Like (Trạng thái màu sắc)
        if (imgLikeIcon != null && tvLikeLabel != null) {
            imgLikeIcon.setImageResource(getIconForReaction(myReaction));
            if (myReaction != null) {
                tvLikeLabel.setText(myReaction);
            } else {
                tvLikeLabel.setText("Thích");
            }
        }

        // 4. Đổ Top Reaction (Hiển thị các icon nhỏ xíu)
        if (layoutTopReactions != null) {
            if (reactionCount > 0) {
                layoutTopReactions.setVisibility(View.VISIBLE);
                tvReactionCount.setText(String.valueOf(reactionCount));

                imgReact1.setVisibility(View.GONE);
                imgReact2.setVisibility(View.GONE);

                if (topReactions != null && !topReactions.isEmpty()) {
                    imgReact1.setVisibility(View.VISIBLE);
                    imgReact1.setImageResource(getIconForReaction(topReactions.get(0)));

                    if (topReactions.size() > 1) {
                        imgReact2.setVisibility(View.VISIBLE);
                        imgReact2.setImageResource(getIconForReaction(topReactions.get(1)));
                    }
                }
            } else {
                layoutTopReactions.setVisibility(View.GONE);
            }
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
        // Khi danh sách bình luận thay đổi
        viewModel.getCommentsLiveData().observe(this, comments -> {
            currentCommentList.clear();
            currentCommentList.addAll(comments);
            commentAdapter.notifyDataSetChanged();
        });

        viewModel.getMessageLiveData().observe(this, msg -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Lắng nghe sự thay đổi của số lượng bình luận
        viewModel.getCommentCountLiveData().observe(this, count -> {
            if (tvCommentCount != null) {
                tvCommentCount.setText(String.valueOf(count));
            }
        });

        // Khi đăng bình luận thành công
        viewModel.getActionSuccessLiveData().observe(this, isSuccess -> {
            if (isSuccess) {
                edtComment.setText("");
                edtComment.setHint("Viết bình luận...");
                replyingToId = null;
                hideKeyboard();
            }
        });
    }

    // --- Hàm hỗ trợ chuyển đổi chữ thành Icon ---
    private int getIconForReaction(String type) {
        if (type == null) return R.drawable.ic_like;
        switch (type) {
            case "Like": return R.drawable.ic_like_color;
            case "Love": return R.drawable.ic_love;
            case "Haha": return R.drawable.ic_haha;
            case "Wow":  return R.drawable.ic_wow;
            case "Sad":  return R.drawable.ic_sad;
            case "Angry":return R.drawable.ic_angry;
            default: return R.drawable.ic_like;
        }
    }

    // --- Các hàm hỗ trợ khác ---
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