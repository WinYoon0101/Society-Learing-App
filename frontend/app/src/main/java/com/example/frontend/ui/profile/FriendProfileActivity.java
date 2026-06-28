package com.example.frontend.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.repository.ChatRepository;
import com.example.frontend.data.repository.FriendRepository;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.ui.chat.ChatDetailActivity;
import com.example.frontend.utils.Constants;
import com.example.frontend.utils.Result;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendProfileActivity extends AppCompatActivity {
    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private MutableLiveData<Result<Conversation>> convLive = new MutableLiveData<>();
    private ImageView imgCover;
    private ImageView imgAvatar;
    private TextView tvFriendName;
    private TextView tvBio;
    private TextView tvStats;
    private TextView tvLocation;
    private TextView tvHometown;
    private TextView tvBirthday;
    private TextView tvGender;
    private MaterialButton btnFriend;
    private MaterialButton btnMessage;
    private ImageButton btnBack;
    private LinearLayout tabAll;
    private LinearLayout tabPic;
    private LinearLayout tabFriends;
    private FrameLayout friendPostsContainer;
    private MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
    private MutableLiveData<Result<List<Friend>>> friendLiveData = new MutableLiveData<>();
    private MutableLiveData<Result<List<Friend>>> pendingLiveData = new MutableLiveData<>();
    private MutableLiveData<Result<Object>> actionLiveData = new MutableLiveData<>();
    private User currentUser;
    private String friendId;
    private boolean isFriend = false;
    private boolean isPending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        imgCover = findViewById(R.id.imgCover);
        imgAvatar = findViewById(R.id.imgAvatar);

        tvFriendName = findViewById(R.id.tvFriendName);
        tvBio = findViewById(R.id.tvBio);
        tvStats = findViewById(R.id.tvStats);

        tvLocation = findViewById(R.id.tvLocation);
        tvHometown = findViewById(R.id.tvHometown);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvGender = findViewById(R.id.tvGender);

        btnFriend = findViewById(R.id.btnAddFriend);
        btnMessage = findViewById(R.id.btnMessage);

        btnBack = findViewById(R.id.btnBack);

        tabAll = findViewById(R.id.tabAll);
        tabPic = findViewById(R.id.tabPic);
        tabFriends = findViewById(R.id.tabFriends);

        friendPostsContainer = findViewById(R.id.friendPostsContainer);
        friendId = getIntent().getStringExtra("USER_ID");
        if (friendId == null) {
            friendId = getIntent().getStringExtra("FRIEND_ID");
        }
        if(friendId == null){
            finish();
            return;
        }
        btnBack.setOnClickListener(v -> finish());

        btnMessage = findViewById(R.id.btnMessage);

        chatRepository = new ChatRepository(this);
        friendRepository = new FriendRepository(this);
        userRepository = new UserRepository(this);
        init();
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

        actionLiveData.observe(this,result->{

            if(result==null) return;

            if(result.status==Result.Status.SUCCESS){

                loadFriendStatus();

            }

        });

    }
    private void init(){
        loadUser();
        loadFriendStatus();
        initClick();
    }
    private void loadUser(){userRepository.getUserById(friendId,userLiveData);
        userLiveData.observe(this,result->{

            if(result==null) return;

            if(result.status==Result.Status.SUCCESS){

                currentUser=result.data;

                bindUser();

            }
            else if(result.status==Result.Status.ERROR){

                Toast.makeText(this,
                        result.message,
                        Toast.LENGTH_SHORT).show();

            }

        });
    }
    private void bindUser(){
        if(currentUser==null) return;
        tvFriendName.setText(currentUser.getUsername());
        tvBio.setText(
                currentUser.getBio()==null ?
                        "" :
                        currentUser.getBio()
        );
        tvLocation.setText(currentUser.getLocation());
        tvHometown.setText(currentUser.getHometown());
        tvBirthday.setText(currentUser.getBirthday());
        tvGender.setText(currentUser.getGender());
        Glide.with(this).load(currentUser.getAvatar()).placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(imgAvatar);
        Glide.with(this).load(currentUser.getCover()).placeholder(R.drawable.bg_cover_default).error(R.drawable.bg_cover_default).into(imgCover);
        tvStats.setText(currentUser.getFriendCount()+" bạn bè");
    }
    private void loadFriendStatus() {
        friendRepository.getFriends(friendLiveData);
        friendRepository.getPendingRequests(pendingLiveData);
        friendLiveData.observe(this,result->{

            if(result==null) return;

            if(result.status==Result.Status.SUCCESS){

                isFriend=false;

                for(Friend f:result.data){

                    if(f.getId().equals(friendId)){

                        isFriend=true;

                        break;

                    }

                }

                updateFriendButton();

            }

            else if(result.status==Result.Status.ERROR){

                Log.e("FRIEND",result.message);

            }

        });

        pendingLiveData.observe(this, result -> {
            if (result.status != Result.Status.SUCCESS || result.data == null)
                return;
            isPending = false;
            for (Friend f : result.data) {
                if (f.getId().equals(friendId)) {
                    isPending = true;
                    break;
                }
            }
            updateFriendButton();
        });
    }
    private void updateFriendButton() {
        if (isFriend) {
            btnFriend.setText("Bạn bè");
            btnFriend.setBackgroundTintList(getColorStateList(R.color.green));
            btnFriend.setTextColor(getColor(R.color.white));
        }
        else if (isPending) {
            btnFriend.setText("Đã gửi lời mời");
            btnFriend.setEnabled(false);
            btnFriend.setBackgroundTintList(getColorStateList(R.color.gray));
        }

        else {
            btnFriend.setEnabled(true);
            btnFriend.setText("Thêm bạn bè");
            btnFriend.setBackgroundTintList(getColorStateList(R.color.gray));
            btnFriend.setTextColor(getColor(R.color.black));
        }
    }
    private void initClick() {
        btnFriend.setOnClickListener(v -> {
            if (isFriend) {
                showFriendBottomSheet();
            }
            else if (!isPending) {
                friendRepository.sendFriendRequest(friendId, actionLiveData);
            }
        });
    }
    private void showFriendBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_friend_action, null);
        dialog.setContentView(view);
        LinearLayout btnRemove = view.findViewById(R.id.btnRemoveFriend);
        btnRemove.setOnClickListener(v -> {
            dialog.dismiss();
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Hủy kết bạn")
                    .setMessage("Bạn có chắc chắn muốn hủy kết bạn với " + currentUser.getUsername() + " không?")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xác nhận", (d, which) -> {
                        friendRepository.removeFriend(friendId, actionLiveData);
                    })
                    .show();
        });
        dialog.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadUser();
        loadFriendStatus();
    }
}
