import express from "express";
import {
  updateProfile,
  updateAvatar,
  getMyProfile,
} from "../controllers/user.controller";
import { authenticate } from "../middlewares/auth.middleware";

const router = express.Router();

router.put("/update", authenticate, updateProfile);
router.put("/avatar", authenticate, updateAvatar);
router.get("/profile", authenticate, getMyProfile);

export default router;
