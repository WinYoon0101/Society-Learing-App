import mongoose, { Document, Schema } from "mongoose";

export type MediaFileType = "image" | "video" | "document";
export type MediaSourceType = "post" | "story" | "message";

export interface IMedia extends Document {
  _id: mongoose.Types.ObjectId;
  userId: mongoose.Types.ObjectId;
  url: string;
  fileType: MediaFileType;
  sourceType: MediaSourceType;
  targetId: mongoose.Types.ObjectId;
  createdAt: Date;
  updatedAt: Date;
}

const MediaSchema = new Schema<IMedia>(
  {
    userId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: [true, "userId là bắt buộc"],
    },
    url: {
      type: String,
      required: [true, "URL là bắt buộc"],
      trim: true,
    },
    fileType: {
      type: String,
      required: [true, "fileType là bắt buộc"],
      enum: {
        values: ["image", "video", "document"],
        message: "fileType phải là image, video hoặc document",
      },
    },
    sourceType: {
      type: String,
      required: [true, "sourceType là bắt buộc"],
      enum: {
        values: ["post", "story", "message"],
        message: "sourceType phải là post, story hoặc message",
      },
    },
    targetId: {
      type: Schema.Types.ObjectId,
      required: [true, "targetId là bắt buộc"],
    },
  },
  {
    timestamps: true,
  }
);

// ─── Indexes ──────────────────────────────────────────────────────────────────

// Truy vấn media theo user
MediaSchema.index({ userId: 1, createdAt: -1 });

// Truy vấn media theo nguồn (ví dụ: tất cả ảnh của 1 post)
MediaSchema.index({ sourceType: 1, targetId: 1 });

// Lọc theo loại file
MediaSchema.index({ fileType: 1 });

const Media = mongoose.model<IMedia>("Media", MediaSchema);

export default Media;
