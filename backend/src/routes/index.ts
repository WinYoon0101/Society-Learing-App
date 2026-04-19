import { Router } from "express";

import authRoutes from "./auth.routes";
import friendRoutes from "./friend.routes";
import documentRoutes from "./document.routes";
import mediaRoutes from "./media.routes";
import postRoutes from "./post.routes";

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

export default router;