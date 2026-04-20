import { Router } from "express";
import { authenticate } from "../middlewares/auth.middleware";
import {
  getConversations,
  getOrCreateConversation,
  getMessages,
  setNickname,
  setColor,
  deleteMessage,
} from "../controllers/chat.controller";

const router = Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);

// Conversations
router.get("/conversations", getConversations);
router.post("/conversations", getOrCreateConversation);

// Messages
router.get("/conversations/:conversationId/messages", getMessages);

// Customization
router.patch("/conversations/:conversationId/nickname", setNickname);
router.patch("/conversations/:conversationId/color", setColor);

// Delete message
router.delete("/messages/:messageId", deleteMessage);

export default router;
