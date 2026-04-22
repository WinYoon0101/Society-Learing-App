import express from "express";
import { generateAndSaveQuiz } from "../controllers/quiz.controller";

const router = express.Router();

router.post("/generate-quiz", generateAndSaveQuiz);

export default router;