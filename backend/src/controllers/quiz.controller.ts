import { Request, Response } from "express";
import { GoogleGenerativeAI, SchemaType, type Schema } from "@google/generative-ai";
import Quiz from "../models/quiz.model";
import { Attempt } from "../models/attempt.model";

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

// Hàm fallback dự phòng khi AI lỗi
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

// 3. Tạo Quiz bằng AI
export const generateAndSaveQuiz = async (req: Request, res: Response) => {
    try {
        const { text, numQuestions = 5, title = "Quiz mới" } = req.body;
        const userId = (req as any).user?.id;

        if (!userId) return res.status(401).json({ error: "Không tìm thấy người dùng" });
        if (!text) return res.status(400).json({ error: "Nội dung trống" });

        const prompt = `Hãy tạo ${numQuestions} câu hỏi trắc nghiệm bằng tiếng Việt dựa trên nội dung sau. Yêu cầu: 4 đáp án A, B, C, D không được trùng lặp và chỉ có 1 đáp án đúng. Nội dung: ${text}`;

        const result = await model.generateContent({
            contents: [{ role: 'user', parts: [{ text: prompt }] }],
            generationConfig: {
                responseMimeType: "application/json",
                responseSchema: schema,
                temperature: 0.2, // Giúp AI bớt "ảo giác"
            },
        });

        let rawText = result.response.text();
        
        // CHẶN LỖI: Dọn dẹp markdown block nếu Gemini trả về thừa ký tự
        rawText = rawText.replace(/```json/gi, "").replace(/```/g, "").trim();
        
        const quizData = JSON.parse(rawText);

        const newQuiz = new Quiz({
            title,
            userId,
            content: text,
            questions: quizData,
            nextReview: new Date(), 
            status: "new"
        });
        await newQuiz.save();

        res.status(201).json({ success: true, data: newQuiz });
    } catch (err: any) {
        console.error("Lỗi AI, chuyển sang Fallback:", err.message);
        
        // GỌI FALLBACK KHI AI BỊ LỖI / QUÁ TẢI
        try {
            const { text, numQuestions = 5, title = "Quiz mới (Dự phòng)" } = req.body;
            const userId = (req as any).user?.id;
            
            const fallbackData = fallback(text, numQuestions);
            
            const newQuiz = new Quiz({
                title,
                userId,
                content: text,
                questions: fallbackData,
                nextReview: new Date(), 
                status: "new"
            });
            await newQuiz.save();

            res.status(201).json({ 
                success: true, 
                message: "Đã tạo bằng hệ thống dự phòng do AI bận",
                data: newQuiz 
            });
        } catch (fallbackErr: any) {
            res.status(500).json({ error: "Máy chủ AI đang bận", detail: fallbackErr.message });
        }
    }
};

// 4. Nộp bài làm Quiz
export const submitQuiz = async (req: Request, res: Response) => {
    try {
        const { quizId, answers } = req.body; 
        const userId = (req as any).user?.id;

        const quiz = await Quiz.findById(quizId);
        if (!quiz) return res.status(404).json({ error: "Quiz không tồn tại" });

        // CHẶN LỖI: Bắt buộc số câu trả lời phải bằng số câu hỏi (Tránh sập server)
        if (!Array.isArray(answers) || answers.length !== quiz.questions.length) {
            return res.status(400).json({ 
                error: "Dữ liệu trả lời không hợp lệ. Số lượng câu trả lời phải bằng số câu hỏi." 
            });
        }

        let correctCount = 0;
        const details = quiz.questions.map((q: any, index: number) => {
            const isCorrect = q.correct === answers[index];
            if (isCorrect) correctCount++;
            return {
                questionIndex: index,
                userAnswer: answers[index],
                isCorrect
            };
        });

        const score = Math.round((correctCount / quiz.questions.length) * 100);

        // Thuật toán Spaced Repetition đơn giản
        let daysToAdd = 1;
        if (score >= 80) daysToAdd = 7;
        else if (score >= 50) daysToAdd = 3;

        const nextReview = new Date();
        nextReview.setDate(nextReview.getDate() + daysToAdd);

        quiz.lastAttemptAt = new Date();
        quiz.nextReview = nextReview;
        quiz.status = score >= 80 ? "mastered" : "learning";
        if (score > (quiz.bestScore || 0)) quiz.bestScore = score;
        await quiz.save();

        const newAttempt = new Attempt({
            quizId,
            userId,
            score,
            correctCount,
            totalQuestions: quiz.questions.length,
            details
        });
        await newAttempt.save();

        res.status(200).json({ success: true, score, nextReview, status: quiz.status });
    } catch (err: any) {
        res.status(500).json({ error: "Lỗi khi nộp bài", detail: err.message });
    }
};

// 5. Lấy bài đến hạn ôn tập
export const getDueQuizzes = async (req: Request, res: Response) => {
    try {
        const userId = (req as any).user?.id;
        const now = new Date();

        const dueQuizzes = await Quiz.find({
            userId,
            nextReview: { $lte: now } // Lấy bài có ngày hẹn nhỏ hơn hoặc bằng hiện tại
        }).sort({ nextReview: 1 });

        res.status(200).json({ success: true, count: dueQuizzes.length, data: dueQuizzes });
    } catch (err: any) {
        res.status(500).json({ error: "Lỗi server", detail: err.message });
    }
};

// 6. Xóa Quiz
export const deleteQuiz = async (req: Request, res: Response) => {
    try {
        const { id } = req.params;
        const userId = (req as any).user?.id;

        const quiz = await Quiz.findOneAndDelete({ _id: id, userId });
        if (!quiz) return res.status(404).json({ error: "Không tìm thấy Quiz để xóa" });

        // Xóa luôn lịch sử làm bài
        await Attempt.deleteMany({ quizId: id });

        res.status(200).json({ success: true, message: "Đã xóa thành công" });
    } catch (err: any) {
        res.status(500).json({ error: "Lỗi xóa quiz" });
    }
};

// 7. Lấy danh sách Quiz của User
export const getUserQuizzes = async (req: Request, res: Response) => {
    try {
        const userId = (req as any).user?.id; 

        // Tìm tất cả quiz của user này, sắp xếp mới nhất lên đầu
        const quizzes = await Quiz.find({ userId }).sort({ createdAt: -1 });

        res.status(200).json({
            success: true,
            data: quizzes
        });
    } catch (err: any) {
        res.status(500).json({ error: "Lỗi khi lấy danh sách Quiz", detail: err.message });
    }
};