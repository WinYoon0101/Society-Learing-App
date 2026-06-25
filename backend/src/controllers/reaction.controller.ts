import { Request, Response } from 'express';
import Reaction from '../models/reaction.model';
import Post from '../models/post.model';
import Notification from '../models/notification.model';
import User from '../models/user.model';

export const toggleReaction = async (req: Request, res: Response): Promise<void> => {
    try {
        // Lấy ID người dùng từ token (hoặc từ body nếu test)
        const userId = (req as any).user?.id || req.body.userId; 
        const { targetId, targetType, type } = req.body;

        if (!userId || !targetId || !targetType || !type) {
            res.status(400).json({ message: "Thiếu dữ liệu đầu vào!" });
            return;
        }

        const existingReaction = await Reaction.findOne({ userId, targetId });

        if (existingReaction) {
            if (existingReaction.type === type) {
                // 1. HỦY CẢM XÚC (Bấm lại nút cũ)
                await Reaction.findByIdAndDelete(existingReaction._id);
                
                // Thu hồi thông báo tương ứng
                await Notification.deleteOne({ 
                    sender: userId, 
                    targetId: targetId, 
                    type: "post_reaction" 
                }).catch(err => console.error("Lỗi xóa thông báo:", err.message));

                res.status(200).json({ message: "Đã thu hồi cảm xúc", action: "REMOVED" });
            } else {
                // 2. ĐỔI CẢM XÚC (VD: Đổi từ Like sang Love)
                existingReaction.type = type;
                await existingReaction.save();
                res.status(200).json({ message: "Đã cập nhật cảm xúc", action: "UPDATED", data: existingReaction });
            }
        } else {
            // 3. THẢ CẢM XÚC MỚI
            const newReaction = new Reaction({ userId, targetId, targetType, type });
            await newReaction.save();

            // === TẠO THÔNG BÁO BỌC THÉP ===
            if (targetType.toLowerCase() === 'post') {
                const post = await Post.findById(targetId);
                
                // 1. Lấy ID an toàn (Đề phòng bài viết cũ dùng 'author' thay vì 'authorId')
                const authorOfPost = post?.authorId || (post as any)?.author;
                const senderOfReaction = userId; 
                
                // 2. Kẻ vạch an toàn: Phải có đủ cả người nhận, người gửi và không tự thả tim chính mình
                if (post && authorOfPost && senderOfReaction && authorOfPost.toString() !== senderOfReaction.toString()) {
                    // Tìm thông tin người gửi để lấy tên
                    const senderInfo = await User.findById(senderOfReaction).select('username');
                    // Nếu không tìm thấy tên thì để mặc định là 'Ai đó'
                    const senderName = senderInfo?.username || 'Ai đó';
                    
                    await Notification.create({
                        recipient: authorOfPost, 
                        sender: senderOfReaction,
                        type: 'post_reaction',
                        targetId: targetId,
                        content: `${senderName} đã bày tỏ cảm xúc về bài viết của bạn`
                    }).catch(err => console.error("Lỗi Mongoose khi lưu thông báo:", err.message));
                } else {
                    console.log("Bỏ qua tạo thông báo (Do thiếu ID bài viết hoặc tự thả tim chính mình)");
                }
            }
            // =============================

            res.status(201).json({ message: "Đã thả cảm xúc thành công", action: "ADDED", data: newReaction });
        }
    } catch (error: any) {
        console.error("Lỗi toggleReaction:", error);
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