import express from "express";
import cors from "cors";
import { createServer } from "http";
import { Server } from "socket.io";
import routes from "./routes";
import swaggerDocs from "./swagger/swagger";
import { initChatSocket } from "./socket/chat.socket";

const app = express();
const httpServer = createServer(app);

const io = new Server(httpServer, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"],
  },
});

app.use(cors());
app.use(express.json());

app.use("/api", routes);

swaggerDocs(app);

// Khởi tạo WebSocket chat
initChatSocket(io);

export { httpServer };
export default app;
