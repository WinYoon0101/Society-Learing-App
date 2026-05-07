import mongoose from 'mongoose';
import { Response } from "express";
import { AuthRequest } from '../middlewares/auth.middleware';
import Post from '../models/post.model';
import Media from '../models/media.model';
import Group from '../models/group.model';
import Comment from '../models/comment.model'; 
import Reaction from '../models/reaction.model'; 

// =====================================
// API ĐĂNG BÀI (GIỮ NGUYÊN BẢN GỐC)
// =====================================
export const createPost = async (req: AuthRequest, res: Response) => {
    try {
        const { content, privacy, groupId } = req.body;
        const authorId = req.user?.id;
        
        const newPost = new Post({
            authorId: authorId,
            groupId: groupId || null,
            content: content,
            privacy: privacy || "Public",
        });
        
        const savePost = await newPost.save();
        
        if (req.files && Array.isArray(req.files) && req.files.length > 0) {   
            const mediaDocument = req.files.map((file: any) => {
                const isVideo = file.mimetype.includes('video');
                return {
                    userId: authorId,
                    url: file.path,
                    fileType: isVideo ? 'video' : 'image',
                    sourceType: 'post',
                    targetId: savePost._id
                };
            });
            await Media.insertMany(mediaDocument);
        }

        res.status(201).json({
            success: true, 
            message: "Đăng bài thành công",
            PostId: savePost._id,
        });
        
    } catch(error) {
        console.error("Lỗi đăng bài", error);
        res.status(500).json({
            success: false,
            message: "Lỗi hệ thống khi đăng bài"
        });
    }
};

// =====================================
// API LẤY BẢNG TIN (ĐÃ CẬP NHẬT TRẢ VỀ MẢNG ẢNH)
// =====================================
export const getFeed = async (req: AuthRequest, res: Response) => {
    try {
        const currentUserId = req.user?.id;
        const posts = await Post.find().sort({ createdAt: -1 }).populate('authorId', 'username avatar').lean(); 

        const postsWithDetails = await Promise.all(posts.map(async (post) => {
            const postIdObj = new mongoose.Types.ObjectId(post._id.toString());

            // ĐÃ SỬA: Lấy TẤT CẢ ảnh thuộc bài viết này thay vì 1 ảnh
            const mediaList = await Media.find({ targetId: post._id, fileType: 'image' });
            const imageUrls = mediaList.map(media => media.url); // Trích xuất mảng các đường link

            const commentCount = await Comment.countDocuments({ postId: post._id });
            const countReaction = await Reaction.countDocuments({ targetId: postIdObj });

            let myReaction = null;
            if (currentUserId) {
                const myReactDoc = await Reaction.findOne({ targetId: postIdObj, userId: currentUserId });
                if (myReactDoc) {
                    myReaction = myReactDoc.type;
                }
            }

            const topReactDocs = await Reaction.aggregate([
                { $match: { targetId: postIdObj } },
                { $group: { _id: "$type", count: { $sum: 1 } } },
                { $sort: { count: -1 } },
                { $limit: 2 }
            ]);
            const topReactions = topReactDocs.map(doc => doc._id);

            return {
                ...post,
                images: imageUrls, // ĐÃ SỬA: Trả về mảng "images"
                countComment: commentCount,
                countReaction: countReaction,
                myReaction: myReaction,
                topReactions: topReactions
            };
        }));

        res.status(200).json({ success: true, data: postsWithDetails });
    } catch (error) {
        console.error("Lỗi lấy feed", error);
        res.status(500).json({ success: false, message: "Lỗi lấy feed" });
    }
};

// =====================================
// API XÓA BÀI VIẾT (GIỮ NGUYÊN BẢN GỐC)
// =====================================
export const deletePost = async (req: AuthRequest, res: Response) => {
    try {
        const postId = req.params.id; 
        const userId = req.user?.id;

        const post = await Post.findById(postId);
        
        if (!post) {
            return res.status(404).json({ success: false, message: "Không tìm thấy bài viết" }); 
        }
        
        let hasPermission = false;
        if (post.authorId.toString() === userId) {
            hasPermission = true; 
        }       
        if (!hasPermission && post.groupId) {
            const group = await Group.findById(post.groupId);
            if (group && group.creatorId.toString() === userId) {
                hasPermission = true;
            }
        }

        if (!hasPermission) {
            return res.status(403).json({ success: false, message: "Bạn không có quyền xóa bài này!" });
        }

        await Media.deleteMany({ targetId: postId });
        await Post.findByIdAndDelete(postId);
        await Comment.deleteMany({ postId: postId });
        await Reaction.deleteMany({ targetId: postId });

        res.status(200).json({ success: true, message: "Đã xóa bài viết!" });
    } catch (error) {
        res.status(500).json({ success: false, message: "Lỗi hệ thống khi xóa bài" });
    }
};