import { Router } from "express";

import authRoutes from "./auth.routes";
import friendRoutes from "./friend.routes";
import documentRoutes from "./document.routes";
import mediaRoutes from "./media.routes";
import userRoutes from "./user.routes";
import postRoutes from "./post.routes";

import chatRoutes from "./chat.routes";

import quizRoutes from "./quiz.routes";

// 1. THÊM DÒNG IMPORT NÀY
import commentRoutes from "./comment.routes"; // (Tên file bên trong ./ có thể khác tùy bạn đặt nhé, vd: comment.routes)
import reactionRoutes from "./reaction.routes";


const router = Router();

// Auth routes
router.use("/auth", authRoutes);

// Friend routes
router.use("/friends", friendRoutes);

// Document routes
router.use("/documents", documentRoutes);

// Media routes
router.use("/media", mediaRoutes);

// User routes
router.use("/user", userRoutes);

// Post routes
router.use('/posts', postRoutes);

// Chat routes
router.use('/chat', chatRoutes);

router.use("/comments", commentRoutes); 
router.use('/quiz', quizRoutes);

router.use("/reactions", reactionRoutes);


export default router;