# Society Learning App 🎓📱

Một ứng dụng di động kết hợp hoàn hảo giữa mạng xã hội và nền tảng học tập trực tuyến. Dự án mang đến trải nghiệm đột phá với các tính năng tiên tiến như tạo trắc nghiệm tự động bằng AI, nhận diện cảm xúc (Face Detection) trong quá trình học tập (Pomodoro), livestream tương tác và trò chuyện theo thời gian thực. 

Dự án bao gồm hai phần chính: **Backend (Node.js/Express)** và **Frontend (Android/Kotlin/Java)**.

---

## 🌟 Tính năng nổi bật

### 📚 Học tập & Trải nghiệm thông minh
- **Tạo Quiz tự động bằng AI:** Tích hợp `Google Generative AI` tự động phân tích chủ đề học tập và sinh ra bộ câu hỏi trắc nghiệm (Quiz/Attempt) một cách thông minh và nhanh chóng.
- **Quản lý Tài liệu:** Lưu trữ, chia sẻ và quản lý tài liệu học tập dễ dàng giữa các người dùng và hội nhóm.
- **Pomodoro & AI Emotion Detector:** Tích hợp phương pháp Pomodoro để quản lý thời gian tập trung. Đặc biệt kết hợp AI (TensorFlow Lite & Google ML Kit) qua CameraX để nhận diện khuôn mặt và phân tích cảm xúc của người dùng trong suốt phiên học.
- **Smart Scanner (OCR & Code Detection):** Tích hợp AI (Google ML Kit) để quét và bóc tách văn bản, đoạn code từ hình ảnh hoặc camera. Thuật toán tự động nhận diện ngôn ngữ lập trình, giữ nguyên định dạng thụt lề (indentation) và tô màu cú pháp (Syntax Highlighting) qua CodeView, cho phép người dùng chia sẻ nhanh chóng lên bảng tin.
- **Tạo Sơ đồ tư duy (Mindmap):** Ứng dụng AI để tự động phân tích, bóc tách các tài liệu hoặc bài học phức tạp và trực quan hóa thành Sơ đồ tư duy (Mindmap), giúp người dùng hệ thống hóa kiến thức cốt lõi nhanh chóng và sinh động.
- **Quản lý Lịch biểu (Calendar):** Tích hợp lịch thông minh giúp người dùng lên kế hoạch cá nhân, theo dõi các sự kiện của nhóm hoặc đặt lịch nhắc nhở cho các bài kiểm tra (Quiz).

### 🌐 Mạng xã hội & Tương tác
- **Bảng tin (News Feed):** Trung tâm cập nhật mọi hoạt động từ bạn bè và cộng đồng. Được thiết kế tối ưu để hiển thị mượt mà đa dạng các định dạng nội dung phức tạp trên cùng một luồng cuộn, bao gồm: bài viết văn bản dài, album hình ảnh, video, và trực tiếp chia sẻ tài liệu/đoạn code.
- **Hệ thống Tương tác chuyên sâu:** Người dùng có thể tương tác trực tiếp qua bình luận, bộ thả cảm xúc phong phú, nhắc tên người dùng khác, và chia sẻ khoảnh khắc với tính năng Story tự động biến mất sau 24h.
- **Mạng lưới Bạn bè & Gợi ý:** Hệ thống quản lý kết bạn toàn diện bao gồm gửi/nhận, đồng ý/từ chối lời mời. Giao diện Gợi ý kết bạn với các thao tác (ví dụ như hủy yêu cầu) sẽ lập tức cập nhật trạng thái hiển thị ngay tại chỗ thay vì làm biến mất phần tử, giữ cho luồng cuộn và trải nghiệm người dùng luôn liền mạch.
- **Không gian Nhóm độc lập:** Cho phép người dùng khởi tạo, tham gia và quản trị các cộng đồng học tập riêng. Mỗi nhóm sở hữu một không gian độc lập với bảng tin riêng, hệ thống quản lý thành viên và phân quyền quản trị viên chặt chẽ.
- **Trò chuyện Real-time đa luồng:** Tích hợp kiến trúc Socket.io mang đến trải nghiệm nhắn tin 1-1 với độ trễ cực thấp. Hỗ trợ hiển thị trạng thái hoạt động (Online/Offline).
- **Gọi video & Livestream chất lượng cao:** Ứng dụng công nghệ ZegoCloud để cung cấp tính năng gọi thoại/video trực tiếp và phát sóng trực tiếp (Livestreaming). Hoạt động mượt mà ngay cả trong môi trường mạng yếu, tối ưu hóa cho các buổi học nhóm hoặc chia sẻ kiến thức trực tuyến.

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

### 📱 Frontend (Android Mobile App)
- **Ngôn ngữ & Kiến trúc:** Kotlin / Java (Min SDK 26, Target SDK 36), kiến trúc MVVM (ViewModel, LiveData)
- **Networking:** Retrofit, OkHttp (với Logging Interceptor)
- **Real-time:** Socket.io-client
- **AI & Computer Vision:** TensorFlow Lite, Google ML Kit (Face Detection), CameraX, Text Recognition (OCR offline)
- **Image Processing & UI Tools:** UCrop, CodeView, PhotoView
- **Streaming & Calling:** ZegoCloud (UIKit Prebuilt Live Streaming & Signaling)
- **UI & Animation:** Material Design, Lottie, Glide, CircleImageView
- **Native C++:** External Native Build bằng CMake

---

## 📁 Cấu trúc dự án

```text
Society_Mobile/
├── backend/                  # Mã nguồn server Node.js & REST API
│   ├── src/                  # Source code TypeScript (Controllers, Models, Routes)
│   ├── .env.example          # Template cấu hình biến môi trường
│   ├── package.json          # Quản lý dependencies (npm)
│   └── tsconfig.json         # Cấu hình TypeScript
└── frontend/                 # Mã nguồn ứng dụng Android
    ├── app/                  # Module chính của ứng dụng
    │   ├── src/main/cpp/     # Mã nguồn Native C++ & CMakeLists
    │   ├── src/main/java/    # Source code Android (Kotlin/Java)
    │   └── build.gradle.kts  # Build script cho module App
    ├── gradle/               # Gradle wrapper
    └── build.gradle.kts      # Cấu hình build project root
```

---

## 🚀 Hướng dẫn cài đặt và chạy dự án

### Yêu cầu hệ thống
- **Backend:** Node.js (v18+ khuyến nghị), MongoDB, tài khoản Cloudinary, API Key cho Google Generative AI.
- **Frontend:** Android Studio (phiên bản mới nhất), JDK 11+.

### 1. Thiết lập Backend
1. Di chuyển vào thư mục backend:
   ```bash
   cd backend
   ```
2. Cài đặt các gói thư viện phụ thuộc:
   ```bash
   npm install
   ```
3. Tạo file `.env` từ cấu hình mẫu và điền các thông tin cần thiết:
   - `PORT`, `MONGO_URI`, `JWT_SECRET`
   - Cấu hình Cloudinary
   - `GOOGLE_API_KEY` (Sử dụng cho tính năng tạo Quiz bằng AI)
4. Chạy server ở chế độ phát triển:
   ```bash
   npm run dev
   ```
   *Server sẽ khởi động và lắng nghe tại `http://localhost:<PORT>`*

### 2. Thiết lập Frontend (Android)
1. Mở thư mục `frontend` bằng Android Studio.
2. Đợi Gradle đồng bộ dự án (Sync Project with Gradle Files).
3. Thêm các cấu hình API Key cho ZegoCloud, Facebook Login, và thêm file `google-services.json` vào thư mục `app/` (nếu dùng xác thực của Google).
4. Cập nhật `BASE_URL` trỏ về API server của Backend trong file cấu hình Retrofit.
5. Chạy ứng dụng trên máy ảo (Emulator) hoặc thiết bị thật.

---

## 📝 Tài liệu API (API Documentation)
Toàn bộ tài liệu API được tự động hóa (auto-generated) bằng Swagger. 
Sau khi chạy backend thành công, bạn có thể xem và thử nghiệm trực tiếp các API tại:
👉 `http://localhost:<PORT>/docs`

---
