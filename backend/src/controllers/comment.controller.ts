import { Request, Response } from 'express';
import Comment from '../models/comment.model'; // Đảm bảo đường dẫn này đúng với model của bạn

// Interface nhận diện req.user từ middleware auth
interface AuthRequest extends Request {
    user?: {
        id: string;
    };
}

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
            post: postId, // Lưu ý: Đổi thành tên trường đúng trong model của bạn (vd: postId)
            author: userId, // Lưu ý: Đổi thành tên trường đúng trong model của bạn (vd: userId)
            content: content,
            parentComment: parentId || null // Dành cho tính năng reply sau này
        });

        await newComment.save();

        // Populate thông tin người dùng để trả về frontend có sẵn avatar/tên hiển thị luôn
        await newComment.populate('author', 'username avatar');

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

        // Chỉ lấy các comment gốc (không lấy reply lẫn vào)
        const query = { post: postId, parentComment: null };

        const comments = await Comment.find(query)
            .populate('author', 'username avatar')
            .sort({ createdAt: -1 }) // Mới nhất lên đầu
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

        // Tìm và xóa: Chỉ xóa nếu comment này do chính user đó tạo ra
        const deletedComment = await Comment.findOneAndDelete({
            _id: commentId,
            author: userId 
        });

        if (!deletedComment) {
            return res.status(403).json({ message: 'Không tìm thấy bình luận hoặc bạn không có quyền xóa' });
        }

        // Tùy chọn: Xóa luôn các reply của comment này (nếu có)
        // await Comment.deleteMany({ parentComment: commentId });

        return res.status(200).json({
            success: true,
            message: 'Đã xóa bình luận'
        });
    } catch (error) {
        console.error('Lỗi deleteComment:', error);
        return res.status(500).json({ message: 'Lỗi Server' });
    }
};