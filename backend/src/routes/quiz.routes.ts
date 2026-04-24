import express from "express";
import { generateAndSaveQuiz } from "../controllers/quiz.controller";
import { authenticate } from "../middlewares/auth.middleware";

const router = express.Router();

router.post("/generate-quiz", authenticate, generateAndSaveQuiz);

export default router;