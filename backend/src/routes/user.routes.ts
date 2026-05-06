import express from "express";
import {
  updateProfile,
  updateAvatar,
  getMyProfile,
  updateCover,
} from "../controllers/user.controller";
import { authenticate } from "../middlewares/auth.middleware";
import { uploadFile } from "../middlewares/upload.middleware";

const router = express.Router();

router.put("/update", authenticate, updateProfile);
router.put("/avatar", authenticate, uploadFile, updateAvatar);
router.get("/profile", authenticate, getMyProfile);
router.put("/cover", authenticate, uploadFile, updateCover);

export default router;
