import { Response } from "express";
import mongoose from "mongoose";
import { AuthRequest } from "../middlewares/auth.middleware";
import Conversation from "../models/conversation.model";
import Message from "../models/message.model";
import User from "../models/user.model";
import { emitToConversation } from "../socket/chat.socket";

// Lấy username (dùng cho nội dung system message)
const getUserName = async (userId: any): Promise<string> => {
  try {
    const u = await User.findById(userId).select("username");
    return u?.username || "Người dùng";
  } catch {
    return "Người dùng";
  }
};

/**
 * Tạo 1 tin nhắn hệ thống (isSystem) trong conversation rồi broadcast qua socket.
 * Hiển thị chữ xám giữa màn, không tương tác. `actorId` = người gây ra sự kiện (làm sender).
 */
const sendSystemMessage = async (
  conversationId: any,
  actorId: any,
  text: string
): Promise<void> => {
  try {
    const message = await Message.create({
      conversationId,
      sender: actorId,
      text,
      isSystem: true,
    });
    await Conversation.findByIdAndUpdate(conversationId, {
      lastMessage: message._id,
      updatedAt: new Date(),
      deletedBy: [],
    });
    const populated = await Message.findById(message._id)
      .populate("sender", "username avatar _id")
      .lean();
    emitToConversation(conversationId.toString(), "message:new", populated);
  } catch (err) {
    console.error("sendSystemMessage error:", err);
  }
};

// GET /api/chat/conversations - Lấy danh sách cuộc trò chuyện của user
export const getConversations = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;

    const conversations = await Conversation.find({
      members: userId,
      deletedBy: { $ne: userId }, // ẩn conversation đã xóa-phía-mình
    })
      .populate("members", "username avatar isActive _id")
      .populate({
        path: "lastMessage",
        populate: { path: "sender", select: "username avatar _id" },
      })
      .sort({ updatedAt: -1 });

    // Tính cờ "unread" cho từng conversation (chấm xanh kiểu Messenger) — không query thêm:
    // chưa đọc nếu lastMessage do người khác gửi (không phải system) & mới hơn lastRead của mình.
    const result = conversations.map((conv) => {
      const obj: any = conv.toObject({ flattenMaps: true });
      const last: any = obj.lastMessage;
      // Đã tắt thông báo tin nhắn cho user này?
      const muted =
        Array.isArray(obj.mutedMessages) &&
        obj.mutedMessages.some((u: any) => u.toString() === userId.toString());
      // Đã tắt thông báo CUỘC GỌI cho user này? (hiện icon riêng ở list — G7.5)
      const mutedCall =
        Array.isArray(obj.mutedCalls) &&
        obj.mutedCalls.some((u: any) => u.toString() === userId.toString());

      let unread = false;
      // Conversation đã mute thì KHÔNG tính chưa-đọc (không chấm xanh / không in đậm)
      if (!muted && last && !last.isSystem && last.sender) {
        const senderId = (last.sender._id || last.sender).toString();
        if (senderId !== userId.toString()) {
          const lastReadRaw = obj.lastRead ? obj.lastRead[userId.toString()] : null;
          const lastReadAt = lastReadRaw ? new Date(lastReadRaw) : null;
          if (!lastReadAt || new Date(last.createdAt) > lastReadAt) {
            unread = true;
          }
        }
      }
      obj.unread = unread;
      obj.muted = muted;
      obj.mutedCall = mutedCall;
      return obj;
    });

    res.json({ success: true, data: result });
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
    })
    .populate("members", "username avatar isActive")
    .populate({
      path: "lastMessage",
      populate: {
        path: "sender",
        select: "username avatar _id",
      },
    });

    // Mở lại 1 conversation từng xóa-phía-mình → un-hide
    if (conversation && conversation.deletedBy?.length) {
      await Conversation.findByIdAndUpdate(conversation._id, {
        $pull: { deletedBy: userId },
      });
    }

    if (!conversation) {
      conversation = await Conversation.create({
        members: [userId, targetUserId],
      });
        conversation = await conversation.populate([
          {
            path: "members",
            select: "username avatar isActive",
          },
          {
            path: "lastMessage",
            populate: {
              path: "sender",
              select: "username avatar _id",
            },
          },
        ]);
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

    // Mở chat = đánh dấu đã đọc tới hiện tại (badge unread về 0). timestamps:false để KHÔNG
    // bump updatedAt → tránh conversation bị đẩy lên đầu list chỉ vì xem.
    await Conversation.updateOne(
      { _id: conversationId },
      { $set: { [`lastRead.${userId}`]: new Date() } },
      { timestamps: false }
    );

    const messages = await Message.find({
      conversationId,
      deletedFor: { $ne: userId }, // ẩn message đã xóa-phía-mình
    })
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

    const actorName = await getUserName(userId);
    const targetName = await getUserName(targetUserId);
    const text = nickname && nickname.trim()
      ? `${actorName} đã đặt biệt danh cho ${targetName} là "${nickname.trim()}"`
      : `${actorName} đã đặt lại biệt danh của ${targetName}`;
    await sendSystemMessage(conversationId, userId, text);

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

// GET /api/chat/unread-count - Tổng số tin nhắn chưa xem (1-1 + group)
export const getUnreadCount = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const conversations = await Conversation.find({
      members: userId,
      deletedBy: { $ne: userId },
      mutedMessages: { $ne: userId }, // bỏ qua conversation đã tắt thông báo tin nhắn
    }).select("_id lastRead");

    let total = 0;
    await Promise.all(
      conversations.map(async (conv) => {
        const lastRead = conv.lastRead?.get(userId.toString()) || new Date(0);
        const count = await Message.countDocuments({
          conversationId: conv._id,
          sender: { $ne: userId },
          isSystem: { $ne: true },
          deletedFor: { $ne: userId },
          createdAt: { $gt: lastRead },
        });
        total += count;
      })
    );

    res.json({ success: true, data: { count: total } });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// POST /api/chat/conversations/group - Tạo group chat
export const createGroup = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { name, memberIds } = req.body as { name?: string; memberIds?: string[] };

    if (!Array.isArray(memberIds) || memberIds.length < 2) {
      res.status(400).json({ success: false, message: "Nhóm cần ít nhất 3 thành viên" });
      return;
    }

    // Gộp creator + members, loại trùng
    const unique = Array.from(new Set([userId, ...memberIds.map(String)]));

    let conversation = await Conversation.create({
      members: unique,
      isGroup: true,
      name: name?.trim() || "",
      admin: userId,
    });

    conversation = await conversation.populate([
      { path: "members", select: "username avatar isActive _id" },
      { path: "lastMessage", populate: { path: "sender", select: "username avatar _id" } },
    ]);

    const creatorName = await getUserName(userId);
    await sendSystemMessage(conversation._id, userId, `${creatorName} đã tạo nhóm`);

    res.status(201).json({ success: true, data: conversation });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// POST /api/chat/conversations/:conversationId/members - Thêm thành viên (1-1 sẽ thành group)
export const addMembers = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;
    const { userIds } = req.body as { userIds?: string[] };

    if (!Array.isArray(userIds) || userIds.length === 0) {
      res.status(400).json({ success: false, message: "Thiếu userIds" });
      return;
    }

    const conversation = await Conversation.findOne({ _id: conversationId, members: userId });
    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }
    // Chỉ thêm người vào GROUP (thêm người vào 1-1 → dùng luồng tạo nhóm mới riêng)
    if (!conversation.isGroup) {
      res.status(400).json({ success: false, message: "Chỉ thêm người vào nhóm" });
      return;
    }

    const updated = await Conversation.findByIdAndUpdate(
      conversationId,
      { $addToSet: { members: { $each: userIds } } },
      { new: true }
    )
      .populate("members", "username avatar isActive _id")
      .populate({
        path: "lastMessage",
        populate: { path: "sender", select: "username avatar _id" },
      });

    const actorName = await getUserName(userId);
    const addedNames = (await Promise.all(userIds.map((id) => getUserName(id)))).join(", ");
    await sendSystemMessage(conversationId, userId, `${actorName} đã thêm ${addedNames}`);

    res.json({ success: true, data: updated });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// DELETE /api/chat/conversations/:conversationId/members/:userId - Kick thành viên (chỉ admin)
export const kickMember = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const adminId = req.user!.id;
    const { conversationId, userId: targetId } = req.params;

    const conversation = await Conversation.findOne({ _id: conversationId, members: adminId });
    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }
    if (!conversation.isGroup) {
      res.status(400).json({ success: false, message: "Chỉ áp dụng cho nhóm" });
      return;
    }
    if (!conversation.admin || conversation.admin.toString() !== adminId) {
      res.status(403).json({ success: false, message: "Chỉ admin mới xóa được thành viên" });
      return;
    }
    if (targetId === adminId) {
      res.status(400).json({ success: false, message: "Không thể tự xóa — dùng rời nhóm" });
      return;
    }
    if (!conversation.members.some((m) => m.toString() === targetId)) {
      res.status(404).json({ success: false, message: "Thành viên không có trong nhóm" });
      return;
    }

    await Conversation.findByIdAndUpdate(conversationId, { $pull: { members: targetId } });

    const actorName = await getUserName(adminId);
    const targetName = await getUserName(targetId);
    await sendSystemMessage(conversationId, adminId, `${actorName} đã xóa ${targetName} khỏi nhóm`);

    const updated = await Conversation.findById(conversationId)
      .populate("members", "username avatar isActive _id")
      .populate({ path: "lastMessage", populate: { path: "sender", select: "username avatar _id" } });

    res.json({ success: true, data: updated });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// POST /api/chat/conversations/:conversationId/leave - Rời nhóm
export const leaveGroup = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;

    const conversation = await Conversation.findOne({ _id: conversationId, members: userId });
    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }
    if (!conversation.isGroup) {
      res.status(400).json({ success: false, message: "Chỉ rời được nhóm" });
      return;
    }

    const remaining = conversation.members.filter((m) => m.toString() !== userId);

    // Nhóm rỗng → xóa hẳn
    if (remaining.length === 0) {
      await Conversation.findByIdAndDelete(conversationId);
      res.json({ success: true, message: "Đã rời và xóa nhóm rỗng" });
      return;
    }

    const update: Record<string, any> = { $pull: { members: userId } };
    // Admin rời → chuyển cho thành viên còn lại đầu tiên
    if (conversation.admin && conversation.admin.toString() === userId) {
      update.admin = remaining[0];
    }
    await Conversation.findByIdAndUpdate(conversationId, update);

    const leaverName = await getUserName(userId);
    await sendSystemMessage(conversationId, userId, `${leaverName} đã rời nhóm`);

    res.json({ success: true, message: "Đã rời nhóm" });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// PATCH /api/chat/conversations/:conversationId/name - Đổi tên nhóm
export const renameGroup = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;
    const { name } = req.body as { name?: string };

    const conversation = await Conversation.findOneAndUpdate(
      { _id: conversationId, members: userId, isGroup: true },
      { name: name?.trim() || "" },
      { new: true }
    );
    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }

    const actorName = await getUserName(userId);
    await sendSystemMessage(
      conversationId,
      userId,
      `${actorName} đã đổi tên nhóm thành "${conversation.name}"`
    );

    // Trả tối giản (tránh members chưa populate)
    res.json({ success: true, data: { name: conversation.name } });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// GET /api/chat/muted-calls - Danh sách conversationId mà user đã TẮT thông báo CUỘC GỌI (G7.5).
// Client dùng để chặn hiện màn cuộc gọi đến cho các conversation này. (Chỉ mutedCalls, KHÔNG dính mutedMessages.)
export const getMutedCalls = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const convs = await Conversation.find({
      members: userId,
      mutedCalls: userId,
    }).select("_id");
    res.json({ success: true, data: convs.map((c) => c._id.toString()) });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// POST /api/chat/conversations/:conversationId/call-log - Ghi lại 1 cuộc gọi vào đoạn chat (G7.4)
// Body: { callType?: "audio"|"video", status?: "ended"|"missed", duration?: number (giây) }
// Tạo system message kiểu "Cuộc gọi video · 0:44" / "Cuộc gọi thoại nhỡ" → cả 2 phía đều thấy.
export const callLog = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;
    const { callType, status, duration } = req.body as {
      callType?: string;
      status?: string;
      duration?: number;
    };

    const conversation = await Conversation.findOne({ _id: conversationId, members: userId });
    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }

    const kind = callType === "video" ? "video" : "thoại";
    let text: string;
    if (status === "missed") {
      text = `Cuộc gọi ${kind} nhỡ`;
    } else {
      const sec = Math.max(0, Math.floor(Number(duration) || 0));
      const mm = Math.floor(sec / 60);
      const ss = sec % 60;
      text = `Cuộc gọi ${kind} · ${mm}:${ss.toString().padStart(2, "0")}`;
    }

    await sendSystemMessage(conversationId, userId, text);
    res.json({ success: true, message: "ok" });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// DELETE /api/chat/conversations/:conversationId - Xóa đoạn chat (ẩn-phía-mình)
export const deleteConversation = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;

    const conversation = await Conversation.findOne({ _id: conversationId, members: userId });
    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }
    await Conversation.findByIdAndUpdate(conversationId, {
      $addToSet: { deletedBy: userId },
    });
    res.json({ success: true, message: "Đã xóa đoạn chat" });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// PATCH /api/chat/conversations/:conversationId/mute - Tắt/bật thông báo (per-user)
// Body: { messages?: boolean, calls?: boolean }  (true = tắt, false = bật lại)
export const setMute = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { conversationId } = req.params;
    const { messages, calls } = req.body as { messages?: boolean; calls?: boolean };

    const conversation = await Conversation.findOne({ _id: conversationId, members: userId });
    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền truy cập" });
      return;
    }

    const addToSet: Record<string, any> = {};
    const pull: Record<string, any> = {};
    if (typeof messages === "boolean") {
      if (messages) addToSet.mutedMessages = userId;
      else pull.mutedMessages = userId;
    }
    if (typeof calls === "boolean") {
      if (calls) addToSet.mutedCalls = userId;
      else pull.mutedCalls = userId;
    }
    const update: Record<string, any> = {};
    if (Object.keys(addToSet).length) update.$addToSet = addToSet;
    if (Object.keys(pull).length) update.$pull = pull;
    if (Object.keys(update).length) {
      await Conversation.findByIdAndUpdate(conversationId, update);
    }

    const updated = await Conversation.findById(conversationId).select("mutedMessages mutedCalls");
    res.json({
      success: true,
      data: {
        mutedMessages: updated?.mutedMessages?.some((u) => u.toString() === userId) || false,
        mutedCalls: updated?.mutedCalls?.some((u) => u.toString() === userId) || false,
      },
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// DELETE /api/chat/messages/:messageId/me - Xóa tin nhắn phía mình (ẩn riêng)
export const deleteMessageForMe = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { messageId } = req.params;

    const message = await Message.findById(messageId);
    if (!message) {
      res.status(404).json({ success: false, message: "Không tìm thấy tin nhắn" });
      return;
    }
    const conversation = await Conversation.findOne({
      _id: message.conversationId,
      members: userId,
    });
    if (!conversation) {
      res.status(403).json({ success: false, message: "Không có quyền" });
      return;
    }
    await Message.findByIdAndUpdate(messageId, { $addToSet: { deletedFor: userId } });
    res.json({ success: true, message: "Đã xóa tin nhắn phía bạn" });
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
