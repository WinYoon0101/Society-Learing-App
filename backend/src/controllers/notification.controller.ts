import { Response } from "express";
import Notification from "../models/notification.model";
import { AuthRequest } from "../middlewares/auth.middleware";

// Lấy danh sách thông báo của User
export const getNotifications = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.user?.id;
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 20;
    const skip = (page - 1) * limit;

    if (!userId) {
      res.status(401).json({ success: false, message: "Không tìm thấy thông tin xác thực." });
      return;
    }

    // Lấy danh sách thông báo gửi cho userId
    const notifications = await Notification.find({ receiverId: userId })
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit)
      .populate("senderId", "_id username avatar"); // Lấy thông tin người tương tác

    // Đếm số thông báo chưa đọc (để FE hiện số trên chuông)
    const unreadCount = await Notification.countDocuments({ receiverId: userId, isRead: false });

    res.status(200).json({
      success: true,
      data: notifications,
      unreadCount,
      currentPage: page,
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi lấy thông báo", error });
  }
};

// Đánh dấu 1 thông báo là đã đọc
export const markAsRead = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const notificationId = req.params.id;
    const userId = req.user?.id;

    const notification = await Notification.findOneAndUpdate(
      { _id: notificationId, receiverId: userId },
      { isRead: true },
      { new: true }
    );

    if (!notification) {
      res.status(404).json({ success: false, message: "Không tìm thấy thông báo." });
      return;
    }

    res.status(200).json({ success: true, message: "Đã đánh dấu đọc." });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};

// Đánh dấu đọc TẤT CẢ thông báo
export const markAllAsRead = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.user?.id;

    await Notification.updateMany(
      { receiverId: userId, isRead: false },
      { isRead: true }
    );

    res.status(200).json({ success: true, message: "Đã đánh dấu đọc tất cả." });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};