import { Request, Response } from 'express';
import Notification from '../models/notification.model';

// Interface nhận diện req.user từ middleware JWT
interface AuthRequest extends Request {
    user?: {
        id: string;
    };
}

/**
 * 1. Lấy danh sách thông báo
 */
export const getNotifications = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const userId = req.user?.id;
        if (!userId) return res.status(401).json({ message: 'Không tìm thấy thông tin người dùng' });

        const notifications = await Notification.find({ recipient: userId })
            .populate('sender', 'username avatar')
            .sort({ createdAt: -1 });

        const unreadCount = await Notification.countDocuments({ recipient: userId, isRead: false });

        return res.status(200).json({
            success: true,
            data: notifications,
            unreadCount: unreadCount
        });
    } catch (error) {
        console.error('Lỗi lấy thông báo:', error);
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};

/**
 * 2. Đánh dấu một thông báo là đã đọc
 */
export const markAsRead = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const notification = await Notification.findOneAndUpdate(
            { _id: req.params.id, recipient: req.user?.id },
            { isRead: true },
            { new: true }
        );

        if (!notification) return res.status(404).json({ message: 'Không tìm thấy thông báo' });
        return res.status(200).json({ success: true, data: notification });
    } catch (error) {
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};

/**
 * 3. Đánh dấu tất cả đã đọc
 */
export const markAllAsRead = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        await Notification.updateMany({ recipient: req.user?.id, isRead: false }, { isRead: true });
        return res.status(200).json({ success: true, message: 'Đã đánh dấu tất cả' });
    } catch (error) {
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};

/**
 * 4. Xóa thông báo
 */
export const deleteNotification = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const result = await Notification.findOneAndDelete({ _id: req.params.id, recipient: req.user?.id });
        if (!result) return res.status(404).json({ message: 'Thông báo không tồn tại' });
        return res.status(200).json({ success: true, message: 'Đã xóa' });
    } catch (error) {
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};