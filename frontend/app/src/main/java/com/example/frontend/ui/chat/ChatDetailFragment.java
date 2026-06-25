package com.example.frontend.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.User;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.socket.ChatSocketManager;
import com.example.frontend.ui.profile.FriendProfileActivity;
import com.example.frontend.utils.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailFragment extends Fragment {
    private static final int PICK_FILE_REQUEST = 101;

    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttach;
    private TextView tvChatName;
    private ImageButton btnChatDetailMore;

    private String conversationId;
    private String currentUserId;
    private String token;
    private User otherMember;
    private Map<String, String> nicknames;
    private boolean isGroup;
    private List<User> members;
    private String groupName;
    private String adminId;
    private List<String> mutedMessages;
    private List<String> mutedCalls;

    // Reply state
    private String replyingToMessageId;
    private View replyPreviewBar;
    private TextView tvReplyPreviewSender;
    private TextView tvReplyPreviewText;
    private ImageButton btnCancelReply;

    public static ChatDetailFragment newInstance(Conversation conversation, User otherMember) {
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("conversation", conversation);
        if (otherMember != null) {
            args.putSerializable("otherMember", otherMember);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", "");
        token = prefs.getString("JWT_TOKEN", "");

        if (getArguments() != null) {
            Conversation conversation = (Conversation) getArguments().getSerializable("conversation");
            otherMember = (User) getArguments().getSerializable("otherMember");
            if (conversation != null) {
                conversationId = conversation.getId();
                nicknames = conversation.getNicknames();
                isGroup = conversation.isGroup();
                members = conversation.getMembers();
                groupName = conversation.getName();
                adminId = conversation.getAdmin();
                mutedMessages = conversation.getMutedMessages();
                mutedCalls = conversation.getMutedCalls();
                if (otherMember == null) {
                    otherMember = getOtherMember(conversation);
                }
            }
        }

        setupUI(view);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        observeMessages();
        initializeSocket();

        if (conversationId != null) {
            viewModel.fetchMessages(conversationId);
            setupSocketListeners();
            // Chủ động join room để nhận realtime (kể cả nhóm vừa tạo / vừa được add)
            ChatSocketManager.INSTANCE.joinConversation(conversationId);
        }
    }

    private void setupUI(View view) {
        tvChatName = view.findViewById(R.id.tvChatName);
        btnChatDetailMore = view.findViewById(R.id.btnChatDetailMore);
        ImageButton btnBack = view.findViewById(R.id.btnChatDetailBack);
        rvMessages = view.findViewById(R.id.rvMessages);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnAttach = view.findViewById(R.id.btnAttach);
        btnAttach.setOnClickListener(v -> openFilePicker());

        replyPreviewBar = view.findViewById(R.id.replyPreviewBar);
        tvReplyPreviewSender = view.findViewById(R.id.tvReplyPreviewSender);
        tvReplyPreviewText = view.findViewById(R.id.tvReplyPreviewText);
        btnCancelReply = view.findViewById(R.id.btnCancelReply);
        btnCancelReply.setOnClickListener(v -> clearReply());

        if (otherMember != null) {
            tvChatName.setText(resolveDisplayName());
        }

        // Click tên → mở trang cá nhân của đối phương (group: G3 sẽ mở list thành viên)
        tvChatName.setOnClickListener(v -> {
            if (!isGroup) openFriendProfile();
        });
        // Menu 3 chấm: xem trang cá nhân / đổi biệt danh
        btnChatDetailMore.setOnClickListener(this::showChatOptionsMenu);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        messageAdapter = new MessageAdapter(currentUserId);
        messageAdapter.setOnReactionClickListener(new MessageAdapter.OnReactionClickListener() {
            @Override
            public void onLongPress(com.example.frontend.data.model.Message message, View anchor) {
                ReactionPopupHelper.show(requireContext(), anchor, message, currentUserId,
                        emoji -> ChatSocketManager.INSTANCE.reactMessage(message.getId(), emoji));
            }

            @Override
            public void onReactionChipClick(com.example.frontend.data.model.Message message, String emoji) {
                ChatSocketManager.INSTANCE.reactMessage(message.getId(), emoji);
            }

            @Override
            public void onReplyClick(com.example.frontend.data.model.Message message) {
                showReplyPreview(message);
            }

            @Override
            public void onQuoteClick(String replyToMessageId) {
                int pos = messageAdapter.indexOfMessage(replyToMessageId);
                if (pos >= 0) {
                    rvMessages.smoothScrollToPosition(pos);
                    messageAdapter.flashMessage(pos);
                } else {
                    Toast.makeText(getContext(),
                            "Tin nhắn gốc chưa được tải", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvMessages.setAdapter(messageAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void observeMessages() {
        viewModel.getMessagesResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    break;
                case SUCCESS:
                    if (result.data != null && !result.data.isEmpty()) {
                        messageAdapter.submitList(result.data);
                        rvMessages.scrollToPosition(result.data.size() - 1);
                    }
                    break;
                case ERROR:
                    Toast.makeText(getContext(), "Lỗi load tin nhắn: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void initializeSocket() {
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "Token trống, chưa login?", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("ChatDetail", "currentUserId from prefs: [" + currentUserId + "]");
        android.util.Log.d("ChatDetail", "conversationId: [" + conversationId + "]");

        // Luôn reinitialize nếu chưa connect (socket bị null sau logout)
        if (!ChatSocketManager.INSTANCE.isConnected()) {
            ChatSocketManager.INSTANCE.initialize(requireContext(), Constants.SOCKET_URL, token);
            ChatSocketManager.INSTANCE.connect();
            android.util.Log.d("ChatDetail", "Socket initialized with new token");
        }
    }

    private void setupSocketListeners() {
        ChatSocketManager.INSTANCE.setOnMessageNewListener(message -> {
            if (conversationId == null || !conversationId.equals(message.getConversationId())) {
                return kotlin.Unit.INSTANCE;
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    messageAdapter.addMessage(message);
                    rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                });
            }
            return kotlin.Unit.INSTANCE;
        });

        ChatSocketManager.INSTANCE.setOnMessageReactedListener((messageId, reactions) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        messageAdapter.updateReactions(messageId, reactions));
            }
            return kotlin.Unit.INSTANCE;
        });

        ChatSocketManager.INSTANCE.setOnErrorListener(error -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show()
                );
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatSocketManager.INSTANCE.sendMessage(conversationId, messageText, replyingToMessageId);
        etMessage.setText("");
        clearReply();
    }

    /** Hiện thanh "Đang trả lời" + lưu id message gốc để gửi kèm. */
    private void showReplyPreview(com.example.frontend.data.model.Message message) {
        if (message == null) return;
        replyingToMessageId = message.getId();

        String senderName = (message.getSender() != null && message.getSender().getUsername() != null)
                ? message.getSender().getUsername() : "";
        tvReplyPreviewSender.setText("Đang trả lời " + senderName);

        String snippet = message.getText();
        if (snippet == null || snippet.isEmpty()) {
            if (message.getMediaUrl() != null && !message.getMediaUrl().isEmpty()) {
                String mt = message.getMediaType();
                if ("image".equals(mt)) snippet = "[Ảnh]";
                else if ("video".equals(mt)) snippet = "[Video]";
                else snippet = "[Tệp]";
            } else {
                snippet = "";
            }
        }
        tvReplyPreviewText.setText(snippet);
        replyPreviewBar.setVisibility(View.VISIBLE);
    }

    /** Xoá trạng thái reply + ẩn thanh preview. */
    private void clearReply() {
        replyingToMessageId = null;
        if (replyPreviewBar != null) {
            replyPreviewBar.setVisibility(View.GONE);
        }
    }

    // ─────────────────────── Header: profile + options ───────────────────────

    /** Mở trang cá nhân của đối phương. Activity đã có sẵn (không sửa). */
    private void openFriendProfile() {
        if (otherMember == null || otherMember.getId() == null) {
            Toast.makeText(getContext(), "Không có thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), FriendProfileActivity.class);
        intent.putExtra("FRIEND_ID", otherMember.getId());
        intent.putExtra("FRIEND_NAME", otherMember.getUsername());
        intent.putExtra("FRIEND_AVATAR", otherMember.getAvatar());
        startActivity(intent);
    }

    private void showChatOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(
                isGroup ? R.menu.menu_chat_group_options : R.menu.menu_chat_options,
                popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_view_profile) {
                openFriendProfile();
            } else if (id == R.id.action_set_nickname) {
                showNicknameDialog();
            } else if (id == R.id.action_reset_nickname) {
                resetNickname();
            } else if (id == R.id.action_members) {
                openMembers();
            } else if (id == R.id.action_rename_group) {
                showRenameGroupDialog();
            } else if (id == R.id.action_add_people) {
                openAddMembers();
            } else if (id == R.id.action_leave_group) {
                confirmLeaveGroup();
            } else if (id == R.id.action_mute) {
                showMuteDialog();
            } else if (id == R.id.action_delete_chat) {
                confirmDeleteConversation();
            } else {
                return false;
            }
            return true;
        });
        popup.show();
    }

    private void openAddMembers() {
        if (conversationId == null) return;
        AddMembersBottomSheet.newInstance(conversationId, collectMemberIds())
                .show(getChildFragmentManager(), AddMembersBottomSheet.TAG);
    }

    private void openMembers() {
        GroupMembersBottomSheet.newInstance()
                .show(getChildFragmentManager(), GroupMembersBottomSheet.TAG);
    }

    private java.util.ArrayList<String> collectMemberIds() {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        if (members != null) {
            for (User m : members) {
                if (m != null && m.getId() != null) ids.add(m.getId());
            }
        }
        return ids;
    }

    /** Cập nhật state khi thêm/kick thành viên thành công. */
    void onConversationUpdated(Conversation updated) {
        if (updated == null || !isAdded()) return;
        isGroup = updated.isGroup();
        members = updated.getMembers();
        groupName = updated.getName();
        adminId = updated.getAdmin();
        if (updated.getNicknames() != null) nicknames = new HashMap<>(updated.getNicknames());
        tvChatName.setText(resolveDisplayName());
        Toast.makeText(getContext(), "Đã cập nhật thành viên", Toast.LENGTH_SHORT).show();
    }

    // ── getters cho GroupMembersBottomSheet ──
    List<User> getMembers() { return members; }
    String getCurrentUserIdValue() { return currentUserId; }
    boolean isCurrentUserAdmin() {
        return adminId != null && adminId.equals(currentUserId);
    }

    /** Kick 1 thành viên khỏi nhóm (admin). Trả về cập nhật members cho sheet refresh. */
    void kickMember(User member) {
        if (member == null || member.getId() == null || conversationId == null) return;
        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        api.kickConversationMember(conversationId, member.getId()).enqueue(new Callback<ApiResponse<Conversation>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Conversation>> call,
                                   @NonNull Response<ApiResponse<Conversation>> response) {
                if (!isAdded()) return;
                ApiResponse<Conversation> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess() && b.getData() != null) {
                    onConversationUpdated(b.getData());
                } else {
                    Toast.makeText(getContext(), "Xóa thành viên thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Conversation>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi mạng khi xóa thành viên", Toast.LENGTH_SHORT).show();
            }
        });
    }
    String getMemberDisplayName(User member) {
        if (member == null) return "";
        if (nicknames != null && member.getId() != null) {
            String nn = nicknames.get(member.getId());
            if (nn != null && !nn.trim().isEmpty()) return nn;
        }
        return member.getUsername() != null ? member.getUsername() : "";
    }

    /** Mở trang cá nhân của 1 thành viên bất kỳ (dùng từ GroupMembersBottomSheet). */
    void openProfileFor(User member) {
        if (member == null || member.getId() == null) return;
        Intent intent = new Intent(requireContext(), FriendProfileActivity.class);
        intent.putExtra("FRIEND_ID", member.getId());
        intent.putExtra("FRIEND_NAME", member.getUsername());
        intent.putExtra("FRIEND_AVATAR", member.getAvatar());
        startActivity(intent);
    }

    /** Đổi biệt danh cho 1 thành viên bất kỳ (dùng từ GroupMembersBottomSheet). */
    void showNicknameDialogFor(User member) {
        if (member == null || member.getId() == null || conversationId == null) return;
        final EditText input = new EditText(requireContext());
        input.setHint("Biệt danh");
        input.setText(getMemberDisplayName(member));
        input.setSelection(input.getText().length());

        int pad = Math.round(getResources().getDisplayMetrics().density * 20);
        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi biệt danh")
                .setView(input, pad, pad / 2, pad, 0)
                .setPositiveButton("Lưu", (d, w) ->
                        setNicknameFor(member.getId(), input.getText().toString().trim()))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // ─────────────────────── Đổi tên nhóm ───────────────────────

    private void showRenameGroupDialog() {
        if (conversationId == null) return;
        final EditText input = new EditText(requireContext());
        input.setHint("Tên nhóm");
        if (groupName != null) input.setText(groupName);
        input.setSelection(input.getText().length());

        int pad = Math.round(getResources().getDisplayMetrics().density * 20);
        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi tên nhóm")
                .setView(input, pad, pad / 2, pad, 0)
                .setPositiveButton("Lưu", (d, w) -> renameGroup(input.getText().toString().trim()))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void renameGroup(String name) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        api.renameGroup(conversationId, body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                if (!isAdded()) return;
                ApiResponse<Object> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess()) {
                    groupName = name;
                    tvChatName.setText(resolveDisplayName());
                    Toast.makeText(getContext(), "Đã đổi tên nhóm", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Đổi tên nhóm thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi mạng khi đổi tên nhóm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────── Rời nhóm ───────────────────────

    private void confirmLeaveGroup() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Rời nhóm")
                .setMessage("Bạn sẽ không nhận tin nhắn của nhóm này nữa.")
                .setPositiveButton("Rời nhóm", (d, w) -> leaveConversation())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void leaveConversation() {
        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        api.leaveConversation(conversationId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                if (!isAdded()) return;
                ApiResponse<Object> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess()) {
                    Toast.makeText(getContext(), "Đã rời nhóm", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                } else {
                    Toast.makeText(getContext(), "Rời nhóm thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi mạng khi rời nhóm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────── Xóa đoạn chat (ẩn-phía-mình) ───────────────────────

    private void confirmDeleteConversation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa đoạn chat")
                .setMessage("Đoạn chat sẽ bị ẩn khỏi danh sách của bạn. Tin nhắn mới sẽ hiện lại.")
                .setPositiveButton("Xóa", (d, w) -> deleteConversation())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void deleteConversation() {
        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        api.deleteConversation(conversationId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                if (!isAdded()) return;
                ApiResponse<Object> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess()) {
                    Toast.makeText(getContext(), "Đã xóa đoạn chat", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                } else {
                    Toast.makeText(getContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi mạng khi xóa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────── Tắt thông báo ───────────────────────

    private boolean iMuted(List<String> list) {
        return list != null && currentUserId != null && list.contains(currentUserId);
    }

    private void showMuteDialog() {
        final boolean[] checked = { iMuted(mutedMessages), iMuted(mutedCalls) };
        final String[] options = { "Tin nhắn", "Cuộc gọi" };
        new AlertDialog.Builder(requireContext())
                .setTitle("Tắt thông báo")
                .setMultiChoiceItems(options, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Lưu", (d, w) -> setMute(checked[0], checked[1]))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void setMute(boolean muteMessages, boolean muteCalls) {
        Map<String, Boolean> body = new HashMap<>();
        body.put("messages", muteMessages);
        body.put("calls", muteCalls);
        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        api.setMute(conversationId, body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                if (!isAdded()) return;
                ApiResponse<Object> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess()) {
                    // cập nhật state local
                    mutedMessages = applyMute(mutedMessages, muteMessages);
                    mutedCalls = applyMute(mutedCalls, muteCalls);
                    Toast.makeText(getContext(), "Đã cập nhật thông báo", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> applyMute(List<String> list, boolean muted) {
        List<String> result = list != null ? new java.util.ArrayList<>(list) : new java.util.ArrayList<>();
        if (muted) {
            if (currentUserId != null && !result.contains(currentUserId)) result.add(currentUserId);
        } else {
            if (currentUserId != null) result.remove(currentUserId);
        }
        return result;
    }

    /** Dialog nhập biệt danh cho đối phương. */
    private void showNicknameDialog() {
        if (otherMember == null || otherMember.getId() == null || conversationId == null) {
            Toast.makeText(getContext(), "Không thể đổi biệt danh lúc này", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(requireContext());
        input.setHint("Biệt danh");
        input.setText(tvChatName.getText());
        input.setSelection(input.getText().length());

        int pad = Math.round(getResources().getDisplayMetrics().density * 20);
        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi biệt danh")
                .setView(input, pad, pad / 2, pad, 0)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String nickname = input.getText().toString().trim();
                    setNickname(nickname);
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    /** Đặt lại biệt danh về username gốc (gửi nickname rỗng). */
    private void resetNickname() {
        if (otherMember == null || otherMember.getId() == null || conversationId == null) {
            Toast.makeText(getContext(), "Không thể đặt lại biệt danh lúc này", Toast.LENGTH_SHORT).show();
            return;
        }
        setNickname("");
    }

    private void setNickname(String nickname) {
        if (otherMember == null) return;
        setNicknameFor(otherMember.getId(), nickname);
    }

    /** Đổi biệt danh cho 1 userId bất kỳ trong conversation (1-1 hoặc thành viên group). */
    private void setNicknameFor(String targetUserId, String nickname) {
        if (targetUserId == null || conversationId == null) return;
        Map<String, String> body = new HashMap<>();
        body.put("targetUserId", targetUserId);
        body.put("nickname", nickname);

        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        api.setNickname(conversationId, body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                if (!isAdded()) return;
                ApiResponse<Object> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess()) {
                    // Đồng bộ map local để mọi nơi trong session dùng tên mới nhất
                    if (nicknames == null) nicknames = new HashMap<>();
                    if (nickname.isEmpty()) {
                        nicknames.remove(targetUserId);
                    } else {
                        nicknames.put(targetUserId, nickname);
                    }
                    // Cập nhật tên header nếu là 1-1 (đổi biệt danh đối phương)
                    if (!isGroup) {
                        tvChatName.setText(resolveDisplayName());
                    }
                    Toast.makeText(getContext(),
                            nickname.isEmpty() ? "Đã đặt lại biệt danh" : "Đã đổi biệt danh",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),
                            "Đổi biệt danh thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi mạng khi đổi biệt danh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {
                "image/*", "video/*",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST
                && resultCode == android.app.Activity.RESULT_OK
                && data != null && data.getData() != null) {
            uploadAndSendFile(data.getData());
        }
    }

    private void uploadAndSendFile(android.net.Uri fileUri) {
        try {
            android.content.ContentResolver resolver = requireContext().getContentResolver();
            String mimeType = resolver.getType(fileUri);
            if (mimeType == null) mimeType = "application/octet-stream";

            String fileName = "file_" + System.currentTimeMillis();
            android.database.Cursor cursor = resolver.query(fileUri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIdx >= 0) fileName = cursor.getString(nameIdx);
                cursor.close();
            }

            java.io.InputStream inputStream = resolver.openInputStream(fileUri);
            if (inputStream == null) return;
            byte[] bytes = inputStream.readAllBytes();
            inputStream.close();

            final String mediaType;
            if (mimeType.startsWith("image/")) mediaType = "image";
            else if (mimeType.startsWith("video/")) mediaType = "video";
            else mediaType = "document";

            okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse(mimeType), bytes);
            okhttp3.MultipartBody.Part filePart = okhttp3.MultipartBody.Part
                    .createFormData("media", fileName, requestFile);
            okhttp3.RequestBody sourceTypePart = okhttp3.RequestBody
                    .create(okhttp3.MediaType.parse("text/plain"), "message");
            okhttp3.RequestBody targetIdPart = okhttp3.RequestBody
                    .create(okhttp3.MediaType.parse("text/plain"), conversationId);

            com.example.frontend.data.remote.ApiService apiService =
                    com.example.frontend.data.remote.ApiClient.getApiService(requireContext().getApplicationContext());

            android.os.AsyncTask.execute(() -> {
                try {
                    retrofit2.Response<com.example.frontend.data.model.ApiResponse<
                            java.util.List<com.example.frontend.data.model.Media>>> response =
                            apiService.uploadChatMedia(filePart, sourceTypePart, targetIdPart)
                                    .execute();

                    if (response.isSuccessful() && response.body() != null
                            && response.body().getData() != null
                            && !response.body().getData().isEmpty()) {

                        String uploadedUrl = response.body().getData().get(0).getUrl();
                        ChatSocketManager.INSTANCE.sendMessage(
                                conversationId, "", null, uploadedUrl, mediaType);
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(),
                                            "Upload thất bại", Toast.LENGTH_SHORT).show());
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("ChatDetail", "Upload error: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi đọc file", Toast.LENGTH_SHORT).show();
            android.util.Log.e("ChatDetail", "File read error: " + e.getMessage());
        }
    }

    /** Group → tên ghép/đặt; 1-1 → biệt danh nếu có, else username gốc. */
    private String resolveDisplayName() {
        if (isGroup) {
            return ChatNameUtils.groupDisplayName(groupName, members, currentUserId);
        }
        if (otherMember == null) return "";
        if (nicknames != null && otherMember.getId() != null) {
            String nn = nicknames.get(otherMember.getId());
            if (nn != null && !nn.trim().isEmpty()) {
                return nn;
            }
        }
        return otherMember.getUsername() != null ? otherMember.getUsername() : "";
    }

    private User getOtherMember(Conversation conversation) {
        if (conversation.getMembers().isEmpty()) {
            return null;
        }
        for (User member : conversation.getMembers()) {
            if (member.getId() != null && !member.getId().equals(currentUserId)) {
                return member;
            }
        }
        return conversation.getMembers().get(0);
    }
}
