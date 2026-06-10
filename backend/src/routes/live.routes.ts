import express from "express";
import { startLive, getActiveLives, endLive } from "../controllers/live.controller";
import { authenticate } from "../middlewares/auth.middleware";

const router = express.Router();

router.post("/start", authenticate, startLive);
router.get("/active", authenticate, getActiveLives);
router.put("/end/:liveId", authenticate, endLive);

export default router;