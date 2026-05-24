import mongoose, { Document, Schema } from "mongoose";

export type ReactionEmoji = "❤️" | "😆" | "😮" | "😢" | "😡" | "👍";

export interface IReaction {
  userId: mongoose.Types.ObjectId;
  emoji: ReactionEmoji;
}

export interface IMessage extends Document {
  _id: mongoose.Types.ObjectId;
  conversationId: mongoose.Types.ObjectId;
  sender: mongoose.Types.ObjectId;
  text: string;
  replyTo?: mongoose.Types.ObjectId; // reply tin nhắn
  reactions: IReaction[];            // thả cảm xúc
  isDeleted: boolean;
  mediaUrl?: string;                 // Cloudinary URL nếu có đính kèm file
  mediaType?: "image" | "video" | "document"; // loại file đính kèm
  createdAt: Date;
  updatedAt: Date;
}

const ReactionSchema = new Schema<IReaction>(
  {
    userId: { type: Schema.Types.ObjectId, ref: "User", required: true },
    emoji: {
      type: String,
      enum: ["❤️", "😆", "😮", "😢", "😡", "👍"],
      required: true,
    },
  },
  { _id: false }
);

const MessageSchema = new Schema<IMessage>(
  {
    conversationId: {
      type: Schema.Types.ObjectId,
      ref: "Conversation",
      required: true,
    },
    sender: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    text: {
      type: String,
      required: false,
      default: "",
      trim: true,
      maxlength: [5000, "Tin nhắn không được vượt quá 5000 ký tự"],
    },
    mediaUrl: {
      type: String,
      default: null,
    },
    mediaType: {
      type: String,
      enum: ["image", "video", "document"],
      default: null,
    },
    replyTo: {
      type: Schema.Types.ObjectId,
      ref: "Message",
      default: null,
    },
    reactions: {
      type: [ReactionSchema],
      default: [],
    },
    isDeleted: {
      type: Boolean,
      default: false,
    },
  },
  { timestamps: true }
);

MessageSchema.index({ conversationId: 1, createdAt: -1 });

const Message = mongoose.model<IMessage>("Message", MessageSchema);

export default Message;
