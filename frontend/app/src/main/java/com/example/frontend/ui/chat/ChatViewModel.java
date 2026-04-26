package com.example.frontend.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.Message;
import com.example.frontend.data.repository.ChatRepository;
import com.example.frontend.utils.Result;

import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repository;
    private final MutableLiveData<Result<List<Conversation>>> conversationsResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Conversation>> openConversationResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Message>>> messagesResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Message>> sendMessageResult = new MutableLiveData<>();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application.getApplicationContext());
    }

    public LiveData<Result<List<Conversation>>> getConversationsResult() {
        return conversationsResult;
    }

    public LiveData<Result<Conversation>> getOpenConversationResult() {
        return openConversationResult;
    }

    public LiveData<Result<List<Message>>> getMessagesResult() {
        return messagesResult;
    }

    public LiveData<Result<Message>> getSendMessageResult() {
        return sendMessageResult;
    }

    public void fetchConversations() {
        repository.getConversations(conversationsResult);
    }

    public void openConversation(String targetUserId) {
        repository.getOrCreateConversation(targetUserId, openConversationResult);
    }

    public void fetchMessages(String conversationId) {
        repository.getMessages(conversationId, messagesResult);
    }

    public void sendMessage(String conversationId, String text) {
        repository.sendMessage(conversationId, text, sendMessageResult);
    }
}
