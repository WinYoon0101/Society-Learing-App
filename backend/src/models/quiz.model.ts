import mongoose from "mongoose";

const QuestionSchema = new mongoose.Schema({
    question: { type: String, required: true },
    A: { type: String, required: true },
    B: { type: String, required: true },
    C: { type: String, required: true },
    D: { type: String, required: true },
    correct: { type: String, enum: ["A", "B", "C", "D"], required: true }
});

const QuizSchema = new mongoose.Schema({
    title: { type: String, required: true },
    userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
    content: String,
    questions: [QuestionSchema],
    
    // --- Các trường bổ sung cho tính năng ôn tập ---
    bestScore: { type: Number, default: 0 },
    
    // Trạng thái: mới tạo, đang học, hoặc đã thuộc lòng
    status: { 
        type: String, 
        enum: ["new", "learning", "mastered"], 
        default: "new" 
    },

    // Ngày đến hạn ôn tập tiếp theo (Trái tim của tính năng ôn tập tự động)
    nextReview: { type: Date, default: Date.now },

    // Khoảng cách giữa các lần ôn (tính bằng ngày)
    interval: { type: Number, default: 0 },

    // Lần cuối cùng user làm bài này
    lastAttemptAt: { type: Date },

    createdAt: { type: Date, default: Date.now }
});

export default mongoose.model("Quiz", QuizSchema);