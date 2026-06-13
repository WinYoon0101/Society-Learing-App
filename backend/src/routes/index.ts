import { Router } from "express";

import authRoutes from "./auth.routes";
import friendRoutes from "./friend.routes";
import documentRoutes from "./document.routes";
import mediaRoutes from "./media.routes";
import userRoutes from "./user.routes";
import postRoutes from "./post.routes";

import chatRoutes from "./chat.routes";

import quizRoutes from "./quiz.routes";

import liveRoutes from "./live.routes";
import commentRoutes from "./comment.routes"; 
import reactionRoutes from "./reaction.routes";
import groupRoutes from "./group.routes";
import notificationRoutes from "./notification.routes";


import storyRoutes from "./story.routes";

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

// Group routes
router.use("/groups", groupRoutes);

// Live routes
router.use("/live", liveRoutes);

// Notification routes
router.use("/notifications", notificationRoutes);

// Story routes
router.use("/stories", storyRoutes);

export default router;