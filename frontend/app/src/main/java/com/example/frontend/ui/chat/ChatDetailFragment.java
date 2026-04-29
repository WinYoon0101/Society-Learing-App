package com.example.frontend.ui.chat;

import android.content.Context;
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
    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;

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

        if (otherMember != null) {
            tvChatName.setText(otherMember.getUsername());
        }

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        messageAdapter = new MessageAdapter(currentUserId);
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
            //ChatSocketManager.INSTANCE.initialize(requireContext(), Constants.SOCKET_URL, token);
            ChatSocketManager.INSTANCE.connect();
            android.util.Log.d("ChatDetail", "Socket initialized with new token");
        }
    }

    private void setupSocketListeners() {
        ChatSocketManager.INSTANCE.setOnMessageNewListener(message -> {
            if (conversationId == null || !conversationId.equals(message.getConversationId())) {
                return null;
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    messageAdapter.addMessage(message);
                    rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                });
            }
            return null;
        });

        ChatSocketManager.INSTANCE.setOnErrorListener(error -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show()
                );
            }
            return null;
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
