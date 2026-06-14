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
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Comment;
import com.example.frontend.data.model.Post;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private PostDetailViewModel viewModel;
    private CommentAdapter commentAdapter;
    private List<Comment> currentCommentList = new ArrayList<>();

    // Các thành phần giao diện
    private EditText edtComment;
    private ImageView btnSendComment, imgAvatar, btnBack;

    // ĐÃ THÊM tvTime VÀO ĐÂY
    private TextView tvAuthorName, tvContent, tvCommentCount, tvTime;

    private RecyclerView rvComments;
    private RecyclerView rvPostImagesFeed;

    // View liên quan đến Reaction
    private LinearLayout layoutTopReactions, btnLikeContainer;
    private TextView tvReactionCount, tvLikeLabel;
    private TextView imgReact1, imgReact2, imgLikeIcon;

    // Dữ liệu quản lý trạng thái
    private String currentPostId;
    private String replyingToId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        viewModel = new ViewModelProvider(this).get(PostDetailViewModel.class);
        initViews();
        receiveDataFromIntent();
        setupRecyclerView();
        observeViewModel();

        if (currentPostId != null) {
            viewModel.fetchComments(currentPostId);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtComment = findViewById(R.id.edtComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        rvComments = findViewById(R.id.rvComments);

        tvAuthorName = findViewById(R.id.tvAuthorName);
        tvContent = findViewById(R.id.tvContent);
        imgAvatar = findViewById(R.id.imgAvatar);
        tvCommentCount = findViewById(R.id.tvCommentCount);

        // ĐÃ THÊM: Ánh xạ View thời gian
        tvTime = findViewById(R.id.tvTime);

        rvPostImagesFeed = findViewById(R.id.rvPostImages);
        if (rvPostImagesFeed != null) {
            rvPostImagesFeed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPostImagesFeed.setOnFlingListener(null);
            PagerSnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(rvPostImagesFeed);
        }

        layoutTopReactions = findViewById(R.id.layoutTopReactions);
        tvReactionCount = findViewById(R.id.tvReactionCount);
        imgReact1 = findViewById(R.id.imgReact1);
        imgReact2 = findViewById(R.id.imgReact2);

        btnLikeContainer = findViewById(R.id.btnLike);
        imgLikeIcon = findViewById(R.id.imgLike);
        tvLikeLabel = findViewById(R.id.tvLikeCount);

        btnBack.setOnClickListener(v -> finish());

        if (layoutTopReactions != null) {
            layoutTopReactions.setOnClickListener(v -> {
                ReactionListBottomSheet bottomSheet = ReactionListBottomSheet.newInstance(currentPostId);
                bottomSheet.show(getSupportFragmentManager(), "ReactionBottomSheet");
            });
        }

        if (btnLikeContainer != null) {
            btnLikeContainer.setOnClickListener(v -> {
                Toast.makeText(this, "Hãy trở ra màn hình chính để thay đổi cảm xúc nhé!", Toast.LENGTH_SHORT).show();
            });
        }

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
        String content = getIntent().getStringExtra("POST_CONTENT");

        if (content == null || content.isEmpty()) {
            // Mở từ THÔNG BÁO -> Chỉ có POST_ID, phải gọi API lấy chi tiết
            if (currentPostId != null) {
                viewModel.fetchPostById(currentPostId);
            }
        } else {
            // Mở từ BẢNG TIN -> Đã có sẵn dữ liệu
            String authorName = getIntent().getStringExtra("AUTHOR_NAME");
            String authorAvatar = getIntent().getStringExtra("AUTHOR_AVATAR");
            String postTime = getIntent().getStringExtra("POST_TIME"); // ĐÃ THÊM HỨNG THỜI GIAN

            ArrayList<String> postImages = getIntent().getStringArrayListExtra("POST_IMAGES");
            int commentCount = getIntent().getIntExtra("COMMENT_COUNT", 0);
            int reactionCount = getIntent().getIntExtra("REACTION_COUNT", 0);
            String myReaction = getIntent().getStringExtra("MY_REACTION");
            ArrayList<String> topReactions = getIntent().getStringArrayListExtra("TOP_REACTIONS");

            if (tvContent != null) tvContent.setText(content);
            if (authorName != null && tvAuthorName != null) tvAuthorName.setText(authorName);
            if (authorAvatar != null && imgAvatar != null) {
                Glide.with(this).load(authorAvatar).placeholder(R.drawable.ic_user).into(imgAvatar);
            }

            // ĐÃ THÊM: Set chữ hiển thị thời gian
            if (tvTime != null) tvTime.setText(formatTime(postTime));

            if (postImages != null && !postImages.isEmpty() && rvPostImagesFeed != null) {
                rvPostImagesFeed.setVisibility(View.VISIBLE);
                PostImageAdapter imageAdapter = new PostImageAdapter(this, postImages);
                rvPostImagesFeed.setAdapter(imageAdapter);
            } else if (rvPostImagesFeed != null) {
                rvPostImagesFeed.setVisibility(View.GONE);
            }

            if (tvCommentCount != null) {
                tvCommentCount.setText(String.valueOf(commentCount));
            }

            if (imgLikeIcon != null && tvLikeLabel != null) {
                imgLikeIcon.setText(getEmojiForReaction(myReaction));
                if (myReaction != null) {
                    tvLikeLabel.setText(myReaction);
                } else {
                    tvLikeLabel.setText("Thích");
                }
            }

            if (layoutTopReactions != null) {
                if (reactionCount > 0) {
                    layoutTopReactions.setVisibility(View.VISIBLE);
                    tvReactionCount.setText(String.valueOf(reactionCount));

                    imgReact1.setVisibility(View.GONE);
                    imgReact2.setVisibility(View.GONE);

                    if (topReactions != null && !topReactions.isEmpty()) {
                        imgReact1.setVisibility(View.VISIBLE);
                        imgReact1.setText(getEmojiForReaction(topReactions.get(0)));

                        if (topReactions.size() > 1) {
                            imgReact2.setVisibility(View.VISIBLE);
                            imgReact2.setText(getEmojiForReaction(topReactions.get(1)));
                        }
                    }
                } else {
                    layoutTopReactions.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setupRecyclerView() {
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        String myUserId = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("USER_ID", "");
        commentAdapter = new CommentAdapter(currentCommentList, myUserId);
        rvComments.setAdapter(commentAdapter);

        commentAdapter.setOnReplyClickListener((commentId, userName) -> {
            replyingToId = commentId;
            edtComment.setHint("Đang trả lời " + userName + "...");
            edtComment.requestFocus();
            showKeyboard();
        });

        commentAdapter.setOnDeleteClickListener((commentId, position) -> {
            String token = "Bearer " + getSavedToken();
            viewModel.deleteComment(token, currentPostId, commentId);
        });

        commentAdapter.setOnReactionChangedListener(() -> {
            if (currentPostId != null) {
                viewModel.fetchComments(currentPostId);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getCommentsLiveData().observe(this, comments -> {
            currentCommentList.clear();
            currentCommentList.addAll(comments);
            commentAdapter.notifyDataSetChanged();
        });

        viewModel.getMessageLiveData().observe(this, msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());

        viewModel.getCommentCountLiveData().observe(this, count -> {
            if (tvCommentCount != null) tvCommentCount.setText(String.valueOf(count));
        });

        viewModel.getActionSuccessLiveData().observe(this, isSuccess -> {
            if (isSuccess) {
                edtComment.setText("");
                edtComment.setHint("Viết bình luận...");
                replyingToId = null;
                hideKeyboard();
            }
        });

        viewModel.getPostLiveData().observe(this, post -> {
            if (post == null) return;

            if (tvContent != null) tvContent.setText(post.getContent());
            if (post.getAuthorId() != null) {
                if (tvAuthorName != null) tvAuthorName.setText(post.getAuthorId().getUsername());
                if (imgAvatar != null) Glide.with(this).load(post.getAuthorId().getAvatar()).placeholder(R.drawable.ic_user).into(imgAvatar);
            }

            // ĐÃ THÊM: Set thời gian khi dữ liệu được load từ Thông báo
            if (tvTime != null) tvTime.setText(formatTime(post.getCreatedAt()));

            if (post.getImages() != null && !post.getImages().isEmpty() && rvPostImagesFeed != null) {
                rvPostImagesFeed.setVisibility(View.VISIBLE);
                PostImageAdapter imageAdapter = new PostImageAdapter(this, post.getImages());
                rvPostImagesFeed.setAdapter(imageAdapter);
            } else if (rvPostImagesFeed != null) {
                rvPostImagesFeed.setVisibility(View.GONE);
            }
        });
    }

    // ĐÃ THÊM: Hàm format thời gian y như bên PostAdapter
    private String formatTime(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "Vừa xong";
        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = format.parse(dateString);
            if (date == null) return "Vừa xong";

            long diffMs = System.currentTimeMillis() - date.getTime();
            long minutes = diffMs / (60 * 1000);
            long hours = diffMs / (60 * 60 * 1000);
            long days = hours / 24;

            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            if (days < 7) return days + " ngày trước";

            return new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "Vừa xong";
        }
    }

    private String getEmojiForReaction(String type) {
        if (type == null) return "👍";
        switch (type) {
            case "Like": return "👍";
            case "Love": return "❤️";
            case "Haha": return "😆";
            case "Wow":  return "😮";
            case "Sad":  return "😢";
            case "Angry":return "😡";
            default: return "👍";
        }
    }

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