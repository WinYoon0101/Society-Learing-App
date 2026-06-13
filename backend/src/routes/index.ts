import { Router } from "express";

import authRoutes from "./auth.routes";
import friendRoutes from "./friend.routes";
import documentRoutes from "./document.routes";
import mediaRoutes from "./media.routes";
import userRoutes from "./user.routes";
import postRoutes from "./post.routes";
import chatRoutes from "./chat.routes";
import quizRoutes from "./quiz.routes";
import commentRoutes from "./comment.routes"; 
import reactionRoutes from "./reaction.routes";

// 1. ĐÃ THÊM: Import file route của Notification
import notificationRoutes from "./notification.routes"; 

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

// Comment routes
router.use("/comments", commentRoutes); 

// Quiz routes
router.use('/quiz', quizRoutes);

// Reaction routes
router.use("/reactions", reactionRoutes);

// 2. ĐÃ THÊM: Khai báo đường dẫn cho Notification
router.use("/notifications", notificationRoutes);

export default router;