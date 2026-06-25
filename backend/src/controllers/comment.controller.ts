import { Request, Response } from 'express';
import Comment from '../models/comment.model'; 
import Post from '../models/post.model';
import User from '../models/user.model';
import Notification from '../models/notification.model';
import Reaction from '../models/reaction.model'; // 👉 BỔ SUNG: Import model Reaction

interface AuthRequest extends Request {
    user?: {
        id: string;
    };
}

// 👉 BỔ SUNG: Helper function để đính kèm Cảm xúc vào Comment
const attachReactionsToComments = async (comments: any[], userId?: string) => {
    return await Promise.all(comments.map(async (cmt) => {
        const commentIdObj = cmt._id;
        
        // 1. Đếm tổng số cảm xúc của bình luận này
        const countReaction = await Reaction.countDocuments({ targetId: commentIdObj, targetType: 'Comment' });
        
        // 2. Lấy cảm xúc của chính người dùng hiện tại (nếu có)
        let myReaction = null;
        if (userId) {
            const myReactDoc = await Reaction.findOne({ targetId: commentIdObj, targetType: 'Comment', userId: userId });
            if (myReactDoc) myReaction = myReactDoc.type;
        }

        // 3. Lấy 2 cảm xúc phổ biến nhất (Top Reactions)
        const topReactionsAgg = await Reaction.aggregate([
            { $match: { targetId: commentIdObj, targetType: 'Comment' } },
            { $group: { _id: "$type", count: { $sum: 1 } } },
            { $sort: { count: -1 } },
            { $limit: 2 }
        ]);
        const topReactions = topReactionsAgg.map(r => r._id);

        // Trộn data trả về
        const cmtObj = cmt.toObject ? cmt.toObject() : cmt;
        return {
            ...cmtObj,
            countReaction,
            myReaction,
            topReactions
        };
    }));
};

/**
 * 1. Viết comment mới
 */
export const createComment = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const { postId, content, parentId } = req.body;
        const userId = req.user?.id;

        if (!userId) {
            return res.status(401).json({ message: 'Không tìm thấy thông tin người dùng' });
        }

        const newComment = new Comment({
            postId: postId, 
            userId: userId, 
            content: content,
            parentId: parentId || null 
        });

        await newComment.save();
        await newComment.populate('userId', 'username avatar');

        // === TẠO THÔNG BÁO BÌNH LUẬN / TRẢ LỜI ===
        try {
            const senderInfo = await User.findById(userId).select('username');
            const senderName = senderInfo?.username || 'Ai đó';

            if (parentId) {
                // TRƯỜNG HỢP 1: LÀ TRẢ LỜI (REPLY) MỘT BÌNH LUẬN KHÁC
                const parentComment = await Comment.findById(parentId);
                
                // Gửi thông báo cho chủ của bình luận gốc 
                if (parentComment && parentComment.userId.toString() !== userId.toString()) {
                    await Notification.create({
                        recipient: parentComment.userId, 
                        sender: userId,
                        type: 'comment_reply', 
                        targetId: postId, // Nhấn vào vẫn dẫn về bài viết
                        content: `${senderName} đã trả lời bình luận của bạn`
                    });
                }
            } else {
                // TRƯỜNG HỢP 2: LÀ BÌNH LUẬN TRỰC TIẾP VÀO BÀI VIẾT
                const post = await Post.findById(postId);
                const authorOfPost = post?.authorId || (post as any)?.author;
                
                // Gửi thông báo cho tác giả bài viết
                if (post && authorOfPost && authorOfPost.toString() !== userId.toString()) {
                    await Notification.create({
                        recipient: authorOfPost, 
                        sender: userId,
                        type: 'post_comment', 
                        targetId: postId, 
                        content: `${senderName} đã bình luận về bài viết của bạn`
                    });
                }
            }
        } catch (notifError: any) {
            console.error("Lỗi Mongoose khi lưu thông báo comment:", notifError.message);
        }
        // =========================================

        return res.status(201).json({
            success: true,
            message: 'Đã thêm bình luận',
            data: newComment
        });
    } catch (error) {
        console.error('Lỗi createComment:', error);
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};

/**
 * 2. Lấy danh sách comment của một bài viết (Có phân trang)
 */
// 👉 ĐÃ SỬA Request -> AuthRequest để lấy req.user?.id
export const getCommentsByPost = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const { postId } = req.params;
        const page = parseInt(req.query.page as string) || 1;
        const limit = parseInt(req.query.limit as string) || 10;
        const skip = (page - 1) * limit;

        const query = { postId: postId, parentId: null };

        const comments = await Comment.find(query)
            .populate('userId', 'username avatar') 
            .sort({ createdAt: -1 }) 
            .skip(skip)
            .limit(limit);

        const total = await Comment.countDocuments(query);

        // 👉 BỔ SUNG: Chèn Cảm xúc vào dữ liệu trả về
        const finalComments = await attachReactionsToComments(comments, req.user?.id);

        return res.status(200).json({
            success: true,
            data: finalComments, // 👉 Trả về finalComments
            pagination: {
                total,
                page,
                limit,
                totalPages: Math.ceil(total / limit)
            }
        });
    } catch (error) {
        console.error('Lỗi getCommentsByPost:', error);
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};

/**
 *  Lấy danh sách PHẢN HỒI (Replies) của một bình luận
 */
// 👉 ĐÃ SỬA Request -> AuthRequest để lấy req.user?.id
export const getReplies = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const { commentId } = req.params; // ID của bình luận gốc
        const page = parseInt(req.query.page as string) || 1;
        const limit = parseInt(req.query.limit as string) || 10;
        const skip = (page - 1) * limit;

        const query = { parentId: commentId };

        const replies = await Comment.find(query)
            .populate('userId', 'username avatar')
            .sort({ createdAt: 1 }) // Xếp cũ nhất lên đầu
            .skip(skip)
            .limit(limit);

        const total = await Comment.countDocuments(query);

        // 👉 BỔ SUNG: Chèn Cảm xúc vào dữ liệu trả về
        const finalReplies = await attachReactionsToComments(replies, req.user?.id);

        return res.status(200).json({
            success: true,
            data: finalReplies, // 👉 Trả về finalReplies
            pagination: {
                total,
                page,
                limit,
                totalPages: Math.ceil(total / limit)
            }
        });
    } catch (error) {
        console.error('Lỗi getReplies:', error);
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};

/**
 * 3. Xóa comment
 */
export const deleteComment = async (req: AuthRequest, res: Response): Promise<Response | void> => {
    try {
        const commentId = req.params.id;
        const userId = req.user?.id;

        if (!userId) {
            return res.status(401).json({ message: 'Không tìm thấy thông tin người dùng' });
        }

        // ĐÃ SỬA: Kiểm tra quyền xóa bằng cột userId
        const deletedComment = await Comment.findOneAndDelete({
            _id: commentId,
            userId: userId 
        });

        if (!deletedComment) {
            return res.status(403).json({ message: 'Không tìm thấy bình luận hoặc bạn không có quyền xóa' });
        }

        // Tùy chọn: Xóa luôn các reply của comment này (nếu có)
        await Comment.deleteMany({ parentId: commentId });

        return res.status(200).json({
            success: true,
            message: 'Đã xóa bình luận'
        });
    } catch (error) {
        console.error('Lỗi deleteComment:', error);
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};