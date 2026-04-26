package com.example.frontend.data.socket

import android.content.Context
import android.content.SharedPreferences
import com.example.frontend.utils.Constants

/**
 * Hướng dẫn sử dụng ChatSocketManager
 *
 * STEP 1: Khởi tạo socket (trong Activity/Application)
 * ════════════════════════════════════════════════════════════
 * ChatSocketManager.initialize(context, Constants.SOCKET_URL)
 *
 * STEP 2: Kết nối với token
 * ════════════════════════════════════════════════════════════
 * val token = sharedPref.getString("token", "") ?: ""
 * ChatSocketManager.connect(token)
 *
 * STEP 3: Đăng ký listeners
 * ════════════════════════════════════════════════════════════
 * ChatSocketManager.setOnMessageNewListener { message ->
 *     // Cập nhật UI với tin nhắn mới
 *     Log.d("Chat", "Tin mới: ${message.text}")
 * }
 *
 * ChatSocketManager.setOnUserOnlineListener { userId ->
 *     Log.d("Chat", "User online: $userId")
 * }
 *
 * STEP 4: Emit events từ UI
 * ════════════════════════════════════════════════════════════
 * // Gửi tin nhắn
 * ChatSocketManager.sendMessage(conversationId, "Hello", replyTo = null)
 *
 * // Thả cảm xúc
 * ChatSocketManager.reactMessage(messageId, "❤️")
 *
 * // Gõ tin nhắn (gọi khi người dùng bắt đầu gõ)
 * ChatSocketManager.typingStart(conversationId)
 *
 * // Dừng gõ
 * ChatSocketManager.typingStop(conversationId)
 *
 * STEP 5: Disconnect (trong onDestroy)
 * ════════════════════════════════════════════════════════════
 * ChatSocketManager.disconnect()
 *
 *
 * 🔥 EXAMPLE: Sử dụng trong ChatDetailActivity
 * ════════════════════════════════════════════════════════════
 */

object ChatSocketUsageExample {
    /*
    class ChatDetailActivity : AppCompatActivity() {
        private lateinit var messageAdapter: MessageAdapter
        private val messages = mutableListOf<Message>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // 1. Initialize socket
            ChatSocketManager.initialize(this, Constants.SOCKET_URL)

            // 2. Get token & connect
            val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val token = sharedPref.getString("token", "") ?: ""
            ChatSocketManager.connect(token)

            // 3. Setup listeners
            setupChatListeners()

            // 4. Setup UI
            messageAdapter = MessageAdapter(messages)
            recyclerViewMessages.adapter = messageAdapter

            // 5. Send message on button click
            btnSend.setOnClickListener {
                val text = editTextMessage.text.toString().trim()
                if (text.isNotEmpty()) {
                    ChatSocketManager.sendMessage(conversationId, text)
                    editTextMessage.text.clear()
                }
            }
        }

        private fun setupChatListeners() {
            // Nhận tin nhắn mới
            ChatSocketManager.setOnMessageNewListener { message ->
                messages.add(message)
                messageAdapter.notifyItemInserted(messages.size - 1)
                recyclerViewMessages.scrollToPosition(messages.size - 1)
            }

            // Nhận cảm xúc
            ChatSocketManager.setOnMessageReactedListener { messageId, reactions ->
                val index = messages.indexOfFirst { it.id == messageId }
                if (index != -1) {
                    messages[index] = messages[index].copy(reactions = reactions)
                    messageAdapter.notifyItemChanged(index)
                }
            }

            // User online
            ChatSocketManager.setOnUserOnlineListener { userId ->
                if (userId == chatPartner.id) {
                    updateUserStatus(online = true)
                }
            }

            // User offline
            ChatSocketManager.setOnUserOfflineListener { userId ->
                if (userId == chatPartner.id) {
                    updateUserStatus(online = false)
                }
            }

            // User đang gõ
            ChatSocketManager.setOnTypingStartListener { userId, username ->
                if (userId == chatPartner.id) {
                    showTypingIndicator("$username đang gõ...")
                }
            }

            // Dừng gõ
            ChatSocketManager.setOnTypingStopListener { userId ->
                if (userId == chatPartner.id) {
                    hideTypingIndicator()
                }
            }
        }

        override fun onDestroy() {
            ChatSocketManager.disconnect()
            super.onDestroy()
        }
    }
    */
}
