import { Router } from "express";

import authRoutes from "./auth.routes";
import friendRoutes from "./friend.routes";
import documentRoutes from "./document.routes";
import mediaRoutes from "./media.routes";
import postRoutes from "./post.routes";
import chatRoutes from "./chat.routes";

const router = Router();

// Auth routes
router.use("/auth", authRoutes);

// Friend routes
router.use("/friends", friendRoutes);

// Document routes
router.use("/documents", documentRoutes);

// Media routes
router.use("/media", mediaRoutes);

router.use('/posts', postRoutes);

// Chat routes
router.use('/chat', chatRoutes);

export default router;