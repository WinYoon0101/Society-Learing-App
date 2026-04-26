# Chat Feature — Frontend Context (Android Java)

> File này dùng để cung cấp context cho Claude Code CLI khi làm việc với chat feature.
> Cập nhật khi có thay đổi cấu trúc hoặc API.

---

## 1. Kiến trúc tổng quan

Dự án theo pattern **MVVM + Repository**, chia 3 tầng rõ ràng:

```
UI (Fragment/Activity)
    ↕ observe LiveData
ViewModel
    ↕ gọi method
Repository
    ↕ Retrofit (REST)   +   ChatSocketManager (WebSocket)
Backend API (Node.js + Socket.IO)
```

---

## 2. Cấu trúc thư mục liên quan đến Chat

```
frontend/app/src/main/java/com/example/frontend/
│
├── data/
│   ├── model/
│   │   ├── Conversation.kt       ← Data class: id, members, lastMessage, createdAt
│   │   ├── Message.kt            ← Data class: id, conversationId, sender, text, replyTo, reactions, isDeleted
│   │   ├── Reaction.kt           ← Data class: userId, emoji
│   │   └── User.java             ← id, username, avatar, isActive
│   │
│   ├── remote/
│   │   ├── ApiClient.java        ← Retrofit singleton, tự gắn JWT từ SharedPreferences
│   │   ├── ApiService.java       ← Interface định nghĩa tất cả REST endpoints
│   │   └── (LoginRequest, RegisterRequest)
│   │
│   ├── repository/
│   │   └── ChatRepository.java   ← Gọi REST API: getConversations, getOrCreateConversation, getMessages
│   │
│   └── socket/
│       └── ChatSocketManager.kt  ← Singleton Kotlin object, quản lý toàn bộ Socket.IO
│
├── ui/
│   └── chat/
│       ├── ChatFragment.java         ← Màn hình danh sách conversations + online users
│       ├── ChatDetailFragment.java   ← Màn hình chat 1-1, gửi/nhận tin nhắn real-time
│       ├── ChatViewModel.java        ← ViewModel dùng chung cho cả 2 fragment
│       ├── ConversationAdapter.java  ← RecyclerView adapter cho danh sách conversations
│       ├── MessageAdapter.java       ← RecyclerView adapter cho danh sách messages
│       └── OnlineUserAdapter.java    ← RecyclerView ngang, hiển thị user đang online
│
└── utils/
    └── Constants.java    ← BASE_URL = "http://10.0.2.2:3000/api/", SOCKET_URL = "http://10.0.2.2:3000"
```

---

## 3. REST API Endpoints (Retrofit)

Base URL: `http://10.0.2.2:3000/api/` (emulator → localhost)
Auth: JWT tự động gắn qua OkHttp Interceptor từ `SharedPreferences("MyAppPrefs", "JWT_TOKEN")`

| Method | Endpoint | Mô tả | Response |
|--------|----------|-------|----------|
| GET | `chat/conversations` | Lấy danh sách conversations của user hiện tại | `ApiResponse<List<Conversation>>` |
| POST | `chat/conversations` | Tạo hoặc lấy conversation 1-1, body: `{ targetUserId }` | `ApiResponse<Conversation>` |
| GET | `chat/conversations/{conversationId}/messages` | Lấy tin nhắn (phân trang, mặc định page=1, limit=30) | `ApiResponse<List<Message>>` |
| DELETE | `chat/messages/{messageId}` | Thu hồi tin nhắn (soft delete) | `ApiResponse<Message>` |
| PATCH | `chat/conversations/{conversationId}/nickname` | Đặt nickname, body: `{ targetUserId, nickname }` | `ApiResponse<Conversation>` |
| PATCH | `chat/conversations/{conversationId}/color` | Đổi màu chat, body: `{ color }` | `ApiResponse<Conversation>` |

> ⚠️ `POST chat/messages` trong ApiService.java hiện KHÔNG dùng — gửi tin nhắn phải qua Socket.IO.

---

## 4. Socket.IO — ChatSocketManager.kt

**File:** `data/socket/ChatSocketManager.kt`
**Pattern:** Kotlin `object` (singleton)
**Server URL:** `Constants.SOCKET_URL = "http://10.0.2.2:3000"`
**Auth:** Token gửi qua `IO.Options.extraHeaders["Authorization"] = "Bearer $token"`

### Khởi tạo & kết nối
```kotlin
// Gọi 1 lần khi vào ChatDetailFragment
ChatSocketManager.initialize(context, Constants.SOCKET_URL, token)
ChatSocketManager.connect()
```

### Emit events (Client → Server)

| Event | Payload | Mô tả |
|-------|---------|-------|
| `message:send` | `{ conversationId, text, replyTo? }` | Gửi tin nhắn |
| `message:react` | `{ messageId, emoji }` | Thả cảm xúc (toggle) |
| `message:delete` | `{ messageId }` | Thu hồi tin nhắn |
| `typing:start` | `{ conversationId }` | Bắt đầu gõ |
| `typing:stop` | `{ conversationId }` | Dừng gõ |
| `users:online` | _(không có payload)_ | Lấy danh sách user online |

### Listen events (Server → Client)

| Event | Payload | Callback method |
|-------|---------|-----------------|
| `message:new` | `Message` object | `setOnMessageNewListener` |
| `message:reacted` | `{ messageId, reactions[] }` | `setOnMessageReactedListener` |
| `message:deleted` | `{ messageId, conversationId }` | `setOnMessageDeletedListener` |
| `user:online` | `{ userId }` | `setOnUserOnlineListener` |
| `user:offline` | `{ userId }` | `setOnUserOfflineListener` |
| `typing:start` | `{ conversationId, userId, username }` | `setOnTypingStartListener` |
| `typing:stop` | `{ conversationId, userId }` | `setOnTypingStopListener` |
| `error` | `{ message }` | `setOnErrorListener` |

---

## 5. Data Models

### Conversation.kt
```kotlin
id: String           // MongoDB _id
members: List<User>  // 2 members cho chat 1-1
lastMessage: Message?
createdAt, updatedAt: Date?
```

### Message.kt
```kotlin
id: String
conversationId: String
sender: User         // populated: { _id, username, avatar }
text: String
replyTo: Message?    // populated nếu là reply
reactions: List<Reaction>
isDeleted: Boolean   // true = "Tin nhắn đã bị thu hồi"
createdAt, updatedAt: Date?
```

### Reaction.kt
```kotlin
userId: String
emoji: String  // Một trong: ❤️ 😆 😮 😢 😡 👍
```

### User.java (các field dùng trong chat)
```java
String id        // _id từ MongoDB
String username
String avatar    // URL hoặc null
boolean isActive // dùng để check online status
```

---

## 6. SharedPreferences — "MyAppPrefs"

| Key | Giá trị |
|-----|---------|
| `JWT_TOKEN` | Bearer token, dùng cho cả Retrofit interceptor và Socket auth |
| `USER_ID` | MongoDB _id của user hiện tại |
| `USER_AVATAR` | Avatar URL của user hiện tại (có thể null) |

---

## 7. Luồng UI

### ChatFragment (danh sách)
1. `onViewCreated` → đọc `USER_ID`, `USER_AVATAR` từ SharedPreferences
2. `viewModel.fetchConversations()` → gọi `GET chat/conversations`
3. Observe `conversationsResult`:
   - Có data → hiển thị `rvConversations` (ConversationAdapter)
   - Extract members `isActive = true` → hiển thị `rvOnlineUsers` (OnlineUserAdapter, horizontal)
   - Empty → hiển thị `layoutEmptyChat`
4. Click conversation → navigate sang `ChatDetailFragment.newInstance(conversation, otherMember)`
5. Click online user → `viewModel.openConversation(userId)` → navigate sang `ChatDetailFragment`

### ChatDetailFragment (chat 1-1)
1. Nhận `Conversation` và `User otherMember` qua Bundle args
2. `viewModel.fetchMessages(conversationId)` → load lịch sử tin nhắn
3. `initializeSocket()` → khởi tạo socket nếu chưa connect
4. `setupSocketListeners()`:
   - `message:new` → filter đúng `conversationId` → `messageAdapter.addMessage()` + scroll xuống
   - `error` → Toast
5. Gửi tin nhắn: `btnSend` → `ChatSocketManager.sendMessage(conversationId, text)`
6. `rvMessages` dùng `LinearLayoutManager(stackFromEnd = true)` để scroll xuống cuối

---

## 8. Những gì chưa implement (TODO)

- [ ] `MessageAdapter` — chưa có UI cho reaction, reply, isDeleted
- [ ] Typing indicator UI trong `ChatDetailFragment`
- [ ] `message:reacted` listener chưa update UI
- [ ] `message:deleted` listener chưa update UI (đổi text thành "Tin nhắn đã bị thu hồi")
- [ ] Online status real-time (hiện chỉ dựa vào `isActive` từ REST, chưa dùng `user:online/offline` socket events)
- [ ] `OnlineUserAdapter` — chưa rõ data source (hiện lấy từ conversations members)
- [ ] Pagination cho messages (hiện load page 1 cố định)
- [ ] Reply UI (chọn tin nhắn để reply, hiển thị preview)

---

## 9. Known bugs & fixes

### Bug: "Không có quyền gửi tin nhắn" khi conversation mới tạo
- **Nguyên nhân**: Socket join room khi connect, nhưng conversation tạo sau khi connect thì không được join
- **Fix BE**: Trong `message:send` handler, gọi `socket.join(conversationId)` trước khi check permission — đã fix

### Bug: Tin nhắn của mình hiện bên trái (như đối phương)
- **Nguyên nhân**: `USER_ID` lưu từ login = plain string, `sender._id` từ socket có thể là ObjectId format khác
- **Fix BE**: Dùng `.lean()` khi populate message để `_id` serialize thành plain string — đã fix
- **Fix FE**: `User.getId()` thêm `.trim()`, `MessageAdapter` trim cả 2 phía trước `equals()` — đã fix
- **Debug**: Check Logcat tag `MessageAdapter` và `ChatDetail` để so sánh 2 ID nếu vẫn còn lỗi

---

## 10. Lưu ý khi code

- **Kotlin vs Java**: Models (`Conversation`, `Message`, `Reaction`) viết bằng Kotlin data class. UI và Repository viết bằng Java. Khi gọi Kotlin từ Java cần chú ý null safety.
- **Socket singleton**: `ChatSocketManager` là `object` Kotlin, gọi từ Java bằng `ChatSocketManager.INSTANCE`.
- **Thread**: Socket callbacks chạy trên background thread → phải `runOnUiThread {}` khi update UI.
- **Emulator IP**: `10.0.2.2` = localhost của máy host khi chạy trên Android Emulator.
- **Token refresh**: Hiện chưa có auto-refresh token, nếu token hết hạn cần login lại.
- **Socket lifecycle**: Nên `disconnect()` khi user logout hoặc app vào background lâu.
