import { Response } from "express";
import { AuthRequest } from "../middlewares/auth.middleware";
import Comment from "../models/comment.model";
import Post from "../models/post.model";
import Notification from "../models/notification.model";

// API gửi bình luận
export const createComment = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const { postId, content, parentId } = req.body; 
        const userId = req.user?.id;

        if (!userId) {
            return res.status(401).json({ success: false, message: "Vui lòng đăng nhập" });
        }

        const post = await Post.findById(postId);
        if (!post) {
            return res.status(404).json({ success: false, message: "Bài viết không tồn tại" });
        }

        // XÁC ĐỊNH NGƯỜI NHẬN ---
        let recipientId = post.authorId.toString(); 
        let targetId = postId;
        
        if (parentId) {
            const parentComment = await Comment.findById(parentId);
            if (parentComment) {
                recipientId = parentComment.userId.toString();
                targetId = parentId; 
            }
        }

        // 2. Lưu bình luận
        const newComment = new Comment({
            postId: postId,
            userId: userId,
            content: content,
            parentId: parentId || null 
        });
        await newComment.save();
        await newComment.populate("userId", "username avatar avatarUrl");

        // 3. TẠO THÔNG BÁO (Chuẩn theo Model mới của bạn)
        if (userId !== recipientId) { 
            await Notification.create({
                recipient: recipientId, // Khớp với model
                sender: userId,         // Khớp với model
                type: "post_comment",   // Khớp với kiểu bạn đã định nghĩa
                targetId: targetId,     
                content: "đã bình luận về bài viết của bạn", // BẮT BUỘC CÓ
                isRead: false
            }).catch(err => console.error("Lỗi lưu thông báo:", err));
        }

        return res.status(201).json({ success: true, data: newComment });

    } catch (error) {
        console.error("Lỗi tạo bình luận:", error);
        return res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// API lấy bình luận (Xếp cây)
export const getCommentsByPost = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const postId = req.params.postId;
        const comments = await Comment.find({ postId: postId })
            .sort({ createdAt: 1 }) 
            .populate("userId", "username avatar avatarUrl")
            .lean(); 

        const commentMap: any = {};
        const rootComments: any[] = [];

        comments.forEach((comment: any) => {
            comment.replies = [];
            commentMap[comment._id.toString()] = comment;
        });

        comments.forEach((comment: any) => {
            if (comment.parentId) {
                const parentString = comment.parentId.toString();
                if (commentMap[parentString]) {
                    commentMap[parentString].replies.push(comment);
                }
            } else {
                rootComments.push(comment);
            }
        });

        return res.status(200).json({ success: true, data: rootComments });
    } catch (error) {
        console.error("Lỗi lấy bình luận:", error);
        return res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// API xóa bình luận
export const deleteComment = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const { commentId } = req.params;
        const userId = req.user?.id;

        const comment = await Comment.findById(commentId);
        if (!comment || comment.userId.toString() !== userId) {
            return res.status(404).json({ success: false, message: "Không tìm thấy hoặc không có quyền" });
        }

        // Xóa thông báo liên quan
        await Notification.deleteOne({ 
            sender: userId, 
            targetId: comment.parentId || comment.postId, 
            type: "post_comment" 
        });

        await comment.deleteOne(); 
        return res.status(200).json({ success: true, message: "Xóa thành công" });
    } catch (error) {
        return res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};