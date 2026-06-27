import express from "express";
import {
  updateProfile,
  updateAvatar,
  getMyProfile,
  updateCover,
  searchUsers,
  getUserById,
  deleteAvatar,
  deleteCover,
} from "../controllers/user.controller";
import { authenticate } from "../middlewares/auth.middleware";
import { uploadFile } from "../middlewares/upload.middleware";

const router = express.Router();

router.get("/search", authenticate, searchUsers);
router.put("/update", authenticate, updateProfile);
router.put("/avatar", authenticate, uploadFile, updateAvatar);
router.get("/profile", authenticate, getMyProfile);
router.put("/cover", authenticate, uploadFile, updateCover);
router.get("/:id", authenticate, getUserById);
router.delete("/avatar", authenticate, deleteAvatar);
router.delete("/cover", authenticate, deleteCover);

export default router;
