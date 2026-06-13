package com.example.frontend.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.repository.ChatRepository;
import com.example.frontend.ui.chat.ChatDetailActivity;
import com.example.frontend.utils.Result;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendProfileActivity extends AppCompatActivity {

    private String friendId;
    private ChatRepository chatRepository;
    private final MutableLiveData<Result<Conversation>> convLive = new MutableLiveData<>();
    private MaterialButton btnMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        friendId = getIntent().getStringExtra("FRIEND_ID");
        String friendName   = getIntent().getStringExtra("FRIEND_NAME");
        String friendAvatar = getIntent().getStringExtra("FRIEND_AVATAR");

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        CircleImageView imgAvatar = findViewById(R.id.imgAvatar);
        TextView tvName = findViewById(R.id.tvFriendName);
        btnMessage = findViewById(R.id.btnMessage);

        tvName.setText(friendName != null ? friendName : "");
        if (friendAvatar != null && !friendAvatar.isEmpty()) {
            Glide.with(this).load(friendAvatar)
                    .placeholder(R.drawable.ic_user).into(imgAvatar);
        }

        chatRepository = new ChatRepository(this);

        // Nút nhắn tin → tạo/lấy conversation → mở ChatDetailActivity
        btnMessage.setOnClickListener(v -> {
            Log.d("CHAT_DEBUG", "friendId = " + friendId);

            if (friendId == null) {
                Log.e("CHAT_DEBUG", "friendId NULL");
                return;
            }

            btnMessage.setEnabled(false);
            btnMessage.setText("Đang mở...");

            chatRepository.getOrCreateConversation(friendId, convLive);
        });

        convLive.observe(this, result -> {
            if (result == null) return;
            if (result.status == Result.Status.SUCCESS && result.data != null) {
                // Chuyển hẳn sang ChatDetailActivity
                String convJson = new Gson().toJson(result.data);
                Intent intent = new Intent(this, ChatDetailActivity.class);
                intent.putExtra(ChatDetailActivity.EXTRA_CONVERSATION_JSON, convJson);
                startActivity(intent);
                // Reset nút sau khi quay lại
                Log.d("CHAT_DEBUG", "friendId = " + friendId);
                btnMessage.setEnabled(true);
                btnMessage.setText("💬 Nhắn tin");
                Log.e("CHAT_DEBUG", "error = " + result.message);
            } else if (result.status == Result.Status.ERROR) {

            Log.e("CHAT_DEBUG", "Open chat failed: " + result.message);

            btnMessage.setEnabled(true);
            btnMessage.setText("💬 Nhắn tin");

            Toast.makeText(
                    this,
                    "Không thể mở chat: " + result.message,
                    Toast.LENGTH_LONG
            ).show();
        }
        });

        // Hiện bài viết của bạn
        if (savedInstanceState == null && friendId != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.friendPostsContainer, ProfileFeedFragment.forUser(friendId))
                    .commit();
        }
    }
}
