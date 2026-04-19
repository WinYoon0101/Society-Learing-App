import express from "express";
import { updateProfile, updateAvatar } from "../controllers/user.controller";
import { authenticate } from "../middlewares/auth.middleware";

const router = express.Router();

router.put("/update", authenticate, updateProfile);
router.put("/avatar", authenticate, updateAvatar);

export default router;
