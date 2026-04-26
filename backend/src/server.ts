import { httpServer } from "./app";
import dotenv from "dotenv";
dotenv.config();

import connectDB from "./config/database";

const PORT = Number(process.env.PORT) || 3000;

const startServer = async () => {
  await connectDB();

  httpServer.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 Server đang chạy tại http://localhost:${PORT}`);
    console.log(`🔌 WebSocket sẵn sàng tại ws://localhost:${PORT}`);
  });
};

startServer().catch((error) => {
  console.error("❌ Lỗi khởi động server:", error);
  process.exit(1);
});
