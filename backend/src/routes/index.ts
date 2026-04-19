import { Router } from "express";

import authRoutes from "./auth.routes";
import friendRoutes from "./friend.routes";
import documentRoutes from "./document.routes";
import mediaRoutes from "./media.routes";
import userRoutes from "./user.routes";

const router = Router();

// Auth routes
router.use("/auth", authRoutes);

// Friend routes
router.use("/friends", friendRoutes);

// Document routes
router.use("/documents", documentRoutes);

// Media routes
router.use("/media", mediaRoutes);

//User routes
router.use("/user", userRoutes);
export default router;
