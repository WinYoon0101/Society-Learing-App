package com.example.frontend.ui.story;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Story;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.google.android.material.imageview.ShapeableImageView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoryViewActivity extends AppCompatActivity {

    private ImageView imgStory;
    private ShapeableImageView imgAuthorAvatar;
    private TextView tvAuthorName, tvCaption;
    private ProgressBar progressStory;
    private ApiService apiService;

    private static final int STORY_DURATION_MS = 5000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoCloseRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_view);

        imgStory = findViewById(R.id.imgStory);
        imgAuthorAvatar = findViewById(R.id.imgAuthorAvatar);
        tvAuthorName = findViewById(R.id.tvAuthorName);
        tvCaption = findViewById(R.id.tvCaption);
        progressStory = findViewById(R.id.progressStory);
        ImageButton btnClose = findViewById(R.id.btnClose);

        apiService = ApiClient.getApiService(this);

        String storyId = getIntent().getStringExtra("STORY_ID");
        String authorName = getIntent().getStringExtra("STORY_GROUP_AUTHOR_NAME");
        String authorAvatar = getIntent().getStringExtra("STORY_GROUP_AUTHOR_AVATAR");

        tvAuthorName.setText(authorName != null ? authorName : "");
        loadAuthorAvatar(authorAvatar);

        btnClose.setOnClickListener(v -> finish());

        imgStory.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) finish();
            return true;
        });

        if (storyId != null) loadStory(storyId);

        autoCloseRunnable = this::finish;
        handler.postDelayed(autoCloseRunnable, STORY_DURATION_MS);
        startProgressAnimation();
    }

    private void loadAuthorAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .centerCrop()
                    .into(imgAuthorAvatar);
        } else {
            imgAuthorAvatar.setImageResource(R.drawable.ic_user);
        }
    }

    private void loadStory(String storyId) {
        apiService.viewStory(storyId).enqueue(new Callback<ApiResponse<Story>>() {
            @Override
            public void onResponse(Call<ApiResponse<Story>> call,
                                   Response<ApiResponse<Story>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    Story story = response.body().getData();
                    if (story == null) return;

                    Glide.with(StoryViewActivity.this)
                            .load(story.getMediaUrl())
                            .into(imgStory);

                    User author = story.getAuthor();
                    if (author != null) {
                        if (author.getUsername() != null && !author.getUsername().isEmpty()) {
                            tvAuthorName.setText(author.getUsername());
                        }
                        if (author.getAvatar() != null && !author.getAvatar().isEmpty()) {
                            loadAuthorAvatar(author.getAvatar());
                        }
                    }

                    if (story.getCaption() != null && !story.getCaption().isEmpty()) {
                        tvCaption.setVisibility(View.VISIBLE);
                        tvCaption.setText(story.getCaption());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Story>> call, Throwable t) {}
        });
    }

    private void startProgressAnimation() {
        progressStory.setProgress(0);
        progressStory.setMax(100);
        final int steps = 50;
        final long interval = STORY_DURATION_MS / steps;
        final int[] progress = {0};
        handler.post(new Runnable() {
            @Override
            public void run() {
                progress[0]++;
                progressStory.setProgress(progress[0] * 2);
                if (progress[0] < steps) handler.postDelayed(this, interval);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
