import mongoose from 'mongoose';
import { Response } from "express";
import { AuthRequest } from '../middlewares/auth.middleware';
import Post from '../models/post.model';
import Media from '../models/media.model';
import Group from '../models/group.model';
import Comment from '../models/comment.model'; 
import Reaction from '../models/reaction.model'; 
import User from '../models/user.model';
import Friend from '../models/friend.model';

// =====================================
// API ĐĂNG BÀI
// =====================================
export const createPost = async (req: AuthRequest, res: Response) => {
    try {
        const { content, privacy, groupId, tags, initialReaction } = req.body;
        const authorId = req.user?.id;

        // Nếu đăng vào nhóm, kiểm tra user có phải thành viên không
        if (groupId) {
            const group = await Group.findById(groupId).lean();
            if (!group) {
                return res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            }
            const isMember = group.member.some((m) => m.userId.toString() === authorId);
            if (!isMember) {
                return res.status(403).json({ success: false, message: "Bạn không phải thành viên của nhóm này" });
            }
        }

        const newPost = new Post({
            authorId: authorId,
            groupId: groupId || null,
            content: content,
            privacy: privacy || "Public",
            tags: tags ? (Array.isArray(tags) ? tags : JSON.parse(tags)) : []
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

        if (initialReaction) {
            try {
                const react = new Reaction({
                    userId: authorId,
                    targetId: savePost._id,
                    targetType: 'Post',
                    type: initialReaction
                });
                await react.save();
            } catch (e) {
                console.error('Không thể lưu reaction ban đầu:', e);
            }
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
// API LẤY BÀI VIẾT TRANG HOME (FEED) - ĐÃ SỬA GỘP LOGIC
// =====================================
export const getFeed = async (req: AuthRequest, res: Response) => {
    try {
        const currentUserId = req.user?.id;

        // 1. Lấy danh sách ID các nhóm mà User này đã tham gia làm thành viên
        // Tìm cả trường hợp 'member.userId' và 'members.userId' để tránh lệch data cũ
        const userGroups = await Group.find({
            $or: [
                { 'member.userId': currentUserId },
                { 'members.userId': currentUserId }
            ]
        }).select('_id').lean();

        const groupIds = userGroups.map(g => g._id);

        // 2. Lấy danh sách ID bạn bè đã kết bạn thành công
        const friendships = await Friend.find({
            $or: [
                { requester: currentUserId, status: "accepted" },
                { recipient: currentUserId, status: "accepted" },
            ],
        }).lean();

        const friendIds = friendships.map(f =>
            f.requester.toString() === currentUserId ? f.recipient : f.requester
        );

        // Gom ID của mình và bạn bè lại
        const allowedAuthorIds = [new mongoose.Types.ObjectId(currentUserId!), ...friendIds];

        // 3. Tìm bài viết thỏa mãn 1 trong các điều kiện sau:
        // - Hoặc là bài viết có chế độ "Public" ở bên ngoài (không nằm trong nhóm)
        // - Hoặc là bài viết nằm trong danh sách nhóm mà user đã tham gia (bất kể privacy gì)
        // - Hoặc là bài viết của chính mình/bạn bè (kể cả bài viết để chế độ Friends) ngoài nhóm
        const posts = await Post.find({
            $or: [
                { privacy: "Public", groupId: null },
                { groupId: { $in: groupIds } },
                { authorId: { $in: allowedAuthorIds }, groupId: null }
            ]
        })
            .sort({ createdAt: -1 })
            .populate('authorId', 'username avatar')
            .lean();

        // 4. Map nạp thêm dữ liệu tương tác chi tiết (Ảnh, Reaction, Comment)
        const postsWithDetails = await Promise.all(posts.map(async (post) => {
            const postIdObj = new mongoose.Types.ObjectId(post._id.toString());

            const mediaList = await Media.find({ targetId: post._id, fileType: 'image' });
            const imageUrls = mediaList.map(media => media.url);

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
                images: imageUrls,
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
// API XÓA BÀI VIẾT
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

export const toggleSavePost = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const userId = req.user?.id;
        const postId = req.params.id;

        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ message: "Không tìm thấy User" });

        const isSaved = user.savedPosts.includes(postId as any);

        if (isSaved) {
            await User.findByIdAndUpdate(userId, { $pull: { savedPosts: postId } });
            return res.status(200).json({ message: "Đã bỏ lưu bài viết" });
        } else {
            await User.findByIdAndUpdate(userId, { $addToSet: { savedPosts: postId } });
            return res.status(200).json({ message: "Đã lưu bài viết thành công" });
        }
    } catch (error) {
        console.error(error);
        return res.status(500).json({ message: "Lỗi Server" });
    }
};

export const getSavedPosts = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const userId = req.user?.id;
        if (!userId) return res.status(401).json({ message: "Vui lòng đăng nhập" });

        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ message: "Không tìm thấy User" });

        const posts = await Post.find({ _id: { $in: user.savedPosts } })
            .populate('authorId', 'username avatar')
            .populate('mediaFiles')
            .sort({ createdAt: -1 });

        const formattedPosts = posts.map((post: any) => {
            const postObj = post.toJSON({ virtuals: true });
            if (postObj.mediaFiles && postObj.mediaFiles.length > 0) {
                postObj.images = postObj.mediaFiles.map((media: any) => media.url);
            } else {
                postObj.images = [];
            }
            delete postObj.mediaFiles;
            return postObj;
        });

        return res.status(200).json({ data: formattedPosts });
    } catch (error) {
        console.error(error);
        return res.status(500).json({ message: "Lỗi Server" });
    }
};
// =====================================
// API LẤY BÀI VIẾT CỦA TÔI
// =====================================
export const getMyPosts = async (req: AuthRequest, res: Response) => {
    try {
        const userId = req.user?.id;
        const posts = await Post.find({ authorId: userId })
            .sort({ createdAt: -1 })
            .populate('authorId', 'username avatar')
            .lean();

        const postsWithDetails = await Promise.all(posts.map(async (post) => {
            const mediaList = await Media.find({ targetId: post._id, fileType: 'image' });
            const imageUrls = mediaList.map(m => m.url);
            const countComment = await Comment.countDocuments({ postId: post._id });
            const countReaction = await Reaction.countDocuments({ targetId: post._id });
            const myReactDoc = await Reaction.findOne({ targetId: post._id, userId });
            const topReactDocs = await Reaction.aggregate([
                { $match: { targetId: new mongoose.Types.ObjectId(post._id.toString()) } },
                { $group: { _id: "$type", count: { $sum: 1 } } },
                { $sort: { count: -1 } }, { $limit: 2 }
            ]);
            return {
                ...post,
                images: imageUrls,
                countComment,
                countReaction,
                myReaction: myReactDoc?.type ?? null,
                topReactions: topReactDocs.map(d => d._id),
            };
        }));

        res.status(200).json({ success: true, data: postsWithDetails });
    } catch (error) {
        console.error("Lỗi getMyPosts", error);
        res.status(500).json({ success: false, message: "Lỗi server" });
    }
};

// =====================================
// API LẤY BÀI VIẾT CỦA USER KHÁC
// =====================================
export const getPostsByUser = async (req: AuthRequest, res: Response) => {
    try {
        const { userId } = req.params;
        const currentUserId = req.user?.id;
        const posts = await Post.find({ authorId: userId, privacy: { $in: ["Public", "Friends"] } })
            .sort({ createdAt: -1 })
            .populate('authorId', 'username avatar')
            .lean();

        const postsWithDetails = await Promise.all(posts.map(async (post) => {
            const mediaList = await Media.find({ targetId: post._id, fileType: 'image' });
            const imageUrls = mediaList.map(m => m.url);
            const countComment = await Comment.countDocuments({ postId: post._id });
            const countReaction = await Reaction.countDocuments({ targetId: post._id });
            const myReactDoc = await Reaction.findOne({ targetId: post._id, userId: currentUserId });
            const topReactDocs = await Reaction.aggregate([
                { $match: { targetId: new mongoose.Types.ObjectId(post._id.toString()) } },
                { $group: { _id: "$type", count: { $sum: 1 } } },
                { $sort: { count: -1 } }, { $limit: 2 }
            ]);
            return {
                ...post,
                images: imageUrls,
                countComment,
                countReaction,
                myReaction: myReactDoc?.type ?? null,
                topReactions: topReactDocs.map(d => d._id),
            };
        }));

        res.status(200).json({ success: true, data: postsWithDetails });
    } catch (error) {
        console.error("Lỗi getPostsByUser", error);
        res.status(500).json({ success: false, message: "Lỗi server" });
    }
};
