import express from "express";
import { getUserQuizzes, generateAndSaveQuiz } from "../controllers/quiz.controller";
import { authenticate } from "../middlewares/auth.middleware";

const router = express.Router();

router.post("/generate-quiz", authenticate, generateAndSaveQuiz);
router.get("/my-quizzes", authenticate, getUserQuizzes);

export default router;