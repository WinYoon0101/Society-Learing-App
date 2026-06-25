import mongoose from "mongoose";
import { Response } from "express";
import { AuthRequest } from "../middlewares/auth.middleware";
import Group from "../models/group.model";
import Post from "../models/post.model";
import Media from "../models/media.model";
import Comment from "../models/comment.model";
import Reaction from "../models/reaction.model";
import GroupInvitation from "../models/groupInvitation.model";
import Notification from "../models/notification.model";
import User from "../models/user.model";

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
                    status: { $ne: "pending" },
                    createdAt: { $gte: sevenDaysAgo },
                });

                // Lấy thời điểm bài post mới nhất (bỏ bài đang chờ duyệt)
                const latestPost = await Post.findOne({ groupId: group._id, status: { $ne: "pending" } })
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

        const feedFilter = { groupId: { $in: groupIds }, status: { $ne: "pending" } };
        const total = await Post.countDocuments(feedFilter);

        const posts = await Post.find(feedFilter)
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

        // Lấy group mà user chưa là thành viên (gồm cả Public lẫn Private).
        // Nhóm đã gửi yêu cầu vẫn hiện (nút "Đã gửi yêu cầu, chờ duyệt") qua cờ hasPendingRequest.
        const filter: Record<string, unknown> = {
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
            hasPendingRequest: (group.pendingRequests ?? []).some(
                (p) => p.userId.toString() === userId.toString()
            ),
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
            const group = await Group.findById(invitation.groupId);
            if (!group) {
                res.status(404).json({ success: false, message: "Nhóm không còn tồn tại" });
                return;
            }

            await GroupInvitation.findByIdAndUpdate(invitationId, { status: "accepted" });

            const alreadyMember = group.member.some((m) => m.userId.toString() === userId);
            if (alreadyMember) {
                res.status(200).json({
                    success: true,
                    message: "Bạn đã là thành viên của nhóm",
                    data: { status: "joined" },
                });
                return;
            }

            // Lời mời từ admin = đã được duyệt sẵn → vào thẳng (kể cả nhóm Private).
            const inviterIsAdmin = group.member.some(
                (m) => m.userId.toString() === invitation.inviterId.toString() && m.role === "admin"
            );

            // Nhóm Public, hoặc người mời là admin → vào thẳng.
            // Nhóm Private + người mời là thành viên thường → chuyển thành yêu cầu chờ admin duyệt.
            if ((group.privacy as string) === "Public" || inviterIsAdmin) {
                await Group.findByIdAndUpdate(invitation.groupId, {
                    $push: {
                        member: {
                            userId: new mongoose.Types.ObjectId(userId),
                            role: "member",
                            joinAt: new Date(),
                        },
                    },
                });
                res.status(200).json({
                    success: true,
                    message: "Đã tham gia nhóm thành công",
                    data: { status: "joined" },
                });
                return;
            }

            // Private + người mời là thành viên thường → đẩy vào pendingRequests (nếu chưa có) + thông báo admin
            const alreadyPending = (group.pendingRequests ?? []).some(
                (p) => p.userId.toString() === userId
            );
            if (!alreadyPending) {
                await Group.findByIdAndUpdate(invitation.groupId, {
                    $push: {
                        pendingRequests: {
                            userId: new mongoose.Types.ObjectId(userId),
                            requestedAt: new Date(),
                        },
                    },
                });
                try {
                    const requester = await User.findById(userId).select("username");
                    const requesterName = requester?.username || "Một người dùng";
                    const admins = group.member.filter((m) => m.role === "admin");
                    await Notification.insertMany(
                        admins.map((a) => ({
                            recipient: a.userId,
                            sender: new mongoose.Types.ObjectId(userId),
                            type: "group_join_request",
                            targetId: group._id,
                            content: `${requesterName} đã yêu cầu tham gia nhóm ${group.groupName}`,
                        }))
                    );
                } catch (err) {
                    console.error("Lỗi tạo thông báo yêu cầu tham gia (từ lời mời):", err);
                }
            }
            res.status(200).json({
                success: true,
                message: "Đã gửi yêu cầu tham gia, chờ quản trị viên duyệt",
                data: { status: "pending" },
            });
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

        // Bất kỳ thành viên nào (kể cả nhóm Private) đều được mời bạn bè.
        // Lời mời chỉ là "đề cử"; với nhóm Private, khi người được mời đồng ý sẽ
        // chuyển thành yêu cầu chờ admin duyệt (xem respondToInvitation).
        const inviterMember = group.member.find((m) => m.userId.toString() === inviterId);
        if (!inviterMember) {
            res.status(403).json({ success: false, message: "Bạn không phải thành viên của nhóm này" });
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

        // Thông báo cho người được mời
        try {
            const inviter = await User.findById(inviterId).select("username");
            const inviterName = inviter?.username || "Một người dùng";
            await Notification.create({
                recipient: new mongoose.Types.ObjectId(inviteeId),
                sender: new mongoose.Types.ObjectId(inviterId),
                type: "group_invitation",
                targetId: group._id,
                content: `${inviterName} đã mời bạn vào nhóm ${group.groupName}`,
            });
        } catch (err) {
            console.error("Lỗi tạo thông báo lời mời:", err);
        }

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
// YÊU CẦU THAM GIA NHÓM
// POST /api/groups/:groupId/request-join
// - Nhóm Public: vào thẳng (status "joined")
// - Nhóm Private: đẩy vào pendingRequests chờ admin duyệt (status "pending")
// =====================================
export const requestJoinGroup = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const alreadyMember = group.member.some((m) => m.userId.toString() === userId);
        if (alreadyMember) {
            res.status(400).json({ success: false, message: "Bạn đã là thành viên của nhóm này" });
            return;
        }

        // Nhóm Public → tham gia ngay
        if ((group.privacy as string) === "Public") {
            await Group.findByIdAndUpdate(groupId, {
                $push: {
                    member: {
                        userId: new mongoose.Types.ObjectId(userId),
                        role: "member",
                        joinAt: new Date(),
                    },
                },
            });
            res.status(200).json({
                success: true,
                message: "Tham gia nhóm thành công",
                data: { status: "joined" },
            });
            return;
        }

        // Nhóm Private → gửi yêu cầu chờ duyệt
        const alreadyRequested = (group.pendingRequests ?? []).some(
            (p) => p.userId.toString() === userId
        );
        if (alreadyRequested) {
            res.status(400).json({ success: false, message: "Bạn đã gửi yêu cầu tham gia nhóm này" });
            return;
        }

        await Group.findByIdAndUpdate(groupId, {
            $push: {
                pendingRequests: {
                    userId: new mongoose.Types.ObjectId(userId),
                    requestedAt: new Date(),
                },
            },
        });

        // Thông báo cho tất cả admin của nhóm về yêu cầu tham gia
        try {
            const requester = await User.findById(userId).select("username");
            const requesterName = requester?.username || "Một người dùng";
            const admins = group.member.filter((m) => m.role === "admin");
            await Notification.insertMany(
                admins.map((a) => ({
                    recipient: a.userId,
                    sender: new mongoose.Types.ObjectId(userId),
                    type: "group_join_request",
                    targetId: group._id,
                    content: `${requesterName} đã yêu cầu tham gia nhóm ${group.groupName}`,
                }))
            );
        } catch (err) {
            console.error("Lỗi tạo thông báo yêu cầu tham gia:", err);
        }

        res.status(200).json({
            success: true,
            message: "Đã gửi yêu cầu tham gia, chờ quản trị viên duyệt",
            data: { status: "pending" },
        });
    } catch (error) {
        console.error("requestJoinGroup error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// DANH SÁCH YÊU CẦU THAM GIA ĐANG CHỜ (chỉ admin)
// GET /api/groups/:groupId/pending-members
// =====================================
export const getPendingMembers = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId)
            .populate("pendingRequests.userId", "username avatar")
            .lean();

        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const isAdmin = group.member.some(
            (m: any) => m.userId.toString() === userId && m.role === "admin"
        );
        if (!isAdmin) {
            res.status(403).json({ success: false, message: "Chỉ admin mới xem được yêu cầu tham gia" });
            return;
        }

        const pending = (group.pendingRequests ?? []).map((p: any) => ({
            userId: p.userId?._id || p.userId,
            username: p.userId?.username,
            avatar: p.userId?.avatar,
            requestedAt: p.requestedAt,
        }));

        res.status(200).json({ success: true, data: pending });
    } catch (error) {
        console.error("getPendingMembers error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// DUYỆT YÊU CẦU THAM GIA (chỉ admin)
// PATCH /api/groups/:groupId/members/:userId/approve
// =====================================
export const approveMember = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const adminId = req.user!.id;
        const { groupId, userId } = req.params;

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const isAdmin = group.member.some(
            (m) => m.userId.toString() === adminId && m.role === "admin"
        );
        if (!isAdmin) {
            res.status(403).json({ success: false, message: "Chỉ admin mới được duyệt thành viên" });
            return;
        }

        const isPending = (group.pendingRequests ?? []).some((p) => p.userId.toString() === userId);
        if (!isPending) {
            res.status(404).json({ success: false, message: "Không tìm thấy yêu cầu tham gia" });
            return;
        }

        await Group.findByIdAndUpdate(groupId, {
            $pull: { pendingRequests: { userId: new mongoose.Types.ObjectId(String(userId)) } },
            $push: {
                member: {
                    userId: new mongoose.Types.ObjectId(String(userId)),
                    role: "member",
                    joinAt: new Date(),
                },
            },
        });

        // Thông báo cho người được duyệt
        try {
            await Notification.create({
                recipient: new mongoose.Types.ObjectId(String(userId)),
                sender: new mongoose.Types.ObjectId(adminId),
                type: "group_request_approved",
                targetId: group._id,
                content: `Yêu cầu tham gia nhóm ${group.groupName} của bạn đã được duyệt`,
            });
        } catch (err) {
            console.error("Lỗi tạo thông báo duyệt thành viên:", err);
        }

        res.status(200).json({ success: true, message: "Đã duyệt thành viên" });
    } catch (error) {
        console.error("approveMember error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// TỪ CHỐI YÊU CẦU THAM GIA (chỉ admin)
// PATCH /api/groups/:groupId/members/:userId/reject
// =====================================
export const rejectMember = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const adminId = req.user!.id;
        const { groupId, userId } = req.params;

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const isAdmin = group.member.some(
            (m) => m.userId.toString() === adminId && m.role === "admin"
        );
        if (!isAdmin) {
            res.status(403).json({ success: false, message: "Chỉ admin mới được từ chối thành viên" });
            return;
        }

        await Group.findByIdAndUpdate(groupId, {
            $pull: { pendingRequests: { userId: new mongoose.Types.ObjectId(String(userId)) } },
        });

        res.status(200).json({ success: true, message: "Đã từ chối yêu cầu tham gia" });
    } catch (error) {
        console.error("rejectMember error:", error);
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
            requirePostApproval: privacy === "Private", // mặc định nhóm Private cần duyệt bài
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

        const total = await Post.countDocuments({ groupId, status: { $ne: "pending" } });

        const posts = await Post.find({ groupId, status: { $ne: "pending" } })
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
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};
// =====================================
// LẤY BÀI VIẾT CHỜ DUYỆT CỦA NHÓM (admin)
// GET /api/groups/:groupId/pending-posts
// =====================================
export const getPendingPosts = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId).lean();
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const isAdmin = group.member.some(
            (m) => m.userId.toString() === userId && m.role === "admin"
        );
        if (!isAdmin) {
            res.status(403).json({ success: false, message: "Chỉ admin mới xem được bài chờ duyệt" });
            return;
        }

        const posts = await Post.find({ groupId, status: "pending" })
            .sort({ createdAt: -1 })
            .populate("authorId", "username avatar")
            .lean();

        const postsWithDetails = await Promise.all(
            posts.map(async (post) => {
                const mediaList = await Media.find({ targetId: post._id, fileType: "image" }).lean();
                return {
                    ...post,
                    images: mediaList.map((m) => m.url),
                    countComment: 0,
                    countReaction: 0,
                    myReaction: null,
                    topReactions: [],
                };
            })
        );

        res.status(200).json({ success: true, data: postsWithDetails });
    } catch (error) {
        console.error("getPendingPosts error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
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

        // Non-member vẫn xem được thông tin cơ bản (preview) để có thể "Yêu cầu tham gia".
        // Bài viết của nhóm Private vẫn bị chặn ở getPostsByGroup.
        const isMember = group.member.some((m) => m.userId.toString() === userId);

        const isAdmin = group.member.some(
            (m) => m.userId.toString() === userId && m.role === "admin"
        );

        const hasPendingRequest = (group.pendingRequests ?? []).some(
            (p) => p.userId.toString() === userId
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
                hasPendingRequest,
                requirePostApproval: group.requirePostApproval ?? false,
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

        const { groupName, description, privacy, requirePostApproval } = req.body;

        if (groupName !== undefined) group.groupName = groupName;
        if (description !== undefined) group.description = description;
        if (privacy !== undefined) {
            if (!["Public", "Private"].includes(privacy)) {
                res.status(400).json({ success: false, message: "privacy phải là 'Public' hoặc 'Private'" });
                return;
            }
            group.privacy = privacy;
        }
        // requirePostApproval gửi qua multipart là chuỗi "true"/"false"
        if (requirePostApproval !== undefined) {
            group.requirePostApproval = requirePostApproval === true || requirePostApproval === "true";
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
                requirePostApproval: group.requirePostApproval,
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
// RỜI NHÓM
// POST /api/groups/:groupId/leave
// =====================================
export const leaveGroup = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const member = group.member.find((m) => m.userId.toString() === userId);
        if (!member) {
            res.status(400).json({ success: false, message: "Bạn không phải thành viên của nhóm này" });
            return;
        }

        // Quản trị viên duy nhất (còn thành viên khác) không được rời —
        // phải chuyển quyền cho người khác hoặc xóa nhóm trước.
        if (member.role === "admin") {
            const adminCount = group.member.filter((m) => m.role === "admin").length;
            if (adminCount === 1 && group.member.length > 1) {
                res.status(400).json({
                    success: false,
                    message: "Bạn là quản trị viên duy nhất. Hãy chuyển quyền cho thành viên khác hoặc xóa nhóm trước khi rời.",
                });
                return;
            }
        }

        await Group.findByIdAndUpdate(groupId, {
            $pull: { member: { userId: new mongoose.Types.ObjectId(userId) } },
        });

        res.status(200).json({ success: true, message: "Đã rời nhóm" });
    } catch (error) {
        console.error("leaveGroup error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};

// =====================================
// XÓA NHÓM (chỉ creator hoặc admin)
// DELETE /api/groups/:groupId
// Dọn luôn Post / Media / Comment / Reaction / GroupInvitation liên quan
// =====================================
export const deleteGroup = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user!.id;
        const { groupId } = req.params;

        const group = await Group.findById(groupId);
        if (!group) {
            res.status(404).json({ success: false, message: "Không tìm thấy nhóm" });
            return;
        }

        const isCreator = group.creatorId.toString() === userId;
        const isAdmin = group.member.some(
            (m) => m.userId.toString() === userId && m.role === "admin"
        );
        if (!isCreator && !isAdmin) {
            res.status(403).json({ success: false, message: "Chỉ quản trị viên mới được xóa nhóm" });
            return;
        }

        // Lấy toàn bộ bài viết của nhóm để dọn dữ liệu phụ thuộc
        const posts = await Post.find({ groupId }).select("_id").lean();
        const postIds = posts.map((p) => p._id);

        // Lấy comment của các bài để dọn reaction trên comment
        const comments = await Comment.find({ postId: { $in: postIds } }).select("_id").lean();
        const commentIds = comments.map((c) => c._id);

        await Promise.all([
            Media.deleteMany({ targetId: { $in: postIds }, sourceType: "post" }),
            Comment.deleteMany({ postId: { $in: postIds } }),
            Reaction.deleteMany({ targetId: { $in: [...postIds, ...commentIds] } }),
            Post.deleteMany({ groupId }),
            GroupInvitation.deleteMany({ groupId }),
        ]);

        await Group.findByIdAndDelete(groupId);

        res.status(200).json({ success: true, message: "Đã xóa nhóm" });
    } catch (error) {
        console.error("deleteGroup error:", error);
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
            $pull: { member: { userId: new mongoose.Types.ObjectId(String(memberId)) } },
        });

        res.status(200).json({ success: true, message: "Đã xóa thành viên khỏi nhóm" });
    } catch (error) {
        console.error("kickMember error:", error);
        res.status(500).json({ success: false, message: "Lỗi hệ thống" });
    }
};
