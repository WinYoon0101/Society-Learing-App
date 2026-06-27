
import { AuthRequest } from '../middlewares/auth.middleware'; // Import type AuthRequest của bạn
import Post from '../models/post.model';
import User from '../models/user.model';
import Comment from '../models/comment.model';
import Reaction from '../models/reaction.model';
import Media from '../models/media.model';
import Report from '../models/report.model'; 
import Notification from '../models/notification.model';
import mongoose from 'mongoose';
import { Request, Response } from "express";

// =====================================
// [DASHBOARD] LẤY THỐNG KÊ TỔNG QUAN
// Phục vụ trực tiếp cho màn hình Dashboard Android
// =====================================

export const getDashboardStats = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        // 1. Thống kê tổng quan cơ bản
        const totalUsers = await User.countDocuments();
        const startOfToday = new Date();
        startOfToday.setHours(0, 0, 0, 0);
        const newUsersToday = await User.countDocuments({ createdAt: { $gte: startOfToday } });
        
        const totalPosts = await Post.countDocuments();
        const pendingReports = await Report.countDocuments({ status: 'pending' });

        // 2. Dữ liệu cho Pie Chart (Tỷ lệ Tương tác) - Đếm thực tế từ Collection
        const totalReactions = await Reaction.countDocuments();
        const totalComments = await Comment.countDocuments();

        // 3. Dữ liệu cho Line Chart & Bar Chart 1 (Tăng trưởng User & Bài viết 7 ngày qua)
        const sevenDaysAgo = new Date();
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
        sevenDaysAgo.setHours(0, 0, 0, 0);

        // -- Nhóm user theo ngày
        const rawUserGrowth = await User.aggregate([
            { $match: { createdAt: { $gte: sevenDaysAgo } } },
            {
                $group: {
                    _id: { $dateToString: { format: "%d/%m", date: "$createdAt", timezone: "+07:00" } },
                    count: { $sum: 1 }
                }
            }
        ]);

        // -- Nhóm post theo ngày
        const rawPostGrowth = await Post.aggregate([
            { $match: { createdAt: { $gte: sevenDaysAgo } } },
            {
                $group: {
                    _id: { $dateToString: { format: "%d/%m", date: "$createdAt", timezone: "+07:00" } },
                    count: { $sum: 1 }
                }
            }
        ]);

        // -- Tạo mảng chuẩn 7 ngày (lấp đầy những ngày có 0 user/0 post)
        const growth7Days = [];
        for (let i = 6; i >= 0; i--) {
            const d = new Date();
            d.setDate(d.getDate() - i);
            const dateStr = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
            
            const foundUser = rawUserGrowth.find(item => item._id === dateStr);
            const foundPost = rawPostGrowth.find(item => item._id === dateStr);
            
            growth7Days.push({
                date: dateStr,
                newUsers: foundUser ? foundUser.count : 0, // Dùng cho Line Chart
                newPosts: foundPost ? foundPost.count : 0  // Dùng cho Bar Chart
            });
        }

        // 4. Dữ liệu cho Bar Chart 2 (Phân bổ các loại cảm xúc)
        const rawReactionTypes = await Reaction.aggregate([
            {
                $group: {
                    _id: "$type",
                    count: { $sum: 1 }
                }
            }
        ]);

        // Chuẩn hóa object để đảm bảo luôn có đủ key dù chưa ai thả cảm xúc đó
        const reactionBreakdown = {
            Like: 0, Love: 0, Haha: 0, Wow: 0, Angry: 0, Sad: 0
        };
        rawReactionTypes.forEach(item => {
            if (reactionBreakdown.hasOwnProperty(item._id)) {
                reactionBreakdown[item._id as keyof typeof reactionBreakdown] = item.count;
            }
        });

        // ==========================================
        // TRẢ VỀ DỮ LIỆU CHUẨN HOÁ CHO CLIENT
        // ==========================================
        return res.status(200).json({
            success: true,
            data: {
                // Thẻ số liệu tổng quan trên cùng
                overview: {
                    totalUsers,
                    newUsersToday,
                    totalPosts,
                    pendingReports,
                },
                
                // Dùng cho Pie Chart (Tổng quan tương tác)
                interactionsPieChart: {
                    reactions: totalReactions,
                    comments: totalComments,
                },
                
                // Dùng cho biểu đồ ngang/dọc (Chi tiết từng loại cảm xúc)
                reactionBarChart: reactionBreakdown,
                
                // Dùng cho Line Chart (Users) và Bar Chart (Posts) theo thời gian
                growth7DaysChart: growth7Days 
            }
        });
    } catch (error) {
        console.error("Lỗi lấy thống kê Dashboard:", error);
        return res.status(500).json({ success: false, message: "Lỗi hệ thống Admin" });
    }
};

// =====================================
// [REPORTS] LẤY DANH SÁCH BÁO CÁO
// =====================================
export const getAllReportsAdmin = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const reports = await Report.find()
            .sort({ status: -1, createdAt: -1 }) 
            .populate('reporterId', 'username avatar email')
            .lean();

        return res.status(200).json({ success: true, data: reports });
    } catch (error) {
        console.error("Lỗi Admin get reports:", error);
        return res.status(500).json({ success: false, message: "Lỗi server" });
    }
};

// =====================================
// [REPORTS] XỬ LÝ BÁO CÁO (Đổi trạng thái)
// =====================================
export const updateReportStatusAdmin = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const { id } = req.params;
        const { status } = req.body; 

        if (!['resolved', 'ignored', 'pending'].includes(status)) {
            return res.status(400).json({ success: false, message: "Trạng thái không hợp lệ" });
        }

        const report = await Report.findByIdAndUpdate(
            id,
            { status: status },
            { new: true }
        );

        if (!report) {
            return res.status(404).json({ success: false, message: "Không tìm thấy báo cáo" });
        }

        return res.status(200).json({ success: true, message: `Đã đổi trạng thái thành ${status}`, data: report });
    } catch (error) {
        console.error("Lỗi Admin update report:", error);
        return res.status(500).json({ success: false, message: "Lỗi server khi cập nhật báo cáo" });
    }
};

// =====================================
// [POSTS] LẤY BÀI VIẾT (CÓ PHÂN TRANG)
// =====================================
export const getAllPostsAdmin = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        // 1. Lấy page và limit từ query, mặc định là trang 1, mỗi trang 20 bài
        const page = parseInt(req.query.page as string) || 1;
        const limit = parseInt(req.query.limit as string) || 20;
        const skip = (page - 1) * limit;

        // 2. Query có thêm skip() và limit()
        const posts = await Post.find()
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(limit)
            .populate('authorId', 'username avatar email')
            .lean();

        const postsWithMedia = await Promise.all(posts.map(async (post) => {
            const mediaList = await Media.find({ targetId: post._id });
            return {
                ...post,
                mediaFiles: mediaList.map(m => m.url)
            };
        }));

        return res.status(200).json({ success: true, data: postsWithMedia });
    } catch (error) {
        console.error("Lỗi Admin get posts:", error);
        return res.status(500).json({ success: false, message: "Lỗi server" });
    }
};

// =====================================
// [POSTS] ADMIN XÓA BẤT KỲ BÀI VIẾT NÀO
// =====================================
export const deletePostByAdmin = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const { id } = req.params;

        const post = await Post.findById(id);
        if (!post) {
            return res.status(404).json({ success: false, message: "Không tìm thấy bài viết" });
        }

        await Promise.all([
            Media.deleteMany({ targetId: id }),
            Comment.deleteMany({ postId: id }),
            Reaction.deleteMany({ targetId: id }),
            Post.findByIdAndDelete(id)
        ]);

        return res.status(200).json({ success: true, message: "Admin đã xóa bài viết thành công" });
    } catch (error) {
        console.error("Lỗi Admin xóa bài:", error);
        return res.status(500).json({ success: false, message: "Lỗi server khi xóa bài" });
    }
};

// =====================================
// [USERS] LẤY DANH SÁCH NGƯỜI DÙNG
// =====================================
export const getAllUsersAdmin = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const users = await User.find()
            .select('-password') 
            .sort({ createdAt: -1 })
            .lean();

        return res.status(200).json({ success: true, data: users });
    } catch (error) {
        console.error("Lỗi Admin get users:", error);
        return res.status(500).json({ success: false, message: "Lỗi server" });
    }
};

// =====================================
// [USERS] KHÓA / MỞ KHÓA TÀI KHOẢN (BAN / UNBAN)
// =====================================
export const toggleUserStatusAdmin = async (req: AuthRequest, res: Response): Promise<any> => {
    try {
        const { id } = req.params;
        const user = await User.findById(id);
        
        if (!user) {
            return res.status(404).json({ success: false, message: "Không tìm thấy người dùng" });
        }

        // Đảo ngược trạng thái hiện tại (Nếu đang true thì thành false và ngược lại)
        user.isActive = !user.isActive;
        await user.save();

        const statusText = user.isActive ? "Mở khóa" : "Khóa";
        return res.status(200).json({ 
            success: true, 
            message: `Đã ${statusText} tài khoản thành công`, 
            data: user 
        });
    } catch (error) {
        console.error("Lỗi Admin toggle user status:", error);
        return res.status(500).json({ success: false, message: "Lỗi server" });
    }
};

// =====================================
// [NOTIFICATIONS] GỬI THÔNG BÁO HỆ THỐNG
// =====================================
export const sendSystemNotification = async (req: Request, res: Response): Promise<any> => {
    try {
        const { content, userIds, type = 'system_notice' } = req.body;

        if (!content) {
            return res.status(400).json({ success: false, message: "Nội dung thông báo không được để trống" });
        }

        const SYSTEM_SENDER_ID = new mongoose.Types.ObjectId('000000000000000000000000');
        const SYSTEM_TARGET_ID = new mongoose.Types.ObjectId('111111111111111111111111');

        let targetUserIds: mongoose.Types.ObjectId[] = [];

        // 1. Lọc danh sách người nhận
        if (userIds && Array.isArray(userIds) && userIds.length > 0) {
            targetUserIds = userIds; // Gửi cho nhóm cụ thể
        } else {
            // Lấy TẤT CẢ user trong hệ thống
            const allUsers = await User.find({}).select('_id').lean();
            targetUserIds = allUsers.map(u => u._id as mongoose.Types.ObjectId);
        }

        if (targetUserIds.length === 0) {
            return res.status(404).json({ success: false, message: "Không tìm thấy người dùng nào để nhận thông báo" });
        }

        // 2. Chuẩn bị mảng dữ liệu
        const notificationsToInsert = targetUserIds.map(userId => ({
            recipient: userId,
            sender: SYSTEM_SENDER_ID, 
            type: type, 
            targetId: SYSTEM_TARGET_ID, 
            content: content,
            isRead: false
        }));

        // 3. Lưu vào database
        await Notification.insertMany(notificationsToInsert);

        return res.status(200).json({
            success: true,
            message: `Đã gửi thông báo hệ thống thành công tới ${targetUserIds.length} người dùng!`,
            data: {
                totalSent: targetUserIds.length,
                type: type
            }
        });

    } catch (error) {
        console.error("Lỗi Admin gửi thông báo hệ thống:", error);
        return res.status(500).json({ success: false, message: "Lỗi server khi gửi thông báo" });
    }
};