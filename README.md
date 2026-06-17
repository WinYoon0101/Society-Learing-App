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
- **Quản lý Lịch biểu (Calendar):** Tích hợp lịch thông minh giúp người dùng lên kế hoạch cá nhân, theo dõi các sự kiện của nhóm hoặc đặt lịch nhắc nhở cho các bài kiểm tra (Quiz).

### 🌐 Mạng xã hội & Tương tác
- **Cộng đồng kết bạn:** Hỗ trợ đăng bài (Post), bình luận (Comment), thả cảm xúc (Reaction) và chia sẻ bản tin (Story). Kết bạn, tạo và quản lý nhóm (Group) học tập.
- **Trò chuyện Real-time (Chat):** Tích hợp Socket.io cho phép nhắn tin cá nhân, nhắn tin nhóm và nhận thông báo theo thời gian thực với độ trễ cực thấp.
- **Gọi video & Livestream:** Cung cấp tính năng phát sóng trực tiếp (Livestreaming) và gọi điện trực tiếp (Calling) mượt mà thông qua nền tảng ZegoCloud.
- **Hệ thống Thông báo (Push Notifications):** Cập nhật ngay lập tức các tương tác mới (thích, bình luận, nhắc tên), lời mời kết bạn,...

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
