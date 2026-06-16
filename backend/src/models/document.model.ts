import mongoose, { Document, Schema } from "mongoose";

export interface IDocument extends Document {
  uploaderId: mongoose.Types.ObjectId;
  mediaId: mongoose.Types.ObjectId;
  groupId?: mongoose.Types.ObjectId;
  title: string;
  description?: string;
  fileType: string;
  subject: string;
  visibility: "public" | "private" | "group";
  numberView: number;
  numberDownload: number;
  mindmapData?: any; // Lưu JSON Sơ đồ tư duy
  createdAt: Date;
  updatedAt: Date;
}

const documentSchema: Schema = new Schema(
  {
    uploaderId: { type: Schema.Types.ObjectId, ref: "User", required: true },
    mediaId: { type: Schema.Types.ObjectId, ref: "Media", required: true },
    groupId: { type: Schema.Types.ObjectId, ref: "Group", default: null },
    title: { type: String, required: true },
    description: { type: String, default: "" },
    fileType: { type: String, required: true },
    subject: { type: String, required: true },
    visibility: {
      type: String,
      enum: ["public", "private", "group"],
      default: "public",
    },
    numberView: { type: Number, default: 0 },
    numberDownload: { type: Number, default: 0 },
    mindmapData: { type: Schema.Types.Mixed, default: null }, 
  },
  { timestamps: true }
);

export default mongoose.model<IDocument>("Document", documentSchema);