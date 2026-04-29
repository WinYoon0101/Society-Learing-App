import { Response } from "express";
import mongoose from "mongoose";
import { AuthRequest } from "../middlewares/auth.middleware";
import Conversation from "../models/conversation.model";
import Message from "../models/message.model";

// GET /api/chat/conversations - Lấy danh sách cuộc trò chuyện của user
export const getConversations = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;

    const conversations = await Conversation.find({ members: userId })
      .populate("members", "username avatar isActive _id")
      .populate({
        path: "lastMessage",
        populate: { path: "sender", select: "username avatar _id" },
      })
      .sort({ updatedAt: -1 });

    res.json({ success: true, data: conversations });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// POST /api/chat/conversations - Tạo hoặc lấy conversation 1-1
export const getOrCreateConversation = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { targetUserId } = req.body;

    if (!targetUserId) {
      res.status(400).json({ success: false, message: "targetUserId là bắt buộc" });
      return;
    }

    if (userId === targetUserId) {
      res.status(400).json({ success: false, message: "Không thể chat với chính mình" });
      return;
    }

    // Tìm conversation đã tồn tại giữa 2 user
    let conversation = await Conversation.findOne({
      members: {
        $all: [
          new mongoose.Types.ObjectId(userId),
          new mongoose.Types.ObjectId(targetUserId),
        ],
        $size: 2,
      },
    }).populate("members", "username avatar isActive");

    if (!conversation) {
      conversation = await Conversation.create({
        members: [userId, targetUserId],
      });
      conversation = await conversation.populate("members", "username avatar isActive");
    }

    res.json({ success: true, data: conversation });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// GET /api/chat/conversations/:conversationId/messages - Lấy tin nhắn (phân trang)
export const getMessages = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 30;

    // Kiểm tra user có trong conversation không
    const conversation = await Conversation.findOne({
      _id: conversationId,
      members: userId,
    });

    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }

    const messages = await Message.find({ conversationId })
      .populate("sender", "username avatar _id")
      .populate({
        path: "replyTo",
        populate: { path: "sender", select: "username avatar _id" },
      })
      .sort({ createdAt: -1 })
      .skip((page - 1) * limit)
      .limit(limit);

    res.json({
      success: true,
      data: messages.reverse(),
      pagination: { page, limit },
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// PATCH /api/chat/conversations/:conversationId/nickname - Đặt nickname
export const setNickname = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;
    const { targetUserId, nickname } = req.body;

    const conversation = await Conversation.findOne({
      _id: conversationId,
      members: userId,
    });

    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }

    conversation.nicknames.set(targetUserId, nickname);
    await conversation.save();

    res.json({ success: true, data: conversation });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// PATCH /api/chat/conversations/:conversationId/color - Đổi màu chat
export const setColor = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;
    const { color } = req.body;

    const conversation = await Conversation.findOneAndUpdate(
      { _id: conversationId, members: userId },
      { color },
      { new: true }
    );

    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }

    res.json({ success: true, data: conversation });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// DELETE /api/chat/messages/:messageId - Xóa tin nhắn (soft delete)
export const deleteMessage = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { messageId } = req.params;

    const message = await Message.findOneAndUpdate(
      { _id: messageId, sender: userId },
      { isDeleted: true, text: "Tin nhắn đã bị thu hồi" },
      { new: true }
    );

    if (!message) {
      res.status(404).json({ success: false, message: "Không tìm thấy tin nhắn" });
      return;
    }

    res.json({ success: true, data: message });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};
