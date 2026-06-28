import { Router } from "express";
import { authenticate } from "../middlewares/auth.middleware";
import {
  getConversations,
  getOrCreateConversation,
  getMessages,
  sendMessage,
  setNickname,
  setColor,
  deleteMessage,
  getUnreadCount,
  createGroup,
  addMembers,
  kickMember,
  leaveGroup,
  renameGroup,
  deleteConversation,
  setMute,
  deleteMessageForMe,
  callLog,
  getMutedCalls,
} from "../controllers/chat.controller";

const router = Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);

// Conversations
router.get("/conversations", getConversations);
router.get("/unread-count", getUnreadCount);
router.get("/muted-calls", getMutedCalls);
router.post("/conversations", getOrCreateConversation);
router.post("/conversations/group", createGroup);

// Messages
router.get("/conversations/:conversationId/messages", getMessages);
router.post("/messages", sendMessage);

// Customization
router.patch("/conversations/:conversationId/nickname", setNickname);
router.patch("/conversations/:conversationId/color", setColor);
router.patch("/conversations/:conversationId/name", renameGroup);

// Group management
router.post("/conversations/:conversationId/members", addMembers);
router.delete("/conversations/:conversationId/members/:userId", kickMember);
router.post("/conversations/:conversationId/leave", leaveGroup);

// Call log (G7.4)
router.post("/conversations/:conversationId/call-log", callLog);

// Mute & delete conversation (ẩn-phía-mình)
router.patch("/conversations/:conversationId/mute", setMute);
router.delete("/conversations/:conversationId", deleteConversation);

// Delete message
router.delete("/messages/:messageId/me", deleteMessageForMe);
router.delete("/messages/:messageId", deleteMessage);

export default router;
