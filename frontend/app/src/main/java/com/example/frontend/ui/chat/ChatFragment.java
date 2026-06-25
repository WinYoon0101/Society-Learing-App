package com.example.frontend.ui.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.socket.ChatSocketManager;
import com.example.frontend.utils.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private static final String LOG_TAG = "ChatFragment";

    private ChatViewModel viewModel;
    private ConversationAdapter conversationAdapter;
    private OnlineUserAdapter onlineUserAdapter;

    private String currentUserId;
    private String currentUserAvatar;

    // Online rail state
    private final Set<String> onlineIds = new HashSet<>();
    private final List<Friend> friends = new ArrayList<>();
    private Call<ApiResponse<List<Friend>>> friendsCall;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", "");
        currentUserAvatar = prefs.getString("USER_AVATAR", null);
        String token = prefs.getString("JWT_TOKEN", "");

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        setupOnlineUsersRecyclerView(view);
        setupConversationsRecyclerView(view);
        setupNewChatFab(view);
        observeViewModel(view);

        setupSocketForPresence(token);
        fetchFriendsForRail();

        viewModel.fetchConversations();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Xin snapshot online mỗi lần quay lại màn list — nếu socket chưa connect,
        // setOnConnectedListener bên dưới sẽ tự xin lại lúc connect thành công.
        ChatSocketManager.INSTANCE.getOnlineUsers();
    }

    @Override
    public void onDestroyView() {
        ChatSocketManager.INSTANCE.clearOnlineRailListeners();
        if (friendsCall != null) {
            friendsCall.cancel();
            friendsCall = null;
        }
        super.onDestroyView();
    }

    public void openChatWith(String userId) {
        if (userId == null || userId.isEmpty() || viewModel == null) return;
        viewModel.openConversation(userId);
    }

    /** Mở thẳng 1 conversation (vd group vừa tạo). */
    public void openConversation(Conversation conversation) {
        if (conversation == null || !isAdded()) return;
        ChatDetailFragment fragment = ChatDetailFragment.newInstance(conversation, null);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /** Mở sheet tạo nhóm (child của ChatFragment để callback openConversation hoạt động). */
    public void openCreateGroup() {
        CreateGroupBottomSheet.newInstance()
                .show(getChildFragmentManager(), CreateGroupBottomSheet.TAG);
    }

    private void setupNewChatFab(View view) {
        com.google.android.material.floatingactionbutton.FloatingActionButton fab =
                view.findViewById(R.id.fabNewChat);
        fab.setOnClickListener(v ->
                SelectFriendBottomSheet.newInstance()
                        .show(getChildFragmentManager(), SelectFriendBottomSheet.TAG));
    }

    private void setupOnlineUsersRecyclerView(View view) {
        RecyclerView rvOnlineUsers = view.findViewById(R.id.rvOnlineUsers);
        rvOnlineUsers.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        onlineUserAdapter = new OnlineUserAdapter(currentUserId, currentUserAvatar, user -> {
            if (user != null && user.getId() != null) {
                viewModel.openConversation(user.getId());
            }
        });
        rvOnlineUsers.setAdapter(onlineUserAdapter);
    }

    private void setupConversationsRecyclerView(View view) {
        RecyclerView rvConversations = view.findViewById(R.id.rvConversations);

        conversationAdapter = new ConversationAdapter(currentUserId, (conversation, otherMember) -> {
            ChatDetailFragment fragment = ChatDetailFragment.newInstance(conversation, otherMember);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvConversations.setAdapter(conversationAdapter);
    }

    private void observeViewModel(View view) {
        View layoutEmpty = view.findViewById(R.id.layoutEmptyChat);
        RecyclerView rvConversations = view.findViewById(R.id.rvConversations);

        viewModel.getConversationsResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    break;
                case SUCCESS:
                    List<Conversation> conversations = result.data;
                    if (conversations != null && !conversations.isEmpty()) {
                        layoutEmpty.setVisibility(View.GONE);
                        rvConversations.setVisibility(View.VISIBLE);
                        conversationAdapter.submitList(conversations);
                        // Đảm bảo chấm online đúng ngay khi list vừa load (Task A)
                        refreshOnlineRail();
                    } else {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvConversations.setVisibility(View.GONE);
                    }
                    break;
                case ERROR:
                    Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getOpenConversationResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.status == com.example.frontend.utils.Result.Status.SUCCESS && result.data != null) {
                // Tiêu thụ sự kiện ngay để observer không phát lại khi quay về từ back stack
                viewModel.clearOpenConversationResult();
                ChatDetailFragment fragment = ChatDetailFragment.newInstance(result.data, null);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    // ─────────────────────── Socket presence ───────────────────────

    private void setupSocketForPresence(String token) {
        ChatSocketManager socket = ChatSocketManager.INSTANCE;

        if (!socket.isConnected()) {
            socket.initialize(requireContext().getApplicationContext(), Constants.SOCKET_URL, token);
            socket.connect();
        }

        socket.setOnConnectedListener(() -> {
            runOnUi(() -> ChatSocketManager.INSTANCE.getOnlineUsers());
            return kotlin.Unit.INSTANCE;
        });

        socket.setOnOnlineUsersListener(ids -> {
            runOnUi(() -> {
                onlineIds.clear();
                if (ids != null) onlineIds.addAll(ids);
                refreshOnlineRail();
            });
            return kotlin.Unit.INSTANCE;
        });

        socket.setOnUserOnlineListener(userId -> {
            runOnUi(() -> {
                if (userId != null && !userId.equals(currentUserId)) {
                    onlineIds.add(userId);
                    refreshOnlineRail();
                }
            });
            return kotlin.Unit.INSTANCE;
        });

        socket.setOnUserOfflineListener(userId -> {
            runOnUi(() -> {
                if (userId != null) {
                    onlineIds.remove(userId);
                    refreshOnlineRail();
                }
            });
            return kotlin.Unit.INSTANCE;
        });

        // Socket có thể đã connect từ trước → xin snapshot ngay
        if (socket.isConnected()) {
            socket.getOnlineUsers();
        }
    }

    private void runOnUi(Runnable r) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (isAdded()) r.run();
        });
    }

    // ─────────────────────── Friends source ───────────────────────

    private void fetchFriendsForRail() {
        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        friendsCall = api.getFriends();
        friendsCall.enqueue(new Callback<ApiResponse<List<Friend>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Friend>>> call,
                                   @NonNull Response<ApiResponse<List<Friend>>> response) {
                if (!isAdded()) return;
                ApiResponse<List<Friend>> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null) {
                    friends.clear();
                    friends.addAll(body.getData());
                    refreshOnlineRail();
                } else {
                    Log.w(LOG_TAG, "getFriends failed: " + (body != null ? body.getMessage() : "null body"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Friend>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(LOG_TAG, "getFriends error", t);
            }
        });
    }

    /** Rail = bạn bè, online lên đầu. Self slot do adapter render ở vị trí 0. */
    private void refreshOnlineRail() {
        if (onlineUserAdapter == null) return;
        List<User> railOnline = new ArrayList<>();
        List<User> railOffline = new ArrayList<>();
        for (Friend f : friends) {
            if (f.getId() == null) continue;
            if (f.getId().equals(currentUserId)) continue;
            User u = new User(f.getId(), f.getUsername(), f.getAvatar());
            if (onlineIds.contains(f.getId())) {
                railOnline.add(u);
            } else {
                railOffline.add(u);
            }
        }
        List<User> rail = new ArrayList<>(railOnline.size() + railOffline.size());
        rail.addAll(railOnline);
        rail.addAll(railOffline);
        onlineUserAdapter.setOnlineIds(new HashSet<>(onlineIds));
        onlineUserAdapter.submitList(rail);

        // Đồng bộ chấm online cho list "Tin nhắn gần đây" (Task A)
        if (conversationAdapter != null) {
            conversationAdapter.setOnlineIds(new HashSet<>(onlineIds));
        }
    }
}