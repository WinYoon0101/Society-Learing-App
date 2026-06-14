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
import notificationRoutes from "./notification.routes"; 

import chatRoutes from "./chat.routes";

import quizRoutes from "./quiz.routes";

import liveRoutes from "./live.routes";
import commentRoutes from "./comment.routes"; 
import reactionRoutes from "./reaction.routes";
import groupRoutes from "./group.routes";
import notificationRoutes from "./notification.routes";


import storyRoutes from "./story.routes";

const router = Router();

router.use("/auth", authRoutes);
router.use("/friends", friendRoutes);
router.use("/documents", documentRoutes);
router.use("/media", mediaRoutes);
router.use("/user", userRoutes);
router.use('/posts', postRoutes);
router.use('/chat', chatRoutes);
router.use("/comments", commentRoutes); 
router.use('/quiz', quizRoutes);
router.use("/reactions", reactionRoutes);
router.use("/notifications", notificationRoutes);

export default router;