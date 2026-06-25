import mongoose, { Document, Schema } from "mongoose";

export interface IConversation extends Document {
  _id: mongoose.Types.ObjectId;
  members: mongoose.Types.ObjectId[];
  isGroup: boolean;
  name: string;
  admin?: mongoose.Types.ObjectId;
  nicknames: Map<string, string>;
  color: string;
  lastMessage?: mongoose.Types.ObjectId;
  deletedBy: mongoose.Types.ObjectId[];
  mutedMessages: mongoose.Types.ObjectId[];
  mutedCalls: mongoose.Types.ObjectId[];
  createdAt: Date;
  updatedAt: Date;
}

const ConversationSchema = new Schema<IConversation>(
  {
    members: [
      {
        type: Schema.Types.ObjectId,
        ref: "User",
        required: true,
      },
    ],
    // Group chat: nhiều hơn 2 thành viên + có tên + admin (người tạo)
    isGroup: {
      type: Boolean,
      default: false,
    },
    name: {
      type: String,
      default: "",
    },
    admin: {
      type: Schema.Types.ObjectId,
      ref: "User",
      default: null,
    },
    nicknames: {
      type: Map,
      of: String,
      default: {},
    },
    color: {
      type: String,
      default: "#0084ff",
    },
    lastMessage: {
      type: Schema.Types.ObjectId,
      ref: "Message",
      default: null,
    },
    // Ẩn đoạn chat cho riêng từng user (xóa-phía-mình); tin mới sẽ clear lại
    deletedBy: [
      {
        type: Schema.Types.ObjectId,
        ref: "User",
      },
    ],
    // Tắt thông báo per-user: ai trong mảng = đã tắt loại đó
    mutedMessages: [
      {
        type: Schema.Types.ObjectId,
        ref: "User",
      },
    ],
    mutedCalls: [
      {
        type: Schema.Types.ObjectId,
        ref: "User",
      },
    ],
  },
  { timestamps: true }
);

// Index để tìm conversation giữa 2 user nhanh hơn
ConversationSchema.index({ members: 1 });

const Conversation = mongoose.model<IConversation>(
  "Conversation",
  ConversationSchema
);

export default Conversation;
