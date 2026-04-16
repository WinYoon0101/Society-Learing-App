import mongoose, { Document, Schema } from "mongoose";

export type DocumentVisibility = "public" | "private" | "group";

export interface IDocument extends Document {
  _id: mongoose.Types.ObjectId;
  uploaderId: mongoose.Types.ObjectId;
  groupId?: mongoose.Types.ObjectId;
  mediaId: mongoose.Types.ObjectId;
  title: string;
  description?: string;
  fileType: string;
  subject: string;
  visibility: DocumentVisibility;
  numberView: number;
  numberDownload: number;
  createdAt: Date;
  updatedAt: Date;
}

const DocumentSchema = new Schema<IDocument>(
  {
    uploaderId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: [true, "Người tải lên là bắt buộc"],
    },
    groupId: {
      type: Schema.Types.ObjectId,
      ref: "Group",
      default: null,
    },
    mediaId: {
      type: Schema.Types.ObjectId,
      ref: "Media",
      required: [true, "Media là bắt buộc"],
    },
    title: {
      type: String,
      required: [true, "Tiêu đề là bắt buộc"],
      trim: true,
      maxlength: [200, "Tiêu đề không được vượt quá 200 ký tự"],
    },
    description: {
      type: String,
      trim: true,
      maxlength: [1000, "Mô tả không được vượt quá 1000 ký tự"],
      default: null,
    },
    fileType: {
      type: String,
      required: [true, "Loại file là bắt buộc"],
      trim: true,
    },
    subject: {
      type: String,
      required: [true, "Môn học / Chủ đề là bắt buộc"],
      trim: true,
      maxlength: [100, "Môn học không được vượt quá 100 ký tự"],
    },
    visibility: {
      type: String,
      enum: {
        values: ["public", "private", "group"],
        message: "visibility phải là public, private hoặc group",
      },
      default: "public",
    },
    numberView: {
      type: Number,
      default: 0,
      min: 0,
    },
    numberDownload: {
      type: Number,
      default: 0,
      min: 0,
    },
  },
  {
    timestamps: true,
  }
);

// ─── Indexes ──────────────────────────────────────────────────────────────────

// Full-text search trên title + subject
DocumentSchema.index({ title: "text", subject: "text", description: "text" });

// Lọc theo uploader + visibility
DocumentSchema.index({ uploaderId: 1, visibility: 1 });

// Lọc theo subject (browse)
DocumentSchema.index({ subject: 1, visibility: 1 });

// Sắp xếp theo thời gian
DocumentSchema.index({ createdAt: -1 });

// Group docs
DocumentSchema.index({ groupId: 1, visibility: 1 });

const DocumentModel = mongoose.model<IDocument>("Document", DocumentSchema);

export default DocumentModel;
