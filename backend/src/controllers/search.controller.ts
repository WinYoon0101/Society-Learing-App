import mongoose from 'mongoose';
import { Response } from "express";
import { AuthRequest } from '../middlewares/auth.middleware';
import Post from '../models/post.model';
import User from '../models/user.model';
import Group from '../models/group.model';
import Media from '../models/media.model';
import Comment from '../models/comment.model';
import Reaction from '../models/reaction.model';

// ====================================================
// 1. API LẤY TOP TRENDING HASHTAGS 
// ====================================================
export const getTrendingTopics = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const now = new Date();
        
        // Mốc 7 ngày trước (để tính chu kỳ hiện tại)
        const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        
        // Mốc 14 ngày trước (để tính chu kỳ quá khứ)
        const fourteenDaysAgo = new Date(now.getTime() - 14 * 24 * 60 * 60 * 1000);

        const trendingHashtags = await Post.aggregate([
            // Bước 1: Quét tất cả bài viết Public, ngoài nhóm trong vòng 14 ngày qua
            // Đã xóa điều kiện check size để $unwind tự động lọc mảng rỗng, tránh lỗi ngầm
            { 
                $match: { 
                    createdAt: { $gte: fourteenDaysAgo },
                    privacy: "Public",
                    groupId: null
                } 
            },

            // Bước 2: Tách mảng hashtags thành các dòng riêng lẻ
            { $unwind: "$hashtags" },

            // Bước 3: Phân loại bài viết thuộc chu kỳ nào (Tuần này hay Tuần trước)
            {
                $project: {
                    hashtags: 1,
                    isCurrentPeriod: {
                        $cond: [{ $gte: ["$createdAt", sevenDaysAgo] }, 1, 0]
                    },
                    isPreviousPeriod: {
                        $cond: [{ $lt: ["$createdAt", sevenDaysAgo] }, 1, 0]
                    }
                }
            },

            // Bước 4: Gom nhóm theo từng Hashtag và cộng tổng số lượt xuất hiện của cả 2 chu kỳ
            { 
                $group: { 
                    _id: "$hashtags", 
                    currentMentions: { $sum: "$isCurrentPeriod" },
                    previousMentions: { $sum: "$isPreviousPeriod" }
                } 
            },

            // Bước 5: Chỉ giữ lại các hashtag thực sự có người dùng trong tuần này
            { $match: { currentMentions: { $gt: 0 } } },

            // Bước 6: Áp dụng công thức tính toán tỷ lệ % tăng trưởng
            {
                $project: {
                    _id: 0,
                    name: "$_id",
                    mentions: "$currentMentions",
                    trendPercentage: {
                        $cond: [
                            // Nếu tuần trước không có ai dùng (previousMentions = 0)
                            { $eq: ["$previousMentions", 0] },
                            // Trả về cờ hiệu 9999 để Frontend hiện chữ "NEW"
                            9999, 
                            // Nếu tuần trước có người dùng -> Tính % tăng giảm thực tế
                            {
                                $round: [
                                    {
                                        $multiply: [
                                            {
                                                $divide: [
                                                    { $subtract: ["$currentMentions", "$previousMentions"] },
                                                    "$previousMentions"
                                                ]
                                            },
                                            100
                                        ]
                                    },
                                    1 // Làm tròn 1 chữ số thập phân
                                ]
                            }
                        ]
                    }
                }
            },

            // Bước 7: Sắp xếp theo số lượt nhắc đến trong tuần này giảm dần
            { $sort: { mentions: -1 } },

            // Bước 8: Giới hạn lấy tối đa Top 10 xu hướng hot nhất
            { $limit: 10 }
        ]);

        return res.status(200).json({ 
            success: true, 
            data: trendingHashtags 
        });

    } catch (error) {
        console.error("Lỗi lấy Trending Topics:", error);
        return res.status(500).json({ success: false, message: "Lỗi hệ thống khi tính toán bảng xu hướng" });
    }
};

// ====================================================
// 2. API TÌM KIẾM TỔNG HỢP (Bài viết, Thành viên, Nhóm)
// ====================================================
export const searchEverything = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const currentUserId = req.user?.id;
        const { q, hashtag } = req.query; 

        if (!q && !hashtag) {
            return res.status(200).json({
                success: true,
                data: { posts: [], users: [], groups: [] }
            });
        }

        let postFilter: any = { privacy: "Public", groupId: null };

        if (hashtag) {
            postFilter.hashtags = hashtag as string;
        } 
        else if (q) {
            const searchRegex = new RegExp(q as string, 'i');
            postFilter.$or = [
                { content: searchRegex },
                { hashtags: searchRegex }
            ];
        }

        const [rawPosts, users, groups] = await Promise.all([
            Post.find(postFilter)
                .sort({ createdAt: -1 })
                .limit(20)
                .populate('authorId', 'username avatar')
                .lean(),

            hashtag ? [] : User.find({ username: new RegExp(q as string, 'i') })
                .select('_id username avatar')
                .limit(5)
                .lean(),

            hashtag ? [] : Group.find({ name: new RegExp(q as string, 'i') })
                .select('_id name avatar cover member')
                .limit(5)
                .lean()
        ]);

        const postsWithDetails = await Promise.all(rawPosts.map(async (post) => {
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

        return res.status(200).json({
            success: true,
            data: {
                posts: postsWithDetails,
                users: users,
                groups: groups.map((g: any) => ({
                    _id: g._id,
                    name: g.name,
                    avatar: g.avatar,
                    cover: g.cover,
                    memberCount: g.member ? g.member.length : 0
                }))
            }
        });

    } catch (error) {
        console.error("Lỗi thực hiện tìm kiếm:", error);
        return res.status(500).json({ success: false, message: "Lỗi hệ thống khi tìm kiếm" });
    }
};