import express from "express";
import { generateQuiz } from "../controllers/quiz.controller";

const router = express.Router();

router.post("/generate-quiz", generateQuiz);

export default router;