import mongoose from "mongoose";

const QuestionSchema = new mongoose.Schema({
    question: String,
    A: String,
    B: String,
    C: String,
    D: String,
    correct: String
});

const QuizSchema = new mongoose.Schema({
    title: String,
    content: String,
    questions: [QuestionSchema],
    createdAt: { type: Date, default: Date.now }
});

export default mongoose.model("Quiz", QuizSchema);