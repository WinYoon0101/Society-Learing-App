import { Request, Response } from 'express';
import Comment from '../models/comment.model'; 
import Post from '../models/post.model';
import User from '../models/user.model';
import Notification from '../models/notification.model';

interface AuthRequest extends Request {
    user?: {
        id: string;
    };
}

/**
 * 1. Viết comment mới
 */
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

        // === TẠO THÔNG BÁO BÌNH LUẬN ===
        try {
            const post = await Post.findById(postId);
            
            // Lấy ID tác giả bài viết
            const authorOfPost = post?.authorId || (post as any)?.author;
            
            // Kẻ vạch an toàn: Không tự thông báo khi mình tự comment bài của mình
            if (post && authorOfPost && authorOfPost.toString() !== userId.toString()) {
                
                // Lấy thông tin người gửi để gắn tên vào thông báo
                const senderInfo = await User.findById(userId).select('username');
                const senderName = senderInfo?.username || 'Ai đó';

                await Notification.create({
                    recipient: authorOfPost, 
                    sender: userId,
                    type: 'post_comment', 
                    targetId: postId, 
                    content: `${senderName} đã bình luận về bài viết của bạn`
                });
            }
        } catch (notifError: any) {
            console.error("Lỗi Mongoose khi lưu thông báo comment:", notifError.message);
        }
        // =============================

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
export const getCommentsByPost = async (req: Request, res: Response): Promise<Response | void> => {
    try {
        const { postId } = req.params;
        const page = parseInt(req.query.page as string) || 1;
        const limit = parseInt(req.query.limit as string) || 10;
        const skip = (page - 1) * limit;

        // ĐÃ SỬA: Lọc bằng postId và parentId
        const query = { postId: postId, parentId: null };

        const comments = await Comment.find(query)
            .populate('userId', 'username avatar') // ĐÃ SỬA: Populate userId
            .sort({ createdAt: -1 }) 
            .skip(skip)
            .limit(limit);

        const total = await Comment.countDocuments(query);

        return res.status(200).json({
            success: true,
            data: comments,
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
        // await Comment.deleteMany({ parentId: commentId });

        return res.status(200).json({
            success: true,
            message: 'Đã xóa bình luận'
        });
    } catch (error) {
        console.error('Lỗi deleteComment:', error);
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};