import mongoose, { Document, Schema } from "mongoose";

export interface IStory extends Document {
  authorId: mongoose.Types.ObjectId;
  mediaUrl: string;
  mediaType: "image" | "video";
  caption?: string;
  viewers: mongoose.Types.ObjectId[];
  expiresAt: Date;
  createdAt: Date;
}

const StorySchema = new Schema<IStory>(
  {
    authorId: { type: Schema.Types.ObjectId, ref: "User", required: true },
    mediaUrl:  { type: String, required: true },
    mediaType: { type: String, enum: ["image", "video"], default: "image" },
    caption:   { type: String, default: "" },
    viewers:   [{ type: Schema.Types.ObjectId, ref: "User" }],
    expiresAt: { type: Date, default: () => new Date(Date.now() + 24 * 60 * 60 * 1000) },
  },
  { timestamps: true }
);

// Tự xóa sau 24h
StorySchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });
StorySchema.index({ authorId: 1, createdAt: -1 });

export default mongoose.model<IStory>("Story", StorySchema);
