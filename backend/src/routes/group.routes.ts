import { Router } from "express";
import { authenticate } from "../middlewares/auth.middleware";
import { uploadImages, uploadFile } from "../middlewares/upload.middleware";
import {
    getMyGroups,
    getGroupPosts,
    discoverGroups,
    getMyInvitations,
    respondToInvitation,
    sendInvitation,
    joinPublicGroup,
    createGroup,
    getPostsByGroup,
    getGroupDetail,
    updateGroup,
    getGroupMembers,
    kickMember,
} from "../controllers/group.controller";

const router = Router();

// --- Tab "Nhóm của bạn" ---
router.get("/my", authenticate, getMyGroups);

// --- Tab "Bài viết" (tất cả nhóm đang tham gia) ---
router.get("/posts", authenticate, getGroupPosts);

// --- Tab "Khám phá" (hỗ trợ ?search=keyword) ---
router.get("/discover", authenticate, discoverGroups);

// --- Tab "Lời mời" ---
router.get("/invitations", authenticate, getMyInvitations);
router.post("/invitations", authenticate, sendInvitation);
router.patch("/invitations/:invitationId", authenticate, respondToInvitation);

// --- Tạo nhóm mới ---
router.post("/", authenticate, uploadFile, createGroup);

// --- Chi tiết nhóm + cập nhật (phải đặt TRƯỚC /:groupId/posts) ---
router.get("/:groupId", authenticate, getGroupDetail);
router.patch("/:groupId", authenticate, uploadFile, updateGroup);

// --- Tham gia nhóm public ---
router.post("/:groupId/join", authenticate, joinPublicGroup);

// --- Feed bài viết của 1 nhóm cụ thể ---
router.get("/:groupId/posts", authenticate, getPostsByGroup);

// --- Thành viên nhóm ---
router.get("/:groupId/members", authenticate, getGroupMembers);
router.delete("/:groupId/members/:memberId", authenticate, kickMember);

export default router;
