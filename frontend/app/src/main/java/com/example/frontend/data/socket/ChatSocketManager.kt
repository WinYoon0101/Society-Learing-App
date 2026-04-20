package com.example.frontend.data.socket

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import com.google.gson.Gson
import com.example.frontend.data.model.Message
import com.example.frontend.data.model.Reaction
import java.net.URISyntaxException

object ChatSocketManager {
    private var socket: Socket? = null
    private val gson = Gson()
    private val TAG = "ChatSocket"

    // Listeners
    private var onMessageNew: ((Message) -> Unit)? = null
    private var onMessageReacted: ((String, List<Reaction>) -> Unit)? = null
    private var onMessageDeleted: ((String) -> Unit)? = null
    private var onUserOnline: ((String) -> Unit)? = null
    private var onUserOffline: ((String) -> Unit)? = null
    private var onTypingStart: ((String, String) -> Unit)? = null
    private var onTypingStop: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun initialize(context: Context, serverUrl: String) {
        if (socket != null && socket!!.connected()) {
            Log.w(TAG, "Socket already connected")
            return
        }

        try {
            val opts = IO.Options()
            opts.reconnection = true
            opts.reconnectionDelay = 1000
            opts.reconnectionDelayMax = 5000
            opts.reconnectionAttempts = 5

            socket = IO.socket(serverUrl, opts)

            setupListeners()

            Log.d(TAG, "Socket initialized")
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Socket URI error: ${e.message}")
        }
    }

    fun connect(token: String) {
        if (socket == null) {
            Log.e(TAG, "Socket not initialized. Call initialize() first")
            return
        }

        try {
            socket?.io()?.options?.extraHeaders = mapOf("Authorization" to "Bearer $token")
            socket?.connect()
            Log.d(TAG, "Socket connecting...")
        } catch (e: Exception) {
            Log.e(TAG, "Connect error: ${e.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        Log.d(TAG, "Socket disconnected")
    }

    private fun setupListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "✅ Connected to server")
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "❌ Disconnected from server")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull() as? Exception
            Log.e(TAG, "Connection error: ${error?.message}")
            onError?.invoke(error?.message ?: "Connection error")
        }

        // ──────────────── Message Events ────────────────
        socket?.on("message:new") { args ->
            try {
                val data = args[0] as JSONObject
                val message = gson.fromJson(data.toString(), Message::class.java)
                Log.d(TAG, "New message: ${message.text}")
                onMessageNew?.invoke(message)
            } catch (e: Exception) {
                Log.e(TAG, "Parse message:new error: ${e.message}")
            }
        }

        socket?.on("message:reacted") { args ->
            try {
                val data = args[0] as JSONObject
                val messageId = data.getString("messageId")
                val reactionsArray = data.getJSONArray("reactions")
                val reactions = mutableListOf<Reaction>()
                for (i in 0 until reactionsArray.length()) {
                    val reaction = gson.fromJson(
                        reactionsArray.getJSONObject(i).toString(),
                        Reaction::class.java
                    )
                    reactions.add(reaction)
                }
                Log.d(TAG, "Message reacted: $messageId with ${reactions.size} reactions")
                onMessageReacted?.invoke(messageId, reactions)
            } catch (e: Exception) {
                Log.e(TAG, "Parse message:reacted error: ${e.message}")
            }
        }

        socket?.on("message:deleted") { args ->
            try {
                val data = args[0] as JSONObject
                val messageId = data.getString("messageId")
                Log.d(TAG, "Message deleted: $messageId")
                onMessageDeleted?.invoke(messageId)
            } catch (e: Exception) {
                Log.e(TAG, "Parse message:deleted error: ${e.message}")
            }
        }

        // ──────────────── User Status Events ────────────────
        socket?.on("user:online") { args ->
            try {
                val data = args[0] as JSONObject
                val userId = data.getString("userId")
                Log.d(TAG, "User online: $userId")
                onUserOnline?.invoke(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Parse user:online error: ${e.message}")
            }
        }

        socket?.on("user:offline") { args ->
            try {
                val data = args[0] as JSONObject
                val userId = data.getString("userId")
                Log.d(TAG, "User offline: $userId")
                onUserOffline?.invoke(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Parse user:offline error: ${e.message}")
            }
        }

        // ──────────────── Typing Events ────────────────
        socket?.on("typing:start") { args ->
            try {
                val data = args[0] as JSONObject
                val userId = data.getString("userId")
                val username = data.getString("username")
                Log.d(TAG, "Typing start: $username")
                onTypingStart?.invoke(userId, username)
            } catch (e: Exception) {
                Log.e(TAG, "Parse typing:start error: ${e.message}")
            }
        }

        socket?.on("typing:stop") { args ->
            try {
                val data = args[0] as JSONObject
                val userId = data.getString("userId")
                Log.d(TAG, "Typing stop: $userId")
                onTypingStop?.invoke(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Parse typing:stop error: ${e.message}")
            }
        }

        // Error event
        socket?.on("error") { args ->
            try {
                val data = args[0] as JSONObject
                val message = data.getString("message")
                Log.e(TAG, "Server error: $message")
                onError?.invoke(message)
            } catch (e: Exception) {
                Log.e(TAG, "Parse error event: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EMIT EVENTS
    // ═══════════════════════════════════════════════════════════════════════

    fun sendMessage(conversationId: String, text: String, replyTo: String? = null) {
        if (socket == null || !socket!!.connected()) {
            Log.e(TAG, "Socket not connected")
            onError?.invoke("Socket not connected")
            return
        }

        val data = JSONObject().apply {
            put("conversationId", conversationId)
            put("text", text)
            if (replyTo != null) {
                put("replyTo", replyTo)
            }
        }

        socket?.emit("message:send", data)
        Log.d(TAG, "Message sent to $conversationId")
    }

    fun reactMessage(messageId: String, emoji: String) {
        if (socket == null || !socket!!.connected()) {
            Log.e(TAG, "Socket not connected")
            return
        }

        val data = JSONObject().apply {
            put("messageId", messageId)
            put("emoji", emoji)
        }

        socket?.emit("message:react", data)
        Log.d(TAG, "Reaction sent: $emoji to $messageId")
    }

    fun deleteMessage(messageId: String) {
        if (socket == null || !socket!!.connected()) {
            Log.e(TAG, "Socket not connected")
            return
        }

        val data = JSONObject().apply {
            put("messageId", messageId)
        }

        socket?.emit("message:delete", data)
        Log.d(TAG, "Delete message request: $messageId")
    }

    fun typingStart(conversationId: String) {
        if (socket == null || !socket!!.connected()) return

        val data = JSONObject().apply {
            put("conversationId", conversationId)
        }

        socket?.emit("typing:start", data)
    }

    fun typingStop(conversationId: String) {
        if (socket == null || !socket!!.connected()) return

        val data = JSONObject().apply {
            put("conversationId", conversationId)
        }

        socket?.emit("typing:stop", data)
    }

    fun getOnlineUsers() {
        if (socket == null || !socket!!.connected()) return
        socket?.emit("users:online")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LISTENER REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════

    fun setOnMessageNewListener(listener: (Message) -> Unit) {
        onMessageNew = listener
    }

    fun setOnMessageReactedListener(listener: (messageId: String, reactions: List<Reaction>) -> Unit) {
        onMessageReacted = listener
    }

    fun setOnMessageDeletedListener(listener: (messageId: String) -> Unit) {
        onMessageDeleted = listener
    }

    fun setOnUserOnlineListener(listener: (userId: String) -> Unit) {
        onUserOnline = listener
    }

    fun setOnUserOfflineListener(listener: (userId: String) -> Unit) {
        onUserOffline = listener
    }

    fun setOnTypingStartListener(listener: (userId: String, username: String) -> Unit) {
        onTypingStart = listener
    }

    fun setOnTypingStopListener(listener: (userId: String) -> Unit) {
        onTypingStop = listener
    }

    fun setOnErrorListener(listener: (message: String) -> Unit) {
        onError = listener
    }

    fun isConnected(): Boolean = socket?.connected() ?: false
}
