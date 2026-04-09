import { Router } from "express";

import authRoutes from "./auth.routes";
import friendRoutes from "./friend.routes";

const router = Router();

// Auth routes
router.use("/auth", authRoutes);

// Friend routes
router.use("/friends", friendRoutes);

export default router;