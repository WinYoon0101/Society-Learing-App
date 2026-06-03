import { Request, Response } from "express";
import axios from "axios";
import { extractJSON } from "../utils/parser";

const HF_API = "https://api-inference.huggingface.co/models/google/flan-t5-base";

function fallback(text: string, num = 5) {
    const sentences = text.split(/[.!?]/).filter(s => s.length > 20);

    return sentences.slice(0, num).map(s => ({
        question: s.replace(/\w+/, "_____"),
        A: "AI",
        B: "data",
        C: "code",
        D: "học",
        correct: "A"
    }));
}

export const generateQuiz = async (req: Request, res: Response) => {
    try {
        const { text, numQuestions = 5 } = req.body;

        if (!text) {
            return res.status(400).json({ error: "Text required" });
        }

        const prompt = `
Tạo ${numQuestions} câu hỏi trắc nghiệm từ nội dung sau.
Trả về JSON dạng:
[
 { "question": "", "A": "", "B": "", "C": "", "D": "", "correct": "A" }
]

Nội dung:
${text}
`;

        const response = await axios.post(
            HF_API,
            { inputs: prompt },
            {
                headers: {
                    Authorization: `Bearer ${process.env.HF_TOKEN}`
                }
            }
        );

        const raw = response.data?.[0]?.generated_text || "";
        const parsed = extractJSON(raw);

        if (!parsed) {
            return res.json({
                success: true,
                source: "fallback",
                quiz: fallback(text, numQuestions)
            });
        }

        res.json({
            success: true,
            source: "AI",
            quiz: parsed
        });

    } catch (err) {
        return res.json({
            success: true,
            source: "fallback",
            quiz: fallback(req.body.text)
        });
    }
};