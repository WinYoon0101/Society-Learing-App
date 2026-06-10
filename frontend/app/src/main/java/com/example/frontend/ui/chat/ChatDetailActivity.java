package com.example.frontend.ui.chat;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;
import com.example.frontend.data.model.Conversation;
import com.google.gson.Gson;

/**
 * Activity bọc ChatDetailFragment để có thể start từ bất kỳ màn hình nào.
 * Truyền vào extras:
 *   "conversation_json" → JSON string của Conversation
 */
public class ChatDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_JSON = "conversation_json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        if (savedInstanceState == null) {
            String convJson = getIntent().getStringExtra(EXTRA_CONVERSATION_JSON);
            if (convJson == null) { finish(); return; }

            Conversation conversation = new Gson().fromJson(convJson, Conversation.class);
            ChatDetailFragment fragment = ChatDetailFragment.newInstance(conversation, null);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.chatDetailContainer, fragment)
                    .commit();
        }
    }
}