import { Response } from "express";
import { AuthRequest } from "../middlewares/auth.middleware";
import Comment from "../models/comment.model";
import Post from "../models/post.model";
import Notification from "../models/notification.model";

// API gửi binh luận
export const createComment = async (req: AuthRequest, res: Response) => {
    try {
      
        const { postId, content, parentId } = req.body; 
        const userId = req.user?.id;

        // 1. Kiểm tra bài viết tồn tại
        const post = await Post.findById(postId);
        if (!post) {
            return res.status(404).json({ success: false, message: "Bài viết không tồn tại" });
        }

        // XÁC ĐỊNH NGƯỜI NHẬN THÔNG BÁO ---
        let receiverId = post.authorId.toString(); // Mặc định báo cho chủ bài viết
        let targetId = postId;                     // Mặc định đích đến là Bài viết
        let targetType = "Post";

        if (parentId) {
            const parentComment = await Comment.findById(parentId);
            if (!parentComment) {
                return res.status(404).json({ success: false, message: "Bình luận bạn muốn trả lời không tồn tại" });
            }
    
            receiverId = parentComment.userId.toString();
            targetId = parentId; 
            targetType = "Comment";
        }

        // 2. Lưu bình luận vào Database
        const newComment = new Comment({
            postId: postId,
            userId: userId,
            content: content,
            parentId: parentId || null // Nếu không có thì lưu là null
        });
        await newComment.save();
        await newComment.populate("userId", "username avatarUrl");

        // TẠO THÔNG BÁO 
        if (userId !== receiverId) { 
            const newNotification = new Notification({
                receiverId: receiverId, 
                senderId: userId,        
                targetId: targetId,        
                targetType: targetType,      
                type: "COMMENT",
            });
            newNotification.save().catch(err => console.error("Lỗi thông báo:", err));
        }

        res.status(201).json({ success: true, data: newComment });

    } catch (error) {
        console.error("Lỗi tạo bình luận:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// API trả lời bình luận
export const getCommentsByPost = async (req: AuthRequest, res: Response) => {
    try {
        const postId = req.params.postId;

        // Lấy toàn bộ bình luận của bài viết. 
        const comments = await Comment.find({ postId: postId })
            .sort({ createdAt: 1 }) 
            .populate("userId", "username avatarUrl")
            .lean(); 

        // --- THUẬT TOÁN XẾP CÂY (TREE BUILDER) ---
        const commentMap: any = {};
        const rootComments: any[] = [];

        // Bước 1: Khởi tạo mảng 'replies' rỗng cho tất cả mọi người và đưa vào Map
        comments.forEach(comment => {
            comment.replies = [];
            commentMap[comment._id.toString()] = comment;
        });

        // Bước 2: Duyệt lại lần nữa để nhận cha - con
        comments.forEach(comment => {
            if (comment.parentId) {
                const parentString = comment.parentId.toString();
                if (commentMap[parentString]) {
                    commentMap[parentString].replies.push(comment);
                }
            } else {
                rootComments.push(comment);
            }
        });

        // Chỉ cần trả về (root) là các cmt bên trong tự động đi theo
        res.status(200).json({
            success: true,
            data: rootComments 
        });
    } catch (error) {
        console.error("Lỗi lấy bình luận:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// API xóa cmt

export const deleteComment = async (req: AuthRequest, res: Response) => {
    try {
        const { commentId } = req.params;
        const userId = req.user?.id;
        // Tìm bình luận
        const comment = await Comment.findById(commentId);
        if (!comment) {
            return res.status(404).json({ success: false, message: "Không tìm thấy bình luận" });
        }
        // Chỉ người Cmt mới được xóa 
        if (comment.userId.toString() !== userId) {
            return res.status(403).json({ success: false, message: "Bạn không có quyền xóa!" });
        }
        await comment.deleteOne(); 
        await Notification.deleteOne({ targetId: commentId, type: "COMMENT" });

        res.status(200).json({ success: true, message: "Xóa thành công" });
    } catch (error) {
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};