import { Server, Socket } from "socket.io";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import { JWT_SECRET } from "../config/env";
import Message from "../models/message.model";
import Conversation from "../models/conversation.model";
import User from "../models/user.model";

// Map lưu userId -> Set<socketId> (1 user có thể mở nhiều tab)
const onlineUsers = new Map<string, Set<string>>();

// Giữ ref tới io để controller (REST) có thể emit (vd system message)
let ioRef: Server | null = null;

/** Emit 1 event tới tất cả socket trong room conversation (dùng từ controller REST). */
export function emitToConversation(conversationId: string, event: string, payload: any) {
  if (ioRef) ioRef.to(conversationId).emit(event, payload);
}

function addOnlineUser(userId: string, socketId: string) {
  if (!onlineUsers.has(userId)) onlineUsers.set(userId, new Set());
  onlineUsers.get(userId)!.add(socketId);
}

function removeOnlineUser(userId: string, socketId: string) {
  const sockets = onlineUsers.get(userId);
  if (sockets) {
    sockets.delete(socketId);
    if (sockets.size === 0) onlineUsers.delete(userId);
  }
}

export function isUserOnline(userId: string): boolean {
  return onlineUsers.has(userId) && onlineUsers.get(userId)!.size > 0;
}

export function getOnlineUsers(): string[] {
  return Array.from(onlineUsers.keys());
}

// Xác thực socket qua JWT token
async function authenticateSocket(
  socket: Socket
): Promise<{ id: string; username: string; email: string } | null> {
  try {
    const token =
      socket.handshake.auth?.token ||
      socket.handshake.headers?.authorization?.replace("Bearer ", "") ||
      socket.handshake.query?.token;

    if (!token) {
       console.log("Huhu không thấy token đâu cả!");
       return null;
    }

    const decoded = jwt.verify(token, JWT_SECRET) as {
      id: string;
      email: string;
      username: string;
    };

    const user = await User.findById(decoded.id);
    if (!user || !user.isActive) return null;

    return decoded;
  } catch {
    return null;
  }
}

export function initChatSocket(io: Server) {
  ioRef = io;
  io.on("connection", async (socket: Socket) => {
    try {
      const user = await authenticateSocket(socket);

      if (!user) {
        socket.emit("error", { message: "Xác thực thất bại" });
        socket.disconnect();
        return;
      }

      const userId = user.id;
      addOnlineUser(userId, socket.id);

      // Thông báo cho bạn bè biết user online
      socket.broadcast.emit("user:online", { userId });

      console.log(`✅ User ${user.username} connected [${socket.id}]`);

      // Join vào các room conversation của user
      try {
        const conversations = await Conversation.find({ members: userId });
        conversations.forEach((conv) => {
          socket.join(conv._id.toString());
        });
      } catch (convErr) {
        console.error(`⚠️ Lỗi join conversation rooms cho ${user.username}:`, convErr);
        // Không disconnect - chỉ bỏ qua bước join rooms
      }

    // Helper: join room cho conversation mới tạo
    const joinConversationRoom = (conversationId: string) => {
      socket.join(conversationId);
    };

    // ─── GỬI TIN NHẮN ───────────────────────────────────────────────
    socket.on(
      "message:send",
      async (data: {
        conversationId: string;
        text?: string;
        replyTo?: string;
        mediaUrl?: string;
        mediaType?: string;
      }) => {
        try {
          const { conversationId, text, replyTo, mediaUrl, mediaType } = data;

          // Phải có text hoặc mediaUrl
          if (!text?.trim() && !mediaUrl) return;

          // Kiểm tra user có trong conversation không
          const conversation = await Conversation.findOne({
            _id: conversationId,
            members: userId,
          });

          if (!conversation) {
            socket.emit("error", { message: "Không có quyền gửi tin nhắn" });
            return;
          }

          // Đảm bảo socket đã join room (trường hợp conversation mới tạo sau khi connect)
          joinConversationRoom(conversationId);

          // Tạo message
          const messageData: Record<string, any> = {
            conversationId,
            sender: userId,
            text: text?.trim() || "",
          };
          if (replyTo) messageData.replyTo = new mongoose.Types.ObjectId(replyTo);
          if (mediaUrl) messageData.mediaUrl = mediaUrl;
          if (mediaType) messageData.mediaType = mediaType;

          const message = await Message.create(messageData);

          // Cập nhật lastMessage + un-hide cho mọi người đã xóa-phía-mình (tin mới → hiện lại)
          await Conversation.findByIdAndUpdate(conversationId, {
            lastMessage: message._id,
            updatedAt: new Date(),
            deletedBy: [],
          });

          // Populate để gửi về client — đảm bảo _id là string
          const populated = await Message.findById(message._id)
            .populate("sender", "username avatar _id")
            .populate({
              path: "replyTo",
              populate: { path: "sender", select: "username avatar _id" },
            })
            .lean(); // lean() trả plain object, _id tự convert thành string khi JSON.stringify

          // Broadcast tới tất cả members trong room
          io.to(conversationId).emit("message:new", populated);
        } catch (error) {
          console.error("message:send error:", error);
          socket.emit("error", { message: "Lỗi gửi tin nhắn" });
        }
      }
    );

    // ─── THẢ CẢM XÚC ────────────────────────────────────────────────
    socket.on(
      "message:react",
      async (data: { messageId: string; emoji: string }) => {
        try {
          const { messageId, emoji } = data;

          const validEmojis = ["❤️", "😆", "😮", "😢", "😡", "👍"];
          if (!validEmojis.includes(emoji)) {
            socket.emit("error", { message: "Emoji không hợp lệ" });
            return;
          }

          const message = await Message.findById(messageId);
          if (!message) return;

          // Kiểm tra quyền
          const conversation = await Conversation.findOne({
            _id: message.conversationId,
            members: userId,
          });
          if (!conversation) return;

          // Toggle reaction: nếu đã react cùng emoji thì bỏ, khác emoji thì đổi
          const existingIdx = message.reactions.findIndex(
            (r) => r.userId.toString() === userId
          );

          const userObjectId = new mongoose.Types.ObjectId(userId);

          if (existingIdx !== -1) {
            if (message.reactions[existingIdx].emoji === emoji) {
              // Bỏ reaction
              message.reactions.splice(existingIdx, 1);
            } else {
              // Đổi emoji
              message.reactions[existingIdx].emoji = emoji as any;
            }
          } else {
            message.reactions.push({ userId: userObjectId, emoji: emoji as any });
          }

          await message.save();

          io.to(message.conversationId.toString()).emit("message:reacted", {
            messageId,
            reactions: message.reactions,
          });
        } catch (error) {
          socket.emit("error", { message: "Lỗi thả cảm xúc" });
        }
      }
    );

    // ─── THU HỒI TIN NHẮN ───────────────────────────────────────────
    socket.on("message:delete", async (data: { messageId: string }) => {
      try {
        const message = await Message.findOneAndUpdate(
          { _id: data.messageId, sender: userId },
          { isDeleted: true, text: "Tin nhắn đã bị thu hồi" },
          { returnDocument: 'after' }
        );

        if (!message) return;

        io.to(message.conversationId.toString()).emit("message:deleted", {
          messageId: data.messageId,
          conversationId: message.conversationId,
        });
      } catch (error) {
        socket.emit("error", { message: "Lỗi thu hồi tin nhắn" });
      }
    });

    // ─── JOIN ROOM CONVERSATION (vd nhóm mới tạo / vừa được add sau connect) ──
    socket.on(
      "conversation:join",
      async (data: { conversationId: string } | string) => {
        try {
          const conversationId =
            typeof data === "string" ? data : data?.conversationId;
          if (!conversationId) return;
          // Chỉ join nếu là thành viên
          const conv = await Conversation.findOne({
            _id: conversationId,
            members: userId,
          });
          if (conv) {
            socket.join(conversationId.toString());
          }
        } catch (err) {
          console.error("conversation:join error:", err);
        }
      }
    );

    // ─── ĐANG GÕ ────────────────────────────────────────────────────
    socket.on("typing:start", (data: { conversationId: string }) => {
      socket.to(data.conversationId).emit("typing:start", {
        conversationId: data.conversationId,
        userId,
        username: user.username,
      });
    });

    socket.on("typing:stop", (data: { conversationId: string }) => {
      socket.to(data.conversationId).emit("typing:stop", {
        conversationId: data.conversationId,
        userId,
      });
    });

    // ─── LẤY DANH SÁCH ONLINE ───────────────────────────────────────
    socket.on("users:online", () => {
      socket.emit("users:online", { onlineUsers: getOnlineUsers() });
    });

    // ─── DISCONNECT ─────────────────────────────────────────────────
    socket.on("disconnect", () => {
      removeOnlineUser(userId, socket.id);
      socket.broadcast.emit("user:offline", { userId });
      console.log(`❌ User ${user.username} disconnected [${socket.id}]`);
    });

    } catch (err) {
      console.error("❌ Lỗi không bắt được trong socket connection handler:", err);
      socket.disconnect();
    }
  });
}
