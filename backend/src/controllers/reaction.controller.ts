import { Request, Response } from 'express';
import Reaction from '../models/reaction.model'; // Đổi đường dẫn cho đúng với project của bạn

export const toggleReaction = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.id || req.body.userId; 

        // 2. Lấy dữ liệu Android gửi lên
        const { targetId, targetType, type } = req.body;

        // Validate cơ bản
        if (!userId || !targetId || !targetType || !type) {
            res.status(400).json({ message: "Thiếu dữ liệu đầu vào!" });
            return;
        }

        // 3. Tìm xem user này đã từng thả cảm xúc vào bài viết/comment này chưa
        const existingReaction = await Reaction.findOne({ 
            userId: userId, 
            targetId: targetId 
        });

        if (existingReaction) {
            // TRƯỜNG HỢP A: Đã từng thả cảm xúc rồi
            if (existingReaction.type === type) {
                // Hành động: Bấm lại đúng icon cũ -> THU HỒI (Unlike)
                await Reaction.findByIdAndDelete(existingReaction._id);
                
                res.status(200).json({ 
                    message: "Đã thu hồi cảm xúc", 
                    action: "REMOVED" 
                });
            } else {
                // Hành động: Bấm icon khác -> CẬP NHẬT (Ví dụ: Like -> Love)
                existingReaction.type = type;
                await existingReaction.save();
                
                res.status(200).json({ 
                    message: "Đã cập nhật cảm xúc", 
                    action: "UPDATED",
                    data: existingReaction 
                });
            }
        } else {
            // TRƯỜNG HỢP B: Chưa thả bao giờ -> TẠO MỚI
            const newReaction = new Reaction({
                userId,
                targetId,
                targetType, 
                type        
            });

            await newReaction.save();
            
            res.status(201).json({ 
                message: "Đã thả cảm xúc thành công", 
                action: "ADDED",
                data: newReaction 
            });
        }

    } catch (error: any) {
        console.error("Lỗi Reaction Controller: ", error);
        res.status(500).json({ message: "Lỗi Server", error: error.message });
    }
};