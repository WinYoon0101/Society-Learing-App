import { Request, Response } from 'express';
import Reaction from '../models/reaction.model';
import Post from '../models/post.model';
import Notification from '../models/notification.model';

export const toggleReaction = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.id || req.body.userId;
        const { targetId, targetType, type } = req.body;

        if (!userId || !targetId || !targetType || !type) {
            res.status(400).json({ message: "Thiếu dữ liệu đầu vào!" });
            return;
        }

        const existingReaction = await Reaction.findOne({ userId: userId, targetId: targetId });

        if (existingReaction) {
            if (existingReaction.type === type) {
                await Reaction.findByIdAndDelete(existingReaction._id);
                // Xóa thông báo cũ khi bỏ react
                await Notification.deleteOne({ senderId: userId, targetId: targetId, type: "REACTION" });
                res.status(200).json({ message: "Đã thu hồi cảm xúc", action: "REMOVED" });
            } else {
                existingReaction.type = type;
                await existingReaction.save();
                res.status(200).json({ message: "Đã cập nhật cảm xúc", action: "UPDATED", data: existingReaction });
            }
        } else {
            const newReaction = new Reaction({ userId, targetId, targetType, type });
            await newReaction.save();

            // Tạo thông báo nếu react bài viết và không phải react bài của chính mình
            if (targetType === "Post") {
                const post = await Post.findById(targetId).select("authorId").lean();
                if (post && post.authorId.toString() !== userId) {
                    const notification = new Notification({
                        receiverId: post.authorId,
                        senderId: userId,
                        targetId: targetId,
                        targetType: "Post",
                        type: "REACTION",
                    });
                    notification.save().catch(err => console.error("Lỗi tạo thông báo reaction:", err));
                }
            }

            res.status(201).json({ message: "Đã thả cảm xúc thành công", action: "ADDED", data: newReaction });
        }
    } catch (error: any) {
        res.status(500).json({ message: "Lỗi Server", error: error.message });
    }
};

export const getReactionsOfTarget = async (req: Request, res: Response): Promise<void> => {
    try {
        const { targetId } = req.params;
        const reactions = await Reaction.find({ targetId })
            .populate('userId', 'username avatar') 
            .sort({ createdAt: -1 }); 

        res.status(200).json({ success: true, data: reactions });
    } catch (error: any) {
        res.status(500).json({ success: false, message: "Lỗi Server", error: error.message });
    }
};