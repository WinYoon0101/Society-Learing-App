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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.User;
import com.example.frontend.data.socket.ChatSocketManager;
import com.example.frontend.utils.Constants;

public class ChatDetailFragment extends Fragment {
    private static final int PICK_FILE_REQUEST = 101;

    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttach;

    private String conversationId;
    private String currentUserId;
    private String token;
    private User otherMember;

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
        }
    }

    private void setupUI(View view) {
        TextView tvChatName = view.findViewById(R.id.tvChatName);
        ImageButton btnBack = view.findViewById(R.id.btnChatDetailBack);
        rvMessages = view.findViewById(R.id.rvMessages);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnAttach = view.findViewById(R.id.btnAttach);
        btnAttach.setOnClickListener(v -> openFilePicker());

        if (otherMember != null) {
            tvChatName.setText(otherMember.getUsername());
        }

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

        ChatSocketManager.INSTANCE.sendMessage(conversationId, messageText, null);
        etMessage.setText("");
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
