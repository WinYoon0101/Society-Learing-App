package com.example.frontend.ui.feed;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Comment;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private PostDetailViewModel viewModel;
    private CommentAdapter commentAdapter;
    private List<Comment> currentCommentList = new ArrayList<>();

    private EditText edtComment;
    private ImageView btnSendComment, imgAvatar, btnBack;
    private TextView tvAuthorName, tvContent, tvCommentCount, tvTime;
    private RecyclerView rvComments;
    private RecyclerView rvPostImagesFeed;

    private LinearLayout layoutTopReactions, btnLikeContainer;
    private TextView tvReactionCount, tvLikeLabel;
    private TextView imgReact1, imgReact2, imgLikeIcon;
    private View btnShare;

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
            // 👉 BỔ SUNG: Luôn fetch data mới từ server để cập nhật lại Tag và React nếu có thay đổi
            viewModel.fetchPostById(currentPostId);
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
        tvTime = findViewById(R.id.tvTime);
        btnShare = findViewById(R.id.btnShare);

        rvPostImagesFeed = findViewById(R.id.rvPostImages);
        if (rvPostImagesFeed != null) {
            rvPostImagesFeed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPostImagesFeed.setOnFlingListener(null);
            PagerSnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(rvPostImagesFeed);
            rvPostImagesFeed.addItemDecoration(new DotsIndicatorDecoration());
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

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");

                String author = tvAuthorName != null ? tvAuthorName.getText().toString() : "Ai đó";
                String textContent = tvContent != null ? tvContent.getText().toString() : "";

                String shareMessage = author + " vừa chia sẻ một bài viết:\n\n"
                        + "\"" + textContent + "\"\n\n"
                        + "👉 Mở ứng dụng để xem chi tiết nhé!";

                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, "Chia sẻ bài viết qua"));
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

    // ==========================================
    // 👉 HÀM TIỆN ÍCH DÙNG CHUNG: VẼ TÊN VÀ TAG BẤM ĐƯỢC
    // ==========================================
    private void setAuthorAndTags(String authorId, String authorName, String authorAvatar, String tagId, String tagName, int tagCount) {
        if (tvAuthorName == null) return;

        View.OnClickListener goToAuthorProfile = v -> {
            if (authorId != null) {
                Intent intent = new Intent(this, com.example.frontend.ui.profile.FriendProfileActivity.class);
                intent.putExtra("FRIEND_ID", authorId);
                intent.putExtra("FRIEND_NAME", authorName);
                intent.putExtra("FRIEND_AVATAR", authorAvatar);
                startActivity(intent);
            }
        };

        if (imgAvatar != null) {
            Glide.with(this).load(authorAvatar).placeholder(R.drawable.ic_user).into(imgAvatar);
            imgAvatar.setOnClickListener(goToAuthorProfile);
        }

        if (tagName != null && !tagName.isEmpty()) {
            String prefix = " — cùng với ";
            String suffix = "";
            if (tagCount > 1) {
                suffix = " và " + (tagCount - 1) + " người khác";
            }

            String fullText = authorName + prefix + tagName + suffix;
            SpannableString spannableString = new SpannableString(fullText);

            ClickableSpan authorSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    goToAuthorProfile.onClick(widget);
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    ds.setColor(Color.parseColor("#050505"));
                    ds.setFakeBoldText(true);
                }
            };
            spannableString.setSpan(authorSpan, 0, authorName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ClickableSpan taggedSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (tagId != null) {
                        Intent intent = new Intent(PostDetailActivity.this, com.example.frontend.ui.profile.FriendProfileActivity.class);
                        intent.putExtra("FRIEND_ID", tagId);
                        intent.putExtra("FRIEND_NAME", tagName);
                        startActivity(intent);
                    }
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    ds.setColor(Color.parseColor("#050505"));
                    ds.setFakeBoldText(true);
                }
            };

            int startTag = authorName.length() + prefix.length();
            int endTag = startTag + tagName.length();
            spannableString.setSpan(taggedSpan, startTag, endTag, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvAuthorName.setText(spannableString);
            tvAuthorName.setMovementMethod(LinkMovementMethod.getInstance());
            tvAuthorName.setHighlightColor(Color.TRANSPARENT);
            tvAuthorName.setOnClickListener(null);
        } else {
            tvAuthorName.setText(authorName);
            tvAuthorName.setOnClickListener(goToAuthorProfile);
            tvAuthorName.setMovementMethod(null);
        }
    }

    private void receiveDataFromIntent() {
        currentPostId = getIntent().getStringExtra("POST_ID");
        String content = getIntent().getStringExtra("POST_CONTENT");

        if (content != null && !content.isEmpty()) {
            String authorId = getIntent().getStringExtra("AUTHOR_ID");
            String authorName = getIntent().getStringExtra("AUTHOR_NAME");
            String authorAvatar = getIntent().getStringExtra("AUTHOR_AVATAR");
            String postTime = getIntent().getStringExtra("POST_TIME");

            // Dữ liệu Tag từ Adapter chuyển sang
            String tagId = getIntent().getStringExtra("TAG_ID");
            String tagName = getIntent().getStringExtra("TAG_NAME");
            int tagCount = getIntent().getIntExtra("TAG_COUNT", 0);

            ArrayList<String> postImages = getIntent().getStringArrayListExtra("POST_IMAGES");
            int commentCount = getIntent().getIntExtra("COMMENT_COUNT", 0);
            int reactionCount = getIntent().getIntExtra("REACTION_COUNT", 0);
            String myReaction = getIntent().getStringExtra("MY_REACTION");
            ArrayList<String> topReactions = getIntent().getStringArrayListExtra("TOP_REACTIONS");

            if (tvContent != null) tvContent.setText(content);

            // 👉 Gọi hàm Setup UI Tên + Tag
            setAuthorAndTags(authorId, authorName, authorAvatar, tagId, tagName, tagCount);

            if (tvTime != null) tvTime.setText(formatTime(postTime));

            if (postImages != null && !postImages.isEmpty() && rvPostImagesFeed != null) {
                rvPostImagesFeed.setVisibility(View.VISIBLE);
                PostImageAdapter imageAdapter = new PostImageAdapter(this, postImages);
                rvPostImagesFeed.setAdapter(imageAdapter);
            } else if (rvPostImagesFeed != null) {
                rvPostImagesFeed.setVisibility(View.GONE);
            }

            if (tvCommentCount != null) tvCommentCount.setText(String.valueOf(commentCount));

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

        commentAdapter.setOnReactionClickListener((commentId, reactionType) -> {
            viewModel.toggleCommentReaction(commentId, reactionType);
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
                String tagId = null;
                String tagName = null;
                int tagCount = 0;

                if (post.getTags() != null && !post.getTags().isEmpty() && post.getTags().get(0) != null) {
                    tagId = post.getTags().get(0).getId();
                    tagName = post.getTags().get(0).getUsername();
                    tagCount = post.getTags().size();
                }

                // 👉 Tự động vẽ Tên và Tag khi cập nhật xong API
                setAuthorAndTags(post.getAuthorId().getId(), post.getAuthorId().getUsername(), post.getAuthorId().getAvatar(), tagId, tagName, tagCount);
            }

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