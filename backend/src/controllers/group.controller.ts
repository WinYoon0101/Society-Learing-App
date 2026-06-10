import mongoose from "mongoose";
import { Response } from "express";
import { AuthRequest } from "../middlewares/auth.middleware";
import Group from "../models/group.model";
import Post from "../models/post.model";
import Media from "../models/media.model";
import Comment from "../models/comment.model";
import Reaction from "../models/reaction.model";
import GroupInvitation from "../models/groupInvitation.model";

// =====================================
// TAB 1: NHÓM CỦA BẠN
// GET /api/groups/my
// =====================================
export const getMyGroups = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = new mongoose.Types.ObjectId(req.user!.id);
        const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);

        // Lấy tất cả group mà user là thành viên
        const groups = await Group.find({ "member.userId": userId }).lean();

        const groupsWithStats = await Promise.all(
            groups.map(async (group) => {
                // Đếm bài post mới trong 7 ngày gần nhất
                const newPostCount = await Post.countDocuments({
                    groupId: group._id,
                    createdAt: { $gte: sevenDaysAgo },
                });

                // Lấy thời điểm bài post mới nhất
                const latestPost = await Post.findOne({ groupId: group._id })
                    .sort({ createdAt: -1 })
                    .select("createdAt")
                    .lean();

                return {
                    _id: group._id,
                    groupName: group.groupName,
                    avatarUrl: group.avatarUrl,
                    privacy: group.privacy,
                    newPostCount,
                    lastUpdated: latestPost?.createdAt ?? group.updatedAt,
                };
            })
        );

        res.status(200).json({ success: true, data: groupsWithStats });
    } catch (error) {
        console.error("getMyGroups error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// TAB 2: BÀI VIẾT
// GET /api/groups/posts?page=1&limit=10
// =====================================
export const getGroupPosts = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = new mongoose.Types.ObjectId(req.user!.id);
        const page = Math.max(1, parseInt(req.query.page as string) || 1);
        const limit = Math.max(1, parseInt(req.query.limit as string) || 10);
        const skip = (page - 1) * limit;

        // Lấy danh sách groupId mà user là thành viên
        const myGroups = await Group.find({ "member.userId": userId }).select("_id").lean();
        const groupIds = myGroups.map((g) => g._id);

        const total = await Post.countDocuments({ groupId: { $in: groupIds } });

        const posts = await Post.find({ groupId: { $in: groupIds } })
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(limit)
            .populate("authorId", "username avatar")
            .populate("groupId", "groupName avatarUrl privacy")
            .lean();

        // Bổ sung media, reaction, comment cho từng post
        const postsWithDetails = await Promise.all(
            posts.map(async (post) => {
                const postIdObj = new mongoose.Types.ObjectId(post._id.toString());

                const [mediaList, countComment, countReaction, myReactDoc, topReactDocs] =
                    await Promise.all([
                        Media.find({ targetId: post._id, fileType: "image" }).lean(),
                        Comment.countDocuments({ postId: post._id }),
                        Reaction.countDocuments({ targetId: postIdObj }),
                        Reaction.findOne({ targetId: postIdObj, userId: userId }).lean(),
                        Reaction.aggregate([
                            { $match: { targetId: postIdObj } },
                            { $group: { _id: "$type", count: { $sum: 1 } } },
                            { $sort: { count: -1 } },
                            { $limit: 2 },
                        ]),
                    ]);

                return {
                    ...post,
                    images: mediaList.map((m) => m.url),
                    countComment,
                    countReaction,
                    countShare: post.countShare ?? 0,
                    myReaction: myReactDoc?.type ?? null,
                    topReactions: topReactDocs.map((d) => d._id),
                };
            })
        );

        res.status(200).json({
            success: true,
            data: postsWithDetails,
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.ceil(total / limit),
            },
        });
    } catch (error) {
        console.error("getGroupPosts error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// TAB 3: KHÁM PHÁ
// GET /api/groups/discover?search=keyword&page=1&limit=10
// =====================================
export const discoverGroups = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = new mongoose.Types.ObjectId(req.user!.id);
        const page = Math.max(1, parseInt(req.query.page as string) || 1);
        const limit = Math.max(1, parseInt(req.query.limit as string) || 10);
        const skip = (page - 1) * limit;
        const search = req.query.search as string | undefined;

        // Chỉ lấy group Public mà user chưa là thành viên
        const filter: Record<string, unknown> = {
            privacy: "Public",
            "member.userId": { $ne: userId },
        };

        if (search && search.trim()) {
            filter.groupName = { $regex: search.trim(), $options: "i" };
        }

        const [groups, total] = await Promise.all([
            Group.find(filter).skip(skip).limit(limit).lean(),
            Group.countDocuments(filter),
        ]);

        const result = groups.map((group) => ({
            _id: group._id,
            groupName: group.groupName,
            avatarUrl: group.avatarUrl,
            description: group.description,
            memberCount: group.member.length,
            privacy: group.privacy,
        }));

        res.status(200).json({
            success: true,
            data: result,
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.ceil(total / limit),
            },
        });
    } catch (error) {
        console.error("discoverGroups error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// TAB 4: LỜI MỜI
// GET /api/groups/invitations
// =====================================
export const getMyInvitations = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;

        const invitations = await GroupInvitation.find({
            inviteeId: userId,
            status: "pending",
        })
            .populate("groupId", "groupName avatarUrl")
            .populate("inviterId", "username avatar")
            .sort({ createdAt: -1 })
            .lean();

        const result = invitations.map((inv) => ({
            _id: inv._id,
            group: inv.groupId,
            inviter: inv.inviterId,
            createdAt: inv.createdAt,
        }));

        res.status(200).json({ success: true, data: result });
    } catch (error) {
        console.error("getMyInvitations error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// CHẤP NHẬN / TỪ CHỐI LỜI MỜI
// PATCH /api/groups/invitations/:invitationId
// Body: { action: "accept" | "decline" }
// =====================================
export const respondToInvitation = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { invitationId } = req.params;
        const { action } = req.body;

        if (!["accept", "decline"].includes(action)) {
            res.status(400).json({ success: false, message: "action phải là 'accept' hoặc 'decline'" });
            return;
        }

        const invitation = await GroupInvitation.findById(invitationId);

        if (!invitation) {
            res.status(404).json({ success: false, message: "Không tìm thấy lời mời" });
            return;
        }

        // Chỉ người được mời mới được phản hồi
        if (invitation.inviteeId.toString() !== userId) {
            res.status(403).json({ success: false, message: "Bạn không có quyền thực hiện hành động này" });
            return;
        }

        if (invitation.status !== "pending") {
            res.status(400).json({ success: false, message: "Lời mời này đã được xử lý" });
            return;
        }

        if (action === "accept") {
            // Thêm user vào group
            await Promise.all([
                GroupInvitation.findByIdAndUpdate(invitationId, { status: "accepted" }),
                Group.findByIdAndUpdate(invitation.groupId, {
                    $push: {
                        member: {
                            userId: new mongoose.Types.ObjectId(userId),
                            role: "member",
                            joinAt: new Date(),
                        },
                    },
                }),
            ]);
            res.status(200).json({ success: true, message: "Đã tham gia nhóm thành công" });
        } else {
            await GroupInvitation.findByIdAndUpdate(invitationId, { status: "declined" });
            res.status(200).json({ success: true, message: "Đã từ chối lời mời" });
        }
    } catch (error) {
        console.error("respondToInvitation error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// MỜI NGƯỜI VÀO NHÓM
// POST /api/groups/invitations
// Body: { groupId, inviteeId }
// =====================================
export const sendInvitation = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const inviterId = req.user!.id;
        const { groupId, inviteeId } = req.body;

        if (!groupId || !inviteeId) {
            res.status(400).json({ success: false, message: "Thiếu groupId hoặc inviteeId" });
            return;
        }

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        // Kiểm tra quyền mời: Private chỉ admin, Public thì member cũng được
        const inviterMember = group.member.find((m) => m.userId.toString() === inviterId);
        if (!inviterMember) {
            res.status(403).json({ success: false, message: "Bạn không phải thành viên của nhóm này" });
            return;
        }
        if (group.privacy === ("Private" as string) && inviterMember.role !== "admin") {
            res.status(403).json({ success: false, message: "Chỉ admin mới được mời vào nhóm riêng tư" });
            return;
        }

        // Kiểm tra invitee chưa là thành viên
        const alreadyMember = group.member.some((m) => m.userId.toString() === inviteeId);
        if (alreadyMember) {
            res.status(400).json({ success: false, message: "Người này đã là thành viên của nhóm" });
            return;
        }

        // Tạo lời mời (unique index sẽ bắt lỗi nếu đã có pending)
        const invitation = new GroupInvitation({
            groupId,
            inviterId,
            inviteeId,
            status: "pending",
        });
        await invitation.save();

        res.status(201).json({ success: true, message: "Đã gửi lời mời thành công" });
    } catch (error: any) {
        // Lỗi duplicate key = đã có lời mời pending
        if (error.code === 11000) {
            res.status(400).json({ success: false, message: "Đã có lời mời đang chờ xử lý cho người này" });
            return;
        }
        console.error("sendInvitation error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// THAM GIA NHÓM PUBLIC
// POST /api/groups/:groupId/join
// =====================================
export const joinPublicGroup = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        if (group.privacy !== ("Public" as string)) {
            res.status(403).json({ success: false, message: "Nhóm riêng tư chỉ tham gia qua lời mời" });
            return;
        }

        const alreadyMember = group.member.some((m) => m.userId.toString() === userId);
        if (alreadyMember) {
            res.status(400).json({ success: false, message: "Bạn đã là thành viên của nhóm này" });
            return;
        }

        await Group.findByIdAndUpdate(groupId, {
            $push: {
                member: {
                    userId: new mongoose.Types.ObjectId(userId),
                    role: "member",
                    joinAt: new Date(),
                },
            },
        });

        res.status(200).json({ success: true, message: "Tham gia nhóm thành công" });
    } catch (error) {
        console.error("joinPublicGroup error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// TẠO NHÓM MỚI
// POST /api/groups
// Body: { groupName, description, privacy }
// File (optional): avatar
// =====================================
export const createGroup = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupName, description, privacy } = req.body;

        if (!groupName || !privacy) {
            res.status(400).json({ success: false, message: "Thiếu groupName hoặc privacy" });
            return;
        }

        if (!["Public", "Private"].includes(privacy)) {
            res.status(400).json({ success: false, message: "privacy phải là 'Public' hoặc 'Private'" });
            return;
        }

        // Lấy avatar URL nếu có upload
        const file = req.file as (Express.Multer.File & { path?: string }) | undefined;
        const avatarUrl = file?.path ?? "";

        const newGroup = new Group({
            groupName,
            description: description ?? "",
            avatarUrl,
            creatorId: new mongoose.Types.ObjectId(userId),
            privacy,
            member: [
                {
                    userId: new mongoose.Types.ObjectId(userId),
                    role: "admin",
                    joinAt: new Date(),
                },
            ],
        });

        await newGroup.save();

        res.status(201).json({
            success: true,
            message: "Tạo nhóm thành công",
            data: {
                _id: newGroup._id,
                groupName: newGroup.groupName,
                privacy: newGroup.privacy,
                avatarUrl: newGroup.avatarUrl,
            },
        });
    } catch (error) {
        console.error("createGroup error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// LẤY BÀI VIẾT CỦA 1 NHÓM CỤ THỂ
// GET /api/groups/:groupId/posts?page=1&limit=10
// =====================================
export const getPostsByGroup = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;
        const page = Math.max(1, parseInt(req.query.page as string) || 1);
        const limit = Math.max(1, parseInt(req.query.limit as string) || 10);
        const skip = (page - 1) * limit;

        const group = await Group.findById(groupId).lean();
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        // Group private: chỉ thành viên mới xem được
        const isMember = group.member.some((m) => m.userId.toString() === userId);
        if ((group.privacy as string) === "Private" && !isMember) {
            res.status(403).json({ success: false, message: "Nhóm riêng tư, bạn cần là thành viên để xem bài viết" });
            return;
        }

        const total = await Post.countDocuments({ groupId });

        const posts = await Post.find({ groupId })
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(limit)
            .populate("authorId", "username avatar")
            .lean();

        const userObjectId = new mongoose.Types.ObjectId(userId);

        const postsWithDetails = await Promise.all(
            posts.map(async (post) => {
                const postIdObj = new mongoose.Types.ObjectId(post._id.toString());

                const [mediaList, countComment, countReaction, myReactDoc, topReactDocs] =
                    await Promise.all([
                        Media.find({ targetId: post._id, fileType: "image" }).lean(),
                        Comment.countDocuments({ postId: post._id }),
                        Reaction.countDocuments({ targetId: postIdObj }),
                        Reaction.findOne({ targetId: postIdObj, userId: userObjectId }).lean(),
                        Reaction.aggregate([
                            { $match: { targetId: postIdObj } },
                            { $group: { _id: "$type", count: { $sum: 1 } } },
                            { $sort: { count: -1 } },
                            { $limit: 2 },
                        ]),
                    ]);

                return {
                    ...post,
                    images: mediaList.map((m) => m.url),
                    countComment,
                    countReaction,
                    countShare: post.countShare ?? 0,
                    myReaction: myReactDoc?.type ?? null,
                    topReactions: topReactDocs.map((d) => d._id),
                };
            })
        );

        res.status(200).json({
            success: true,
            data: postsWithDetails,
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.ceil(total / limit),
            },
        });
    } catch (error) {
        console.error("getPostsByGroup error:", error);
        console.log(JSON.stringify({
            success: true,
            data: postsWithDetails,
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.ceil(total / limit),
            },
        }, null, 2));
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};
// LẤY CHI TIẾT NHÓM
// GET /api/groups/:groupId
// =====================================
export const getGroupDetail = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId)
            .populate("creatorId", "username avatar")
            .lean();

        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        // Nếu nhóm Private, chỉ thành viên mới xem được
        const isMember = group.member.some((m) => m.userId.toString() === userId);
        if ((group.privacy as string) === "Private" && !isMember) {
            res.status(403).json({ success: false, message: "Nhóm riêng tư" });
            return;
        }

        const isAdmin = group.member.some(
            (m) => m.userId.toString() === userId && m.role === "admin"
        );

        res.status(200).json({
            success: true,
            data: {
                _id: group._id,
                groupName: group.groupName,
                description: group.description,
                avatarUrl: group.avatarUrl,
                coverUrl: group.coverUrl,
                privacy: group.privacy,
                memberCount: group.member.length,
                isMember,
                isAdmin,
                createdAt: group.createdAt,
            },
        });
    } catch (error) {
        console.error("getGroupDetail error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// CẬP NHẬT NHÓM (tên, mô tả, ảnh đại diện) - chỉ admin
// PATCH /api/groups/:groupId
// Body: { groupName?, description?, privacy? }  +  file (optional)
// =====================================
export const updateGroup = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const member = group.member.find((m) => m.userId.toString() === userId);
        if (!member || member.role !== "admin") {
            res.status(403).json({ success: false, message: "Chỉ admin mới được cập nhật nhóm" });
            return;
        }

        const { groupName, description, privacy } = req.body;

        if (groupName !== undefined) group.groupName = groupName;
        if (description !== undefined) group.description = description;
        if (privacy !== undefined) {
            if (!["Public", "Private"].includes(privacy)) {
                res.status(400).json({ success: false, message: "privacy phải là 'Public' hoặc 'Private'" });
                return;
            }
            group.privacy = privacy;
        }

        // Nếu có upload ảnh mới
        const file = req.file as (Express.Multer.File & { path?: string }) | undefined;
        if (file?.path) {
            group.avatarUrl = file.path;
        }

        await group.save();

        res.status(200).json({
            success: true,
            message: "Cập nhật nhóm thành công",
            data: {
                _id: group._id,
                groupName: group.groupName,
                description: group.description,
                avatarUrl: group.avatarUrl,
                privacy: group.privacy,
            },
        });
    } catch (error) {
        console.error("updateGroup error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};
// =====================================
// LẤY DANH SÁCH THÀNH VIÊN
// GET /api/groups/:groupId/members
// =====================================
export const getGroupMembers = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId)
            .populate("member.userId", "username avatar")
            .lean();

        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const isMember = group.member.some((m: any) => m.userId._id?.toString() === userId || m.userId.toString() === userId);
        if ((group.privacy as string) === "Private" && !isMember) {
            res.status(403).json({ success: false, message: "Không có quyền xem thành viên nhóm riêng tư" });
            return;
        }

        const members = group.member.map((m: any) => ({
            userId: m.userId._id || m.userId,
            username: m.userId.username,
            avatar: m.userId.avatar,
            role: m.role,
            joinAt: m.joinAt,
        }));

        res.status(200).json({ success: true, data: members });
    } catch (error) {
        console.error("getGroupMembers error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// KICK THÀNH VIÊN (chỉ admin)
// DELETE /api/groups/:groupId/members/:memberId
// =====================================
export const kickMember = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const adminId = req.user!.id;
        const { groupId, memberId } = req.params;

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const requester = group.member.find((m) => m.userId.toString() === adminId);
        if (!requester || requester.role !== "admin") {
            res.status(403).json({ success: false, message: "Chỉ admin mới có thể kick thành viên" });
            return;
        }

        if (adminId === memberId) {
            res.status(400).json({ success: false, message: "Không thể tự kick bản thân" });
            return;
        }

        const targetMember = group.member.find((m) => m.userId.toString() === memberId);
        if (!targetMember) {
            res.status(404).json({ success: false, message: "Thành viên không tồn tại trong nhóm" });
            return;
        }

        await Group.findByIdAndUpdate(groupId, {
            $pull: { member: { userId: new mongoose.Types.ObjectId(memberId) } },
        });

        res.status(200).json({ success: true, message: "Đã xóa thành viên khỏi nhóm" });
    } catch (error) {
        console.error("kickMember error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};
