package com.example.frontend.ui.profile;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.FriendStatus;
import com.example.frontend.data.model.Media;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.repository.ChatRepository;
import com.example.frontend.data.repository.FriendRepository;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.ui.chat.ChatDetailActivity;
import com.example.frontend.ui.feed.CreatePostActivity;
import com.example.frontend.ui.main.HomeActivity;
import com.example.frontend.utils.Constants;
import com.example.frontend.utils.Result;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendProfileActivity extends AppCompatActivity {
    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private ApiService apiService;
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
    TextView txtAll, txtPic, txtFriends;
    private FrameLayout friendPostsContainer;
    private MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
    private MutableLiveData<Result<List<Friend>>> friendLiveData = new MutableLiveData<>();
    private MutableLiveData<Result<List<Friend>>> pendingLiveData = new MutableLiveData<>();
    private MutableLiveData<Result<Object>> actionLiveData = new MutableLiveData<>();
    private User currentUser;
    private String friendId;
    private FrameLayout contentContainer;
    private MutableLiveData<Result<List<Friend>>> profileFriendsLiveData = new MutableLiveData<>();
    private enum FriendState {
        NONE,
        SENT,
        RECEIVED,
        FRIEND
    }
    private FriendState friendState = FriendState.NONE;
    private MutableLiveData<Result<FriendStatus>> friendStatusLiveData=new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        imgCover = findViewById(R.id.imgCover);
        imgAvatar = findViewById(R.id.imgAvatar);

        tvFriendName = findViewById(R.id.tvFriendName);
        tvBio = findViewById(R.id.tvBio);
        tvStats = findViewById(R.id.tvStats);

        btnFriend = findViewById(R.id.btnAddFriend);
        btnMessage = findViewById(R.id.btnMessage);

        btnBack = findViewById(R.id.btnBack);

        tabAll = findViewById(R.id.tabAll);
        tabPic = findViewById(R.id.tabPic);
        tabFriends = findViewById(R.id.tabFriends);
        txtAll = findViewById(R.id.txtAll);
        txtPic = findViewById(R.id.txtPic);
        txtFriends = findViewById(R.id.txtFriends);
        contentContainer = findViewById(R.id.contentContainer);

        friendId = getIntent().getStringExtra("USER_ID");
        if (friendId == null) {
            friendId = getIntent().getStringExtra("FRIEND_ID");
        }
        if(friendId == null){
            finish();
            return;
        }
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(FriendProfileActivity.this, HomeActivity.class);
            intent.putExtra("SELECT_TAB", 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);

            finish();

        });

        btnMessage = findViewById(R.id.btnMessage);

        chatRepository = new ChatRepository(this);
        friendRepository = new FriendRepository(this);
        userRepository = new UserRepository(this);
        apiService = ApiClient.getApiService(this);
        init();
        // Nút nhắn tin → tạo/lấy conversation → mở ChatDetailActivity
        btnMessage.setOnClickListener(v -> {
            Log.d("CHAT_DEBUG", "friendId = " + friendId);

            if (friendId == null) {
                Log.e("CHAT_DEBUG", "friendId NULL");
                return;
            }

            btnMessage.setEnabled(false);

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

        actionLiveData.observe(this,result->{

            if(result==null) return;

            if(result.status==Result.Status.SUCCESS){

                loadFriendStatus();

            }

        });
        tabAll.setOnClickListener(v -> {selectTab(tabAll);showTabAll();});
        tabFriends.setOnClickListener(v -> {selectTab(tabFriends);showTabFriends();});
        tabPic.setOnClickListener(v -> {selectTab(tabPic);showTabPictures();});
        init();
        selectTab(tabAll);
        showTabAll();
    }
    private void init(){
        loadUser();
        loadFriendStatus();
        initClick();
        selectTab(tabAll);
        showTabAll();
    }
    private void loadUser(){userRepository.getUserById(friendId,userLiveData);
        userLiveData.observe(this,result->{
            if(result==null) return;
            if(result.status==Result.Status.SUCCESS){
                currentUser=result.data;
                bindUser();
                selectTab(tabAll);
                showTabAll();
            }
            else if(result.status==Result.Status.ERROR){
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void bindUser(){
        if(currentUser==null) return;
        tvFriendName.setText(currentUser.getUsername());
        tvBio.setText(currentUser.getBio()==null ? "" : currentUser.getBio());
        Glide.with(this).load(currentUser.getAvatar()).placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(imgAvatar);
        // 2. Xử lý load Ảnh bìa (Cover)
        String coverUrl = currentUser.getCover();
        if (coverUrl == null || coverUrl.trim().isEmpty()) {
            // Nếu rỗng hoặc null, set trực tiếp ảnh mặc định
            imgCover.setImageResource(R.drawable.anhbia);
        } else {
            // Nếu có URL, dùng Glide load, nếu URL lỗi hoặc đang load thì dùng anhbia
            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.anhbia)
                    .error(R.drawable.anhbia)
                    .into(imgCover);
        }
        tvStats.setText(currentUser.getFriendCount()+" bạn bè");
    }
    private void loadFriendStatus(){
        friendRepository.checkFriendStatus(friendId, friendStatusLiveData);
        friendStatusLiveData.observe(this,result->{
            if(result==null) return;
            if(result.status!=Result.Status.SUCCESS) return;
            String state=result.data.getState();
            switch(state){
                case "friend":
                    friendState=FriendState.FRIEND;
                    break;

                case "sent":
                    friendState=FriendState.SENT;
                    break;

                case "received":
                    friendState=FriendState.RECEIVED;
                    break;

                default:
                    friendState=FriendState.NONE;

            }
            updateFriendButton();
        });
    }
    private void updateFriendButton(){
        switch(friendState){
            case FRIEND:
                btnFriend.setEnabled(true);
                btnFriend.setText("Bạn bè");
                break;

            case SENT:
                btnFriend.setEnabled(true);
                btnFriend.setText("Đã gửi lời mời");
                break;

            case RECEIVED:
                btnFriend.setEnabled(true);
                btnFriend.setText("Chờ xác nhận");
                break;

            default:
                btnFriend.setEnabled(true);
                btnFriend.setText("Thêm bạn bè");

        }

    }
    private void initClick() {
        btnFriend.setOnClickListener(v->{
            switch(friendState){
                case NONE:
                    friendRepository.sendFriendRequest(friendId, actionLiveData);
                    break;

                case SENT:
                    showCancelRequestDialog();
                    break;

                case RECEIVED:
                    showAcceptRejectBottomSheet();
                    break;

                case FRIEND:
                    showFriendBottomSheet();
                    break;
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
    private void selectTab(LinearLayout selectedTab) {
        tabAll.setBackgroundResource(android.R.color.transparent);
        tabPic.setBackgroundResource(android.R.color.transparent);
        tabFriends.setBackgroundResource(android.R.color.transparent);

        txtAll.setTextColor(Color.parseColor("#6B7280"));
        txtPic.setTextColor(Color.parseColor("#6B7280"));
        txtFriends.setTextColor(Color.parseColor("#6B7280"));

        selectedTab.setBackgroundResource(R.drawable.bg_tab_select);

        if (selectedTab == tabAll) {
            txtAll.setTextColor(Color.parseColor("#10B981"));
        } else if (selectedTab == tabPic) {
            txtPic.setTextColor(Color.parseColor("#10B981"));
        } else if (selectedTab == tabFriends) {
            txtFriends.setTextColor(Color.parseColor("#10B981"));
        }
    }
    private void showTabAll() {
        contentContainer.removeAllViews();
        View v = LayoutInflater.from(this).inflate(R.layout.fragment_profile_all, contentContainer, true);
        TextView tvLocation = v.findViewById(R.id.tvLocation);
        TextView tvHometown = v.findViewById(R.id.tvHometown);
        TextView tvBirthday = v.findViewById(R.id.tvBirthday);
        TextView tvGender = v.findViewById(R.id.tvGender);
        MaterialButton btnEdit = v.findViewById(R.id.btnEditDetails);
        View createPost = v.findViewById(R.id.btnOpenCreatePost);
        ImageView imgPostAvatar = v.findViewById(R.id.imgPostAvatar);

        btnEdit.setVisibility(View.GONE);
        createPost.setVisibility(View.GONE);

        // Hiển thị thông tin user
        if(currentUser!=null){
            Glide.with(this).load(currentUser.getAvatar()).placeholder(R.drawable.ic_profile).into(imgPostAvatar);
            if(currentUser.getLocation()!=null && !currentUser.getLocation().isEmpty()){
                tvLocation.setText("Đang ở " + currentUser.getLocation());
            }else{
                tvLocation.setVisibility(View.GONE);
            }
            if(currentUser.getHometown()!=null && !currentUser.getHometown().isEmpty()){
                tvHometown.setText("Đến từ " + currentUser.getHometown());
            }else{
                tvHometown.setVisibility(View.GONE);
            }
            if(currentUser.getBirthday()!=null){
                tvBirthday.setText(currentUser.getBirthday());
            }else{
                tvBirthday.setVisibility(View.GONE);
            }
            if(currentUser.getGender()!=null){
                tvGender.setText(currentUser.getGender());
            }else{
                tvGender.setVisibility(View.GONE);
            }
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.feedContainer, ProfileFeedFragment.forUser(friendId)).commit();
    }

    private void showTabFriends() {
        contentContainer.removeAllViews();
        View v = LayoutInflater.from(this).inflate(R.layout.fragment_profile_friends, contentContainer, true);
        RecyclerView rv = v.findViewById(R.id.rvFriends);
        TextView tvEmpty = v.findViewById(R.id.tvNoFriends);
        rv.setLayoutManager(new LinearLayoutManager(this));
        friendRepository.getFriendsByUser(friendId, friendLiveData);
        friendLiveData.observe(this, result -> {
            if (result == null) return;
            if (result.status == Result.Status.SUCCESS) {
                List<Friend> list = result.data;
                if (list == null || list.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                }
                else {
                    tvEmpty.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                    rv.setAdapter(new FriendAdapter(list, friend -> {
                        String myId = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("USER_ID", "");
                        if(friend.getId().equals(myId)){
                            Intent intent = new Intent(FriendProfileActivity.this, HomeActivity.class);
                            intent.putExtra("SELECT_TAB", 5);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                        }else{
                            Intent intent = new Intent(FriendProfileActivity.this, FriendProfileActivity.class);
                            intent.putExtra("FRIEND_ID", friend.getId());
                            startActivity(intent);
                        }
                        finish();
                    }));
                }
            } else if (result.status == Result.Status.ERROR) {
                Toast.makeText(FriendProfileActivity.this, result.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTabPictures() {
        contentContainer.removeAllViews();
        View v = LayoutInflater.from(this).inflate(R.layout.fragment_profile_picture, contentContainer, true);
        RecyclerView rv = v.findViewById(R.id.rvPhotos);
        TextView tvEmpty = v.findViewById(R.id.tvNoPhotos);
        TextView txtFriend = v.findViewById(R.id.txtFriend);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        if (currentUser != null) {
            txtFriend.setText("Ảnh của " + currentUser.getUsername());
        }
        final String coverUrl = currentUser != null ? currentUser.getCover() : null;
        apiService.getUserMedia(friendId, "image").enqueue(new Callback<ApiResponse<List<Media>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Media>>> call, Response<ApiResponse<List<Media>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Media> photos = response.body().getData();
                    if (photos == null) {
                        photos = new ArrayList<>();
                    }
                    if (coverUrl != null && !coverUrl.isEmpty()) {
                        Media cover = new Media();
                        cover.setUrl(coverUrl);
                        cover.setFileType("cover");
                        photos.add(0, cover);
                    }
                    if (photos.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    }
                    else {
                        tvEmpty.setVisibility(View.GONE);
                        rv.setVisibility(View.VISIBLE);
                        rv.setAdapter(new PhotoAdapter(photos));
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Media>>> call, Throwable t) {
                Toast.makeText(FriendProfileActivity.this, "Lỗi tải ảnh", Toast.LENGTH_SHORT).show();}
        });
    }
    private void showCancelRequestDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hủy lời mời")
                .setMessage("Bạn có chắc chắn muốn hủy lời mời kết bạn không?")
                .setNegativeButton("Không", null)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    friendRepository.declineFriendRequest(friendId, actionLiveData);
                }).show();
    }

    private void showAcceptRejectBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_pending_action, null);
        dialog.setContentView(view);
        LinearLayout btnAccept = view.findViewById(R.id.btnAcceptFriend);
        LinearLayout btnDecline = view.findViewById(R.id.btnDeclineFriend);
        btnAccept.setOnClickListener(v -> {dialog.dismiss();
            friendRepository.acceptFriendRequest(friendId, actionLiveData);
        });
        btnDecline.setOnClickListener(v -> {
            dialog.dismiss();
            friendRepository.declineFriendRequest(friendId, actionLiveData);
        });
        dialog.show();
    }
}
