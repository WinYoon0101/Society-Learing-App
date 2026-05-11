import { Router } from "express";
import { authenticate } from "../middlewares/auth.middleware";
import { uploadImages } from "../middlewares/upload.middleware";
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
} from "../controllers/group.controller";

const router = Router();

// --- Tab "Nhóm của bạn" ---
router.get("/my", authenticate, getMyGroups);

// --- Tab "Bài viết" ---
router.get("/posts", authenticate, getGroupPosts);

// --- Tab "Khám phá" ---
router.get("/discover", authenticate, discoverGroups);

// --- Tab "Lời mời" ---
router.get("/invitations", authenticate, getMyInvitations);
router.post("/invitations", authenticate, sendInvitation);
router.patch("/invitations/:invitationId", authenticate, respondToInvitation);

// --- Tham gia nhóm public ---
router.post("/:groupId/join", authenticate, joinPublicGroup);

// --- Feed bài viết của 1 nhóm cụ thể ---
router.get("/:groupId/posts", authenticate, getPostsByGroup);

// --- Tạo nhóm mới ---
router.post("/", authenticate, uploadImages, createGroup);

export default router;
