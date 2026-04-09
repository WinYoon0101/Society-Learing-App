import app from "./app";
import { PORT } from "./config/env";
import connectDB from "./config/database";

const startServer = async () => {
  // Kết nối MongoDB trước khi khởi động server
  await connectDB();

  app.listen(PORT, () => {
    console.log(`🚀 Server đang chạy tại http://localhost:${PORT}`);
  });
};

startServer().catch((error) => {
  console.error("❌ Lỗi khởi động server:", error);
  process.exit(1);
});