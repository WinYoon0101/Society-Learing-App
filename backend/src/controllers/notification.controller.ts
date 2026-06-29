import { Request, Response } from 'express';
import Notification from '../models/notification.model';

interface AuthRequest extends Request {
    user?: {
        id: string;
    };
}

export const getNotifications = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const userId = req.user?.id;
        if (!userId) return res.status(401).json({ message: 'Không tìm thấy thông tin người dùng' });

        // 1. Lấy danh sách thông báo từ DB
        const notifications = await Notification.find({ recipient: userId })
            .populate('sender', 'username avatar')
            .sort({ createdAt: -1 });

        // 2. Dựa vào trường "type" của DB để sinh ra "targetType" động
        const formattedNotifications = notifications.map((n: any) => {
            let targetType = 'Post'; // Giá trị mặc định

            if (n.type.startsWith('post_') || n.type.startsWith('comment_') || n.type === 'group_post_approved') {
                targetType = 'Post';
            } else if (n.type.startsWith('group_')) {
                targetType = 'Group';
            } else if (n.type.startsWith('friend_')) {
                targetType = 'Friend';
            }

            // Gộp thêm trường targetType vào Object
            return {
                ...n.toObject(), 
                targetType: targetType
            };
        });

        const unreadCount = await Notification.countDocuments({ recipient: userId, isRead: false });

        // 3. Trả về dữ liệu đã được định dạng 
        return res.status(200).json({
            success: true,
            data: formattedNotifications, // Gửi mảng đã có targetType động
            unreadCount: unreadCount
        });
    } catch (error) {
        console.error('Lỗi lấy thông báo:', error);
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};

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

export const markAllAsRead = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        await Notification.updateMany({ recipient: req.user?.id, isRead: false }, { isRead: true });
        return res.status(200).json({ success: true, message: 'Đã đánh dấu tất cả' });
    } catch (error) {
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};

export const deleteNotification = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const result = await Notification.findOneAndDelete({ _id: req.params.id, recipient: req.user?.id });
        if (!result) return res.status(404).json({ message: 'Thông báo không tồn tại' });
        return res.status(200).json({ success: true, message: 'Đã xóa' });
    } catch (error) {
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};
