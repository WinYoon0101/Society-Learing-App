import { Request, Response } from "express";
import { GoogleGenerativeAI, SchemaType, type Schema } from "@google/generative-ai";
import Quiz from "../models/quiz.model";

// 1. Cấu hình Gemini
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY || "");

// 2. Định nghĩa cấu trúc dữ liệu trả về từ Gemini
const schema: Schema = {
    description: "Danh sách câu hỏi trắc nghiệm",
    type: SchemaType.ARRAY,
    items: {
        type: SchemaType.OBJECT,
        properties: {
            question: { type: SchemaType.STRING, description: "Nội dung câu hỏi" },
            A: { type: SchemaType.STRING },
            B: { type: SchemaType.STRING },
            C: { type: SchemaType.STRING },
            D: { type: SchemaType.STRING },
            correct: { 
                type: SchemaType.STRING, 
                description: "Chỉ chọn 1 trong 4 chữ cái: A, B, C, hoặc D" 
            },
        },
        required: ["question", "A", "B", "C", "D", "correct"],
    },
};

const model = genAI.getGenerativeModel({ 
    model: "gemini-2.5-flash" 
});

export const generateAndSaveQuiz = async (req: Request, res: Response) => {
    try {
        const { text, numQuestions = 5, title = "Quiz mới" } = req.body;
        const userId = (req as any).user?.id; 

if (!userId) {
    return res.status(401).json({ error: "Không tìm thấy thông tin người dùng" });
}

        if (!text) return res.status(400).json({ error: "Nội dung trống" });

        const prompt = `Hãy tạo ${numQuestions} câu hỏi trắc nghiệm bằng tiếng Việt dựa trên nội dung sau: ${text}`;

        // Gọi AI và ép kiểu trả về JSON ở đây
        const result = await model.generateContent({
            contents: [{ role: 'user', parts: [{ text: prompt }] }],
            generationConfig: {
                responseMimeType: "application/json",
                responseSchema: schema, // schema khai báo ở trên đầu file
            },
        });

        const rawText = result.response.text();
        const quizData = JSON.parse(rawText);

        // Lưu DB như cũ...
        const newQuiz = new Quiz({
            title,
            userId,
            content: text,
            questions: quizData
        });
        await newQuiz.save();

        res.status(201).json({ success: true, data: newQuiz });

    } catch (err: any) {
        console.error("LOI_BACKEND:", err);
        res.status(500).json({ 
            error: "Máy chủ AI đang bận", 
            detail: err.message 
        });
    }
};

// Hàm fallback 
function fallback(text: string, num = 5) {
    const sentences = text.split(/[.!?]/).filter(s => s.trim().length > 20);
    const choices = ["A", "B", "C", "D"];
    return sentences.slice(0, num).map(s => ({
        question: `Nội dung nào liên quan đến: "${s.trim().substring(0, 60)}..."?`,
        A: "Ý chính của đoạn văn",
        B: "Số liệu thống kê",
        C: "Ví dụ minh họa",
        D: "Kết luận",
        correct: choices[Math.floor(Math.random() * 4)]
    }));
}