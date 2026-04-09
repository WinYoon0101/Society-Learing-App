import mongoose, { Document, Schema } from "mongoose";

export interface IFriend extends Document {
  requester: mongoose.Types.ObjectId;
  recipient: mongoose.Types.ObjectId;
  status: "pending" | "accepted" | "declined";
  createdAt: Date;
  updatedAt: Date;
}

const FriendSchema = new Schema<IFriend>(
  {
    requester: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    recipient: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    status: {
      type: String,
      enum: ["pending", "accepted", "declined"],
      default: "pending",
    },
  },
  {
    timestamps: true,
  }
);

// Tránh việc gửi nhiều lời mời giữa 2 người dùng chưa được xử lý
FriendSchema.index({ requester: 1, recipient: 1 }, { unique: true });

// Tối ưu query theo requester
FriendSchema.index({ requester: 1, status: 1 });

// Tối ưu query theo recipient
FriendSchema.index({ recipient: 1, status: 1 });


const Friend = mongoose.model<IFriend>("Friend", FriendSchema);

export default Friend;
