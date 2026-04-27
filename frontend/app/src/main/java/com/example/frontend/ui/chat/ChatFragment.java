package com.example.frontend.ui.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.User;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private ChatViewModel viewModel;
    private ConversationAdapter conversationAdapter;
    private OnlineUserAdapter onlineUserAdapter;

    private String currentUserId;
    private String currentUserAvatar;

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

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        setupOnlineUsersRecyclerView(view);
        setupConversationsRecyclerView(view);
        observeViewModel(view);

        viewModel.fetchConversations();
    }

    private void setupOnlineUsersRecyclerView(View view) {
        RecyclerView rvOnlineUsers = view.findViewById(R.id.rvOnlineUsers);
        rvOnlineUsers.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        onlineUserAdapter = new OnlineUserAdapter(currentUserId, currentUserAvatar, user -> {
            // Khi bấm vào user online → mở/tạo conversation với user đó
            viewModel.openConversation(user.getId());
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
                        onlineUserAdapter.submitList(extractOnlineUsers(conversations));
                    } else {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvConversations.setVisibility(View.GONE);
                        onlineUserAdapter.submitList(new ArrayList<>());
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
                ChatDetailFragment fragment = ChatDetailFragment.newInstance(result.data, null);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    // Lấy danh sách user online từ các conversations (không bao gồm current user)
    private List<User> extractOnlineUsers(List<Conversation> conversations) {
        List<User> onlineUsers = new ArrayList<>();
        List<String> seenIds = new ArrayList<>();

        for (Conversation conv : conversations) {
            if (conv.getMembers() == null) continue;
            for (User member : conv.getMembers()) {
                if (member.getId() == null) continue;
                if (member.getId().equals(currentUserId)) continue;
                if (seenIds.contains(member.getId())) continue;
                if (member.isActive()) {
                    seenIds.add(member.getId());
                    onlineUsers.add(member);
                }
            }
        }
        return onlineUsers;
    }
}
