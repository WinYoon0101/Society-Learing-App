---
name: chat-feature
description: Quy tắc bắt buộc khi làm/sửa tính năng chat (reaction, reply, typing, online status, video call optional) trong frontend Android. Kích hoạt khi user yêu cầu code/UI/sửa lỗi liên quan tới chat, message, conversation, reaction, hoặc bất kỳ file nào trong `app/src/main/java/com/example/frontend/ui/chat/`, `data/socket/ChatSocketManager.kt`, `data/repository/ChatRepository.java`, hoặc layout `fragment_chat*.xml` / `item_message*.xml` / `item_conversation.xml`.
---

# Chat Feature — Skill (BẮT BUỘC tuân thủ)

> Đọc `frontend/CHAT_CONTEXT.md` trước khi code. Skill này chỉ ghi các **rule cứng** — chi tiết kiến trúc/endpoint/socket nằm trong CHAT_CONTEXT.md.

---

## Rule 1 — KHÔNG động vào Gradle

- **Tuyệt đối không sửa**: `build.gradle.kts` (root + app), `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`.
- Stack hiện tại đã ổn định với Socket.IO + Retrofit + Glide + Kotlin/Java mix. Mọi thay đổi đều có rủi ro vỡ sync hoặc build lại từ đầu trên môi trường máy ảo.
- Nếu thật sự cần thư viện mới (ví dụ WebRTC cho video call) → **DỪNG LẠI và hỏi user trước**, không tự thêm dependency.
- Không đổi `compileSdk`, `targetSdk`, `minSdk`, `kotlin/jvm version`, `applicationId`. Không bật/tắt plugin (`kapt`, `ksp`, `parcelize`…).
- Cách kiểm tra trước khi commit: `git diff --name-only` không được có file nào trong `gradle*`, `*.gradle.kts`, `gradle.properties`.

## Rule 2 — UI phải theo cấu trúc frontend hiện có, KHÔNG nhồi 1 trang

Cấu trúc đã chốt (xem `CHAT_CONTEXT.md` mục 2):

| Màn hình | Layout | Code |
|---|---|---|
| Danh sách conversation + online users | `fragment_chat.xml` | `ChatFragment.java` |
| Chat 1-1 (messages + input) | `fragment_chat_detail.xml` | `ChatDetailFragment.java` |
| Item conversation | `item_conversation.xml` | `ConversationAdapter.java` |
| Item message gửi đi | `item_message_sent.xml` | `MessageAdapter.SentViewHolder` |
| Item message nhận | `item_message_received.xml` | `MessageAdapter.ReceivedViewHolder` |
| Item online user | `item_online_user.xml` | `OnlineUserAdapter.java` |

Quy tắc:
- **Một màn hình = một fragment + một layout root**. Không nhồi danh sách + chat detail + setting + call vào cùng `fragment_chat.xml`.
- **Một loại item = một file `item_*.xml`**. Reply preview, reaction bar, typing bubble, image message, system message → **tách layout riêng** hoặc dùng `<include>` / `ViewStub`, không nhét cứng vào `item_message_*.xml`.
- **Bottom sheet / dialog**: emoji picker, reaction picker, attachment menu, message options (reply/copy/delete) → dùng `BottomSheetDialogFragment` hoặc `DialogFragment` riêng + layout `dialog_*.xml` / `sheet_*.xml`. Không inflate trực tiếp vào màn chat detail.
- **Video call (nếu làm)**: tách thành `VideoCallActivity` hoặc `VideoCallFragment` riêng, layout `activity_video_call.xml`. Không gắn vào `fragment_chat_detail.xml`.
- **Navigation**: dùng đúng pattern hiện tại — `FragmentTransaction.replace(...).addToBackStack(null)` — không tự thêm Navigation Component nếu chưa có.
- Trước khi tạo file mới, **chạy `Glob` kiểm tra** xem layout/class đã tồn tại chưa. Tránh trùng `MessageOptionsDialog.java` với cái đã có.

## Rule 3 — Luôn gọi BE để lấy đủ dữ liệu hiển thị

- **Không hard-code, không mock data trong UI**. Mọi field hiển thị (avatar, username, lastMessage, online status, reactions, replyTo) đều phải đến từ:
  - REST: `ChatRepository.java` → `ApiService.java` (xem CHAT_CONTEXT.md mục 3 cho danh sách endpoint)
  - Socket: `ChatSocketManager.kt` (xem mục 4)
- Khi cần thêm field mà **BE chưa populate đủ** (ví dụ `replyTo.sender.username`, `reactions[].user.username`):
  1. Kiểm tra response thật bằng Logcat (`Log.d` trong `ChatRepository` callback).
  2. Nếu thiếu → **báo cho user** để sửa BE (`backend/controllers/chat.controller.js`, populate thêm field). Không tự fake bằng cách re-fetch từ FE.
- **Pagination**: khi scroll lên đầu list message, gọi `getMessages(conversationId, page=N, limit=30)` — không load 1 lần toàn bộ.
- **Reaction / delete / typing**: phải đi qua socket event đã định nghĩa (`message:react`, `message:delete`, `typing:start/stop`). Không tự định nghĩa REST endpoint mới cho mấy việc này.
- **Online status**: ưu tiên realtime qua `user:online` / `user:offline` socket events; `User.isActive` từ REST chỉ là fallback lúc mở app.
- Khi UI thiếu data: log rõ field nào null/missing trước khi viết code "xử lý cho có". Sửa nguồn data, không sửa triệu chứng.

---

## Checklist trước khi báo "xong"

1. `git diff --name-only` — không có file gradle nào.
2. Mỗi tính năng mới = layout/file/class riêng đúng quy ước trên.
3. Không có `TODO mock`, không có giá trị hard-code (username, emoji list cố định OK, message text thì không).
4. Build thử trên máy ảo — không yêu cầu user sync gradle lại.
5. Update `CHAT_CONTEXT.md` mục 8 (TODO) hoặc mục 9 (Known bugs) nếu có thay đổi đáng kể.
6. **Update mục "Progress log" bên dưới** — thêm 1 dòng cho mỗi task xong (ngày · tên task · file thay đổi · ghi chú nếu cần). Ghi ngắn — đủ để Claude session sau đọc và hiểu state hiện tại không cần đọc lại toàn bộ git log.

---

## Progress log (đọc trước khi bắt đầu task mới)

> Format: `YYYY-MM-DD · <task> · files: <list>` + 1 dòng note nếu cần. Mới nhất ở trên cùng.

- `2026-05-05 · Send file/image trong chat (paperclip → upload → socket message)` · files: `app/src/main/java/com/example/frontend/data/model/Message.kt`, `data/remote/ApiService.java`, `data/socket/ChatSocketManager.kt`, `ui/chat/MessageAdapter.java`, `ui/chat/ChatDetailFragment.java`, `app/src/main/res/layout/fragment_chat_detail.xml`, `item_message_sent.xml`, `item_message_received.xml`, `item_message_media.xml`, `app/src/main/res/drawable/ic_attachment.xml`
  - Note: BE đã có sẵn `mediaUrl`/`mediaType` ở `message.model.ts` + `chat.socket.ts` (không cần sửa). FE: `Message.kt` thêm 2 field; `ApiService.uploadChatMedia()` POST `/media/upload` (sourceType="message", targetId=conversationId, field "media"); `ChatSocketManager.sendMessage()` thêm 2 optional param `mediaUrl`/`mediaType`. UI: paperclip `btnAttach` cạnh `etMessage`, click → `Intent.ACTION_GET_CONTENT` (image/video/pdf/doc) → `uploadAndSendFile()` chạy AsyncTask: đọc bytes + multipart upload → URL trả về → emit socket. Render: `<include @layout/item_message_media>` ở cả sent/received, `bindMedia()` decide image (Glide centerCrop) vs file row (icon 📄/🎬 + filename). Ẩn `tvSentMessage`/`tvReceivedMessage` khi text rỗng để không hiện bubble trống. `MessageAdapter.updateReactions` đã sync với constructor mới của `Message` (thêm 2 vị trí trước `createdAt`). `ApiClient` không có `getInstance()` — dùng `getApiService(applicationContext)`. ⚠️ `inputStream.readAllBytes()` cần API 33+ (Java 9), nếu minSdk thấp hơn có thể fail — chưa test runtime.

- `2026-05-05 · Reaction trigger UX kiểu Telegram: btnReact bên cạnh bubble` · files: `app/src/main/res/layout/item_message_sent.xml`, `item_message_received.xml`, `app/src/main/res/drawable/ic_emoji_react.xml`, `MessageAdapter.java`
  - Note: Mỗi bubble giờ có nút emoji ☺ (`ic_emoji_react`, alpha 0.5) — bên trái cho sent, bên phải cho received. Tap → mở `ReactionPopupHelper` ngay tại anchor là btnReact (popup nổi sát nút). Long-press bubble vẫn giữ làm fallback. Không đụng socket / `ReactionPopupHelper.show()`.

- `2026-05-05 · Reaction Phần 2: popup picker kiểu Facebook + wire reaction` · files: `app/src/main/res/layout/popup_reaction_picker.xml`, `app/src/main/res/drawable/bg_reaction_popup.xml`, `app/src/main/java/com/example/frontend/ui/chat/ReactionPopupHelper.java`, `ChatDetailFragment.java`
  - Note: Long-press bubble → `PopupWindow` 6 emoji nổi trên anchor (không dùng bottom sheet). `bg_reaction_popup` = white rounded 24dp + stroke 1dp #E0E0E0. `ReactionPopupHelper.show(ctx, anchor, msg, currentUserId, listener)` tự measure + clamp trong `windowVisibleDisplayFrame`, fallback xuống dưới nếu sát top; emoji user đang chọn được highlight bằng `bg_reaction_chip_mine`. `ChatDetailFragment`: long-press + chip click đều gọi `ChatSocketManager.reactMessage()`; thêm `setOnMessageReactedListener` để `messageAdapter.updateReactions()` realtime. `MessageAdapter` không cần sửa — long-press đã wire ở cả Sent + Received ViewHolders từ Phần 1.

- `2026-05-05 · Bug fix: tiếng Việt mất dấu khi gõ tin nhắn` · files: `app/src/main/res/layout/fragment_chat_detail.xml`
  - Note: `etMessage` thiếu `android:inputType` → IME không kích hoạt compose mode, bàn phím tiếng Việt mất dấu. Thêm `inputType="textMultiLine|textCapSentences"`.

- `2026-05-01 · Skill chat-feature khởi tạo + plan icon redesign` · files: `.claude/skills/chat-feature/SKILL.md`
  - Note: Liệt kê các icon hiện sai meaning + cấu trúc layout chuẩn (header/input/reply/typing/reaction sheet/message options sheet/video call). Chưa code UI.

- `2026-05-03 · Pretask UI: send + back + new-chat FAB + select-friend sheet` · files: `app/src/main/res/drawable/ic_send.xml`, `ic_arrow_back.xml`, `ic_add.xml`, `ic_search.xml`, `app/src/main/res/layout/fragment_chat_detail.xml`, `fragment_chat.xml`, `sheet_select_friend.xml`, `item_friend_pick.xml`, `app/src/main/java/com/example/frontend/ui/chat/SelectFriendBottomSheet.java`, `ChatFragment.java`
  - Note: Send button đổi sang `ic_send` (paper plane). Back button bỏ rotation hack, dùng `ic_arrow_back`. FAB đổi sang `ic_add` (dấu +). FAB click → mở `SelectFriendBottomSheet` — sheet đã có search bar + empty state, **chưa wire data** vì BE chưa có API friend-list. Khi BE xong: tạo `FriendPickAdapter`, gọi `ApiService.getFriends()`, click row → `viewModel.openConversation(friend.id)` (đã sẵn ở `ChatViewModel`).
