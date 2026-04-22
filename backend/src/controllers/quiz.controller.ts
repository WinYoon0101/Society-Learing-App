// controllers/quizController.ts
import { Request, Response } from "express";
import axios from "axios";
import { extractJSON } from "../utils/parser";
import Quiz from "../models/quiz.model";

const HF_API = "https://api-inference.huggingface.co/models/google/flan-t5-base";

// Hàm bổ sung để làm sạch text tránh lỗi JSON
function cleanText(text: string) {
    return text.replace(/[\n\r\t]/g, " ").trim();
}

export const generateAndSaveQuiz = async (req: Request, res: Response) => {
    try {       
        const { text, numQuestions = 5, title = "Quiz mới" } = req.body;
        const userId = (req as any).user?._id || "unknown"; 

        if (!text) return res.status(400).json({ error: "Nội dung không được để trống" });

        const prompt = `Task: Create a JSON quiz in Vietnamese.
Content: ${cleanText(text)}
Format: Array of objects [{ "question": "...", "A": "...", "B": "...", "C": "...", "D": "...", "correct": "A" }]
Constraint: Return ONLY JSON. Generate ${numQuestions} questions.`;

        const response = await axios.post(
            HF_API,
            { inputs: prompt },
            {
                headers: { Authorization: `Bearer ${process.env.HF_TOKEN}` },
                timeout: 15000
            }
        );

        let quizData = [];
        const raw = response.data?.[0]?.generated_text || "";
        const parsed = extractJSON(raw);

        // Kiểm tra nếu AI fail thì dùng fallback
        if (!parsed || parsed.length === 0) {
            quizData = fallback(text, numQuestions);
        } else {
            quizData = parsed;
        }

        // --- LƯU VÀO DATABASE ---
        const newQuiz = new Quiz({
            title,
            userId,
            content: text,
            questions: quizData
        });

        await newQuiz.save();

        res.status(201).json({
            success: true,
            source: parsed ? "AI" : "fallback",
            quiz: newQuiz
        });

    } catch (err: any) {
        console.error("Lỗi tạo quiz:", err.message);
        res.status(500).json({ error: "Không thể tạo quiz lúc này" });
    }
};

// Hàm fallback 
function fallback(text: string, num = 5) {
    const sentences = text.split(/[.!?]/).filter(s => s.trim().length > 30);
    return sentences.slice(0, num).map(s => ({
        question: `Nội dung nào sau đây liên quan đến: "${s.trim().substring(0, 50)}..."?`,
        A: "Khái niệm chính trong bài",
        B: "Dữ liệu thực nghiệm",
        C: "Phương pháp nghiên cứu",
        D: "Kết luận vấn đề",
        correct: "A"
    }));
}