import mongoose, { Schema } from "mongoose";

export interface ITask {
  userId: mongoose.Types.ObjectId;
  title: string;
  description: string;
  dueDate: Date;
  priority: "daily" | "medium" | "high";
  status: "pending" | "completed";
  createdAt: Date;
  updatedAt: Date;
}
const TaskSchema = new Schema(
  {
    userId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },

    title: {
      type: String,
      required: true,
      trim: true,
    },

    description: {
      type: String,
      default: "",
      trim: true,
    },

    dueDate: {
      type: Date,
      required: true,
    },

    priority: {
      type: String,
      enum: ["daily", "medium", "high"],
      default: "medium",
    },

    status: {
      type: String,
      enum: ["pending", "completed"],
      default: "pending",
    },
  },
  {
    timestamps: true,
    versionKey: false,
  },
);

export default mongoose.model("Task", TaskSchema);
