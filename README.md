# Society Learning App 🎓📱

Một ứng dụng di động kết hợp hoàn hảo giữa mạng xã hội và nền tảng học tập trực tuyến. Dự án mang đến trải nghiệm đột phá với các tính năng tiên tiến như tạo trắc nghiệm tự động bằng AI, nhận diện cảm xúc (Face Detection) trong quá trình học tập (Pomodoro), livestream tương tác và trò chuyện theo thời gian thực. 

Dự án bao gồm ba phần chính: **Backend (Node.js/Express)**, **Frontend (Android — ứng dụng người dùng)** và **Admin (Android — ứng dụng quản trị viên)**.

---

## 🌟 Tính năng nổi bật

### 📚 Học tập & Trải nghiệm thông minh
- **Tạo Quiz tự động bằng AI:** Tích hợp `Google Generative AI` tự động phân tích chủ đề học tập và sinh ra bộ câu hỏi trắc nghiệm (Quiz/Attempt) một cách thông minh và nhanh chóng.
- **Quản lý Tài liệu:** Lưu trữ, chia sẻ và quản lý tài liệu học tập dễ dàng giữa các người dùng và hội nhóm.
- **Pomodoro & AI Emotion Detector:** Tích hợp phương pháp Pomodoro để quản lý thời gian tập trung. Đặc biệt kết hợp AI (TensorFlow Lite & Google ML Kit) qua CameraX để nhận diện khuôn mặt và phân tích cảm xúc của người dùng trong suốt phiên học.
- **Smart Scanner (OCR & Code Detection):** Tích hợp AI (Google ML Kit) để quét và bóc tách văn bản, đoạn code từ hình ảnh hoặc camera. Thuật toán tự động nhận diện ngôn ngữ lập trình, giữ nguyên định dạng thụt lề (indentation) và tô màu cú pháp (Syntax Highlighting) qua CodeView, cho phép người dùng chia sẻ nhanh chóng lên bảng tin.
- **Tạo Sơ đồ tư duy (Mindmap):** Ứng dụng AI để tự động phân tích, bóc tách các tài liệu hoặc bài học phức tạp và trực quan hóa thành Sơ đồ tư duy (Mindmap), giúp người dùng hệ thống hóa kiến thức cốt lõi nhanh chóng và sinh động.
- **Quản lý Lịch biểu (Calendar):** Tích hợp lịch thông minh giúp người dùng lên kế hoạch cá nhân, theo dõi các sự kiện của bản thân hoặc đặt lịch nhắc nhở cho các bài kiểm tra (Quiz).

### 🌐 Mạng xã hội & Tương tác
- **Bảng tin (News Feed):** Trung tâm cập nhật mọi hoạt động từ bạn bè và cộng đồng. Được thiết kế tối ưu để hiển thị mượt mà đa dạng các định dạng nội dung phức tạp trên cùng một luồng cuộn, bao gồm: bài viết văn bản dài, album hình ảnh, video, và trực tiếp chia sẻ tài liệu/đoạn code.
- **Đăng bài đa chế độ hiển thị:** Khi tạo bài viết, người dùng chọn quyền riêng tư gồm **Công khai (Public)**, **Bạn bè (Friends)** hoặc **Chỉ mình tôi (Private)**. Bảng tin chỉ hiển thị bài phù hợp với mối quan hệ bạn bè và quyền riêng tư của từng bài.
- **Hashtag & Gắn thẻ bạn bè:** Hệ thống tự động trích xuất hashtag từ nội dung bài viết (regex `#từ_khóa`) và lưu vào cơ sở dữ liệu. Người dùng có thể gắn thẻ (tag) bạn bè trong bài viết thông qua danh sách bạn bè đã kết nối; thông tin người được gắn thẻ hiển thị trên bài đăng.
- **Cảm xúc & Đa phương tiện:** Hỗ trợ chọn trạng thái cảm xúc (feeling) khi đăng bài, đính kèm nhiều ảnh/video, và hiển thị biểu tượng quyền riêng tư trực tiếp trên từng bài trong feed.
- **Tìm kiếm & Xu hướng (Trending):** Màn hình tìm kiếm tổng hợp người dùng, nhóm và bài viết theo từ khóa (debounce khi gõ). Khi chưa nhập từ khóa, hiển thị **Top 10 hashtag xu hướng** được tính từ bài viết Public trong 14 ngày gần nhất — so sánh lượt nhắc đến tuần hiện tại (7 ngày) với tuần trước, kèm **tỷ lệ % tăng trưởng** (hashtag mới hiển thị nhãn "MỚI"). Click vào hashtag để lọc chính xác các bài viết theo tag đó.
- **Hệ thống Tương tác chuyên sâu:** Người dùng có thể tương tác trực tiếp qua bình luận, bộ thả cảm xúc phong phú, nhắc tên người dùng khác, và chia sẻ khoảnh khắc với tính năng Story tự động biến mất sau 24h.
- **Mạng lưới Bạn bè & Gợi ý kết bạn thông minh:** Hệ thống quản lý kết bạn toàn diện bao gồm gửi/nhận, đồng ý/từ chối lời mời. **Gợi ý kết bạn** dựa trên thuật toán aggregation MongoDB:
  - Loại bỏ chính mình, người đã là bạn bè, người đã gửi lời mời đang chờ cho mình, và người mình đã từ chối.
  - Giữ lại người mình đã gửi lời mời (hiển thị trạng thái `isPending`) để có thể hủy yêu cầu ngay tại chỗ.
  - Tính **số bạn chung (mutual friends)** của danh sách bạn bè hai bên, sắp xếp giảm dần theo số bạn chung, trả về tối đa 15 gợi ý.
  - Giao diện hỗ trợ thêm bạn, hủy lời mời đã gửi, gỡ gợi ý khỏi danh sách.
- **Không gian Nhóm độc lập:** Cho phép người dùng khởi tạo, tham gia và quản trị các cộng đồng học tập riêng. Mỗi nhóm sở hữu một không gian độc lập với bảng tin riêng, hệ thống quản lý thành viên và phân quyền quản trị viên chặt chẽ.
- **Trò chuyện Real-time đa luồng:** Tích hợp kiến trúc Socket.io mang đến trải nghiệm nhắn tin 1-1 với độ trễ cực thấp. Hỗ trợ hiển thị trạng thái hoạt động (Online/Offline).
- **Gọi video & Livestream chất lượng cao:** Ứng dụng công nghệ ZegoCloud để cung cấp tính năng gọi thoại/video trực tiếp và phát sóng trực tiếp (Livestreaming). Hoạt động mượt mà ngay cả trong môi trường mạng yếu, tối ưu hóa cho các buổi học nhóm hoặc chia sẻ kiến thức trực tuyến.

### 👨‍💼 Ứng dụng Admin (Quản trị hệ thống)
Ứng dụng Android riêng dành cho quản trị viên, kết nối các API `/admin/*` trên Backend:

- **Dashboard thống kê:** Biểu đồ trực quan (Pie, Line, Bar) hiển thị tổng người dùng, người dùng mới, tổng bài viết, báo cáo chờ xử lý, xu hướng tăng trưởng và phân bố tương tác.
- **Quản lý người dùng:** Xem toàn bộ tài khoản, tìm kiếm theo tên/email, xem chi tiết hồ sơ (Bottom Sheet), **khóa/mở khóa tài khoản** người dùng vi phạm.
- **Quản lý bài viết & AI phát hiện nội dung độc hại:** Duyệt danh sách bài viết phân trang. Tích hợp mô hình **TensorFlow Lite** chạy on-device để phân loại nội dung thành 3 nhãn: **SẠCH**, **THÔ TỤC**, **THÙ ĐỊCH**. Quét tự động khi tải bài viết, hỗ trợ lọc chỉ hiển thị bài viết độc hại, và cho phép **xóa bài viết** trực tiếp từ giao diện admin.
- **Gửi thông báo hệ thống:** Phát thông báo đến toàn bộ người dùng hoặc nhóm người dùng cụ thể (chọn bằng checkbox). Hỗ trợ các loại: thông báo thường, cảnh báo, sự kiện.

### 🛡 Hệ thống & Bảo mật
- **Xác thực đa luồng:** Hỗ trợ đăng nhập qua Facebook, Google Auth. Bảo mật tài khoản với hệ thống mã hóa mật khẩu và JWT (JSON Web Tokens).
- **Lưu trữ đa phương tiện đám mây:** Quản lý và tự động upload hình ảnh, video, tài liệu lên Cloudinary thông qua Multer.
- **Giao diện hiện đại (UI/UX):** Sử dụng các component hiện đại như SwipeRefreshLayout, ViewPager2, kết hợp với Lottie Animations và Glide để mang lại trải nghiệm tương tác chuyên nghiệp.

---

## 🛠 Tech Stack

### ⚙️ Backend (RESTful API)
- **Core:** Node.js, Express.js, TypeScript
- **Cơ sở dữ liệu:** MongoDB (Mongoose)
- **Authentication:** JWT, bcryptjs, Google Auth Library
- **Real-time:** Socket.io
- **AI Integration:** Google Generative AI (@google/generative-ai)
- **Media Storage:** Cloudinary, Multer
- **Tài liệu API:** Swagger UI, Swagger JSDoc
- **Khác:** Nodemailer (Gửi email), dotenv, cors

### 📱 Frontend (Android Mobile App — Người dùng)
- **Ngôn ngữ & Kiến trúc:** Kotlin / Java (Min SDK 26, Target SDK 36), kiến trúc MVVM (ViewModel, LiveData)
- **Networking:** Retrofit, OkHttp (với Logging Interceptor)
- **Real-time:** Socket.io-client
- **AI & Computer Vision:** TensorFlow Lite, Google ML Kit (Face Detection), CameraX, Text Recognition (OCR offline)
- **Image Processing & UI Tools:** UCrop, CodeView, PhotoView
- **Streaming & Calling:** ZegoCloud (UIKit Prebuilt Live Streaming & Signaling)
- **UI & Animation:** Material Design, Lottie, Glide, CircleImageView
- **Native C++:** External Native Build bằng CMake

### 🔧 Admin (Android Mobile App — Quản trị viên)
- **Ngôn ngữ:** Java, kiến trúc Fragment-based
- **Networking:** Retrofit, OkHttp
- **AI on-device:** TensorFlow Lite + Flex Delegate — mô hình phân loại nội dung độc hại tiếng Việt
- **Biểu đồ thống kê:** MPAndroidChart (Pie, Line, Bar Chart)
- **UI:** Material Design, BottomSheetDialog, Glide

---

## 📁 Cấu trúc dự án

```text
Society_Mobile/
├── backend/                  # Mã nguồn server Node.js & REST API
│   ├── src/                  # Source code TypeScript (Controllers, Models, Routes)
│   ├── .env.example          # Template biến môi trường (copy thành .env)
│   ├── package.json          # Quản lý dependencies (npm)
│   └── tsconfig.json         # Cấu hình TypeScript
├── frontend/                 # Ứng dụng Android cho người dùng
│   ├── app/                  # Module chính của ứng dụng
│   │   ├── google-services.json  # Firebase / Google Sign-In (cần tự thêm)
│   │   ├── src/main/cpp/     # Mã nguồn Native C++ & CMakeLists
│   │   ├── src/main/java/    # Source code Android (Kotlin/Java)
│   │   │   └── .../utils/
│   │   │       ├── Constants.example.java  # Template cấu hình API (copy → Constants.java)
│   │   │       └── Constants.java          # File thật — nằm trong .gitignore
│   │   └── build.gradle.kts  # Build script cho module App
│   ├── gradle/               # Gradle wrapper
│   └── build.gradle.kts      # Cấu hình build project root
└── admin/                    # Ứng dụng Android cho quản trị viên
    ├── app/
    │   ├── src/main/assets/  # Mô hình TFLite (vietnamese_toxic_model.tflite)
    │   ├── src/main/java/    # Dashboard, Users, Posts, Notifications
    │   └── build.gradle.kts
    └── build.gradle.kts
```

---

## 🚀 Hướng dẫn cài đặt và chạy dự án

### Yêu cầu hệ thống

| Thành phần | Yêu cầu |
|------------|---------|
| **Backend** | Node.js v18+, npm, MongoDB (local hoặc Atlas) |
| **Frontend** | Android Studio (mới nhất), JDK 11, Android SDK 34+, NDK/CMake (native C++) |
| **Admin** | Android Studio, JDK 11, Android SDK 36 |
| **Dịch vụ bên thứ ba** | Cloudinary, Google Gemini API, ZegoCloud (gọi video/livestream), Google/Facebook OAuth (tùy chọn) |

---

### 1. Thiết lập Backend

#### Bước 1 — Clone và cài dependencies

```bash
cd backend
npm install
```

#### Bước 2 — Cấu hình biến môi trường

Tạo file `.env` từ template:

```bash
# Linux / macOS
cp .env.example .env

# Windows (PowerShell)
Copy-Item .env.example .env
```

Điền các biến trong `.env`:

| Biến | Bắt buộc | Mô tả |
|------|----------|-------|
| `PORT` | Không | Cổng server (mặc định `3000`) |
| `MONGO_URI_ATLAS` | **Có** | Chuỗi kết nối MongoDB. Local: `mongodb://localhost:27017/society` |
| `JWT_SECRET` | **Có** | Secret ký access token |
| `JWT_REFRESH_SECRET` | **Có** | Secret ký refresh token |
| `CLOUDINARY_NAME` | **Có** | Tên Cloudinary (upload media) |
| `CLOUDINARY_API_KEY` | **Có** | API Key Cloudinary |
| `CLOUDINARY_API_SECRET` | **Có** | API Secret Cloudinary |
| `GEMINI_API_KEY` | **Có** | Google Gemini — Quiz AI, Mindmap, phân tích tài liệu |
| `GOOGLE_CLIENT_ID` | Khuyến nghị | Xác thực đăng nhập Google |
| `FACEBOOK_APP_ID` | Khuyến nghị | Xác thực đăng nhập Facebook |
| `FACEBOOK_APP_SECRET` | Khuyến nghị | App Secret Facebook |
| `EMAIL_USER` | Khuyến nghị | Email gửi OTP (Gmail App Password) |
| `EMAIL_PASS` | Khuyến nghị | Mật khẩu ứng dụng email |

#### Bước 3 — Khởi động MongoDB

**Local:** cài và chạy MongoDB Community, database mặc định là `society`.

**Atlas:** tạo cluster trên [MongoDB Atlas](https://www.mongodb.com/cloud/atlas), lấy connection string và gán vào `MONGO_URI_ATLAS`.

#### Bước 4 — Chạy server

```bash
# Chế độ phát triển (hot-reload)
npm run dev

# Hoặc build + chạy production
npm run build
npm start
```

Server khởi động tại:
- REST API: `http://localhost:3000/api`
- WebSocket (chat): `ws://localhost:3000`
- Swagger docs: `http://localhost:3000/docs`

---

### 2. Thiết lập Frontend (Android — Người dùng)

#### Bước 1 — Mở project

Mở thư mục `frontend/` bằng Android Studio → **Sync Project with Gradle Files**.

Gradle cần repository `maven.zego.im`, `jitpack.io` (đã cấu hình sẵn trong `settings.gradle.kts`).

#### Bước 2 — Tạo file cấu hình API (`Constants.java`)

File `Constants.java` (chứa URL và key). Tạo từ template:


Chỉnh `Constants.java` theo môi trường:

| Hằng số | Ghi chú |
|---------|---------|
| `BASE_URL` | Phải kết thúc bằng `/api/` |
| `SOCKET_URL` | Cùng host với backend, **không** có `/api` |
| `ZEGO_APP_ID` | App ID từ [ZegoCloud Console](https://console.zegocloud.com/) |
| `ZEGO_APP_SIGN` | App Sign từ ZegoCloud |

**URL theo thiết bị chạy app:**

| Môi trường | `BASE_URL` | `SOCKET_URL` |
|------------|-----------|--------------|
| Emulator | `http://10.0.2.2:3000/api/` | `http://10.0.2.2:3000` |
| Điện thoại | `http://<IP-máy-dev>:3000/api/` | `http://<IP-máy-dev>:3000` |

> `10.0.2.2` là alias của `localhost` trên Android Emulator trỏ về máy host.

#### Bước 3 — Cấu hình Google Sign-In

1. Tải `google-services.json` từ [Firebase Console](https://console.firebase.google.com/) → đặt vào `frontend/app/`.
2. Cập nhật `default_web_client_id` trong `app/src/main/res/values/strings.xml` khớp với OAuth Client ID trên Google Cloud.
3. Đảm bảo `GOOGLE_CLIENT_ID` trong backend `.env` trùng với client ID trên.

#### Bước 4 — Cấu hình Facebook Login

Chỉnh `app/src/main/res/values/strings.xml`:

```xml
<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
```

Đồng bộ `FACEBOOK_APP_ID` / `FACEBOOK_APP_SECRET` trong backend `.env`.

#### Bước 5 — Build và chạy

1. Chọn module `app`, thiết bị Emulator (API 26+) hoặc điện thoại thật.
2. Bật **USB Debugging** nếu dùng thiết bị thật; đảm bảo điện thoại và máy dev **cùng mạng Wi-Fi**.
3. Run ▶ — app yêu cầu quyền Camera, Microphone (ZegoCloud), Storage.

> App đã bật `usesCleartextTraffic="true"` để hỗ trợ HTTP khi dev local.

---

### 3. Thiết lập Admin (Android — Quản trị viên)

#### Bước 1 — Mở project

Mở thư mục `admin/` bằng Android Studio → Sync Gradle.

#### Bước 2 — Cấu hình API URL

Sửa `BASE_URL` trong `admin/app/src/main/java/com/example/admin/data/remote/RetrofitClient.java`:

```java
// Local (Emulator)
private static final String BASE_URL = "http://10.0.2.2:3000/api/";

// Hoặc server deploy
private static final String BASE_URL = "https://your-domain.com/api/";
```

#### Bước 3 — Thêm mô hình AI phát hiện nội dung độc hại

Đặt file `vietnamese_toxic_model.tflite` vào:

```
admin/app/src/main/assets/vietnamese_toxic_model.tflite
```

Mô hình TensorFlow Lite phân loại nội dung thành **SẠCH / THÔ TỤC / THÙ ĐỊCH**, chạy on-device qua `ToxicScanner.java`. Nếu thiếu file này, app vẫn chạy nhưng không quét được nội dung.

#### Bước 4 — Chạy app

Run module `app` trên Emulator hoặc thiết bị thật. App kết nối trực tiếp tới các API `/admin/*` trên Backend (Dashboard, Users, Posts, Notifications).

---

### 4. Kiểm tra kết nối nhanh

Sau khi Backend chạy, mở trình duyệt:

```
http://localhost:3000/docs        → Swagger UI
http://localhost:3000/api/auth    → Kiểm tra route auth
```

Trên Emulator, đăng ký tài khoản mới qua app Frontend. Nếu lỗi kết nối:
1. Kiểm tra Backend đang chạy và MongoDB đã kết nối.
2. Kiểm tra `BASE_URL` / `SOCKET_URL` trong `Constants.java`.
3. Với thiết bị thật: tắt firewall, dùng IP LAN thay vì `localhost`.

---

## 📝 Tài liệu API (API Documentation)
Toàn bộ tài liệu API được tự động hóa (auto-generated) bằng Swagger. 
Sau khi chạy backend thành công, bạn có thể xem và thử nghiệm trực tiếp các API tại:
👉 `http://localhost:<PORT>/docs`

### Một số API đáng chú ý

| Nhóm | Endpoint | Mô tả |
|------|----------|-------|
| Tìm kiếm | `GET /api/search/trending` | Top 10 hashtag xu hướng (7 ngày, so sánh tuần trước) |
| Tìm kiếm | `GET /api/search/results?q=` | Tìm kiếm tổng hợp user, nhóm, bài viết |
| Tìm kiếm | `GET /api/search/results?hashtag=` | Lọc bài viết theo hashtag cụ thể |
| Bạn bè | `GET /api/friends/suggestions` | Gợi ý kết bạn (sắp xếp theo bạn chung) |
| Bài viết | `POST /api/posts` | Đăng bài (privacy, tags, hashtags, feeling, media) |
| Admin | `GET /admin/dashboard` | Thống kê tổng quan hệ thống |
| Admin | `GET /admin/posts` | Danh sách bài viết (phân trang) |
| Admin | `DELETE /admin/posts/:id` | Xóa bài viết |
| Admin | `PUT /admin/users/:id/toggle-status` | Khóa/mở khóa tài khoản |
| Admin | `POST /admin/notifications/send` | Gửi thông báo hệ thống |

---
