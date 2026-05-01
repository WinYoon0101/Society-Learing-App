import express from "express";
import {
  updateProfile,
  updateAvatar,
  getMyProfile,
  updateCover,
} from "../controllers/user.controller";
import { authenticate } from "../middlewares/auth.middleware";

const router = express.Router();

router.put("/update", authenticate, updateProfile);
router.put("/avatar", authenticate, updateAvatar);
router.get("/profile", authenticate, getMyProfile);
router.put("/cover", authenticate, updateCover);

export default router;
