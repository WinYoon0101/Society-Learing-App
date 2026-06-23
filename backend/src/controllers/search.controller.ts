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
        // Chỉ quét các bài viết được tạo trong vòng 7 ngày qua
        const sevenDaysAgo = new Date();
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

        const trendingHashtags = await Post.aggregate([
            // Bước 1: Lọc bài viết Public, nằm ngoài nhóm công khai và có chứa hashtag trong 7 ngày qua
            { 
                $match: { 
                    createdAt: { $gte: sevenDaysAgo },
                    privacy: "Public",
                    groupId: null,
                    hashtags: { $exists: true, $not: { $size: 0 } }
                } 
            },

            // Bước 2: Tách mảng hashtags thành từng dòng riêng biệt để đếm
            { $unwind: "$hashtags" },

            // Bước 3: Gom nhóm theo tên hashtag và đếm số lần xuất hiện
            { 
                $group: { 
                    _id: "$hashtags", 
                    mentions: { $sum: 1 } 
                } 
            },

            // Bước 4: Sắp xếp theo số lượng mentions giảm dần
            { $sort: { mentions: -1 } },

            // Bước 5: Giới hạn lấy Top 10 xu hướng hot nhất
            { $limit: 10 },

            // Bước 6: Định dạng lại dữ liệu trả về cho Frontend dễ dùng
            {
                $project: {
                    _id: 0,
                    name: "$_id",
                    mentions: 1
                }
            }
        ]);

        return res.status(200).json({ 
            success: true, 
            data: trendingHashtags 
        });

    } catch (error) {
        console.error("Lỗi lấy Trending Topics:", error);
        return res.status(500).json({ success: false, message: "Lỗi hệ thống khi lấy bảng xu hướng" });
    }
};

// ====================================================
// 2. API TÌM KIẾM TỔNG HỢP (Bài viết, Thành viên, Nhóm)
// ====================================================
export const searchEverything = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const currentUserId = req.user?.id;
        const { q, hashtag } = req.query; // q: từ khóa thường, hashtag: từ khóa gắn thẻ 

        // Nếu không truyền từ khóa tìm kiếm nào thì trả về mảng rỗng
        if (!q && !hashtag) {
            return res.status(200).json({
                success: true,
                data: { posts: [], users: [], groups: [] }
            });
        }

        let postFilter: any = { privacy: "Public", groupId: null };

        // Trường hợp 1: Nếu người dùng click vào một Hashtag cụ thể từ bảng Trending
        if (hashtag) {
            postFilter.hashtags = hashtag as string;
        } 
        // Trường hợp 2: Người dùng gõ từ khóa tự do vào ô Tìm kiếm
        else if (q) {
            const searchRegex = new RegExp(q as string, 'i'); // Không phân biệt hoa thường
            postFilter.$or = [
                { content: searchRegex },
                { hashtags: searchRegex }
            ];
        }

        // --- TIẾN HÀNH QUERY SONG SONG ---
        const [rawPosts, users, groups] = await Promise.all([
            // Tìm bài viết thỏa mãn bộ lọc (Chỉ bài Public, ngoài Group)
            Post.find(postFilter)
                .sort({ createdAt: -1 })
                .limit(20)
                .populate('authorId', 'username avatar')
                .lean(),

            // Tìm kiếm người dùng theo username (Chỉ chạy khi tìm kiếm bằng từ khóa 'q')
            hashtag ? [] : User.find({ username: new RegExp(q as string, 'i') })
                .select('_id username avatar')
                .limit(5)
                .lean(),

            // Tìm kiếm nhóm theo tên nhóm (Chỉ chạy khi tìm kiếm bằng từ khóa 'q')
            hashtag ? [] : Group.find({ name: new RegExp(q as string, 'i') })
                .select('_id name avatar cover member')
                .limit(5)
                .lean()
        ]);

        // --- ĐỔI DỮ LIỆU BÀI VIẾT ĐỂ LẤY CHI TIẾT (Ảnh, Lượt Tim, Bình luận) ---
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

        // Trả kết quả phân loại rõ ràng về cho Frontend
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