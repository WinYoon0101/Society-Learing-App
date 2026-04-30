import express from "express";
import {
  createPost,
  getFeed,
  deletePost,

} from "../controllers/post.controller";
import { authenticate } from "../middlewares/auth.middleware"; // Ktra đăng nhập
import { uploadMedia } from "../middlewares/upload.middleware"; // Xử lý upload ảnh

const router = express.Router();
router.post("/create", authenticate, uploadMedia, createPost);
router.get("/feed", authenticate, getFeed);
router.delete("/:id", authenticate, deletePost);
// router.get("/me", authenticate, getMyPosts);

export default router;
