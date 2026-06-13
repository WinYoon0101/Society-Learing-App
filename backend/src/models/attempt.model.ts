import mongoose from "mongoose";

const AttemptSchema = new mongoose.Schema({
    quizId: { type: mongoose.Schema.Types.ObjectId, ref: "Quiz", required: true },
    userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
    
    score: { type: Number, required: true }, // Điểm lần này (0-100)
    correctCount: { type: Number, required: true },
    totalQuestions: { type: Number, required: true },
    
    // Lưu chi tiết câu nào đúng, câu nào sai
    details: [{
        questionIndex: Number,
        userAnswer: String,
        isCorrect: Boolean
    }],

    timeSpent: { type: Number }, // (Tùy chọn) Thời gian làm bài tính bằng giây
    createdAt: { type: Date, default: Date.now }
});

export const Attempt = mongoose.model("Attempt", AttemptSchema);