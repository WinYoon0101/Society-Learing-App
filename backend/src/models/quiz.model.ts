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
    userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true }, // Ai tạo?
    content: String, // Nội dung gốc dùng để tạo quiz
    questions: [QuestionSchema],
    bestScore: { type: Number, default: 0 },
    createdAt: { type: Date, default: Date.now }
});

export default mongoose.model("Quiz", QuizSchema);