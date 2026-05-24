import express from "express";
import { getUserQuizzes, generateAndSaveQuiz, submitQuiz } from "../controllers/quiz.controller";
import { authenticate } from "../middlewares/auth.middleware";

const router = express.Router();

router.post("/generate-quiz", authenticate, generateAndSaveQuiz);
router.post("/submit", authenticate, submitQuiz);
router.get("/my-quizzes", authenticate, getUserQuizzes);

export default router;