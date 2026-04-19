import app from "./app";
import dotenv from "dotenv";
dotenv.config();

import connectDB from "./config/database";

const PORT = Number(process.env.PORT) || 3000;

const startServer = async () => {
  // Kết nối MongoDB trước khi khởi động server
  await connectDB();


app.listen(PORT, "0.0.0.0", () => {
  console.log(`🚀 Server đang chạy tại http://localhost:${PORT}`);
});
};

startServer().catch((error) => {
  console.error("❌ Lỗi khởi động server:", error);
  process.exit(1);
});