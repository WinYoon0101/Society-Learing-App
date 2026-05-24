import mongoose, { Document, Schema } from "mongoose";

export type InvitationStatus = "pending" | "accepted" | "declined";

export interface IGroupInvitation extends Document {
    _id: mongoose.Types.ObjectId;
    groupId: mongoose.Types.ObjectId;
    inviterId: mongoose.Types.ObjectId;
    inviteeId: mongoose.Types.ObjectId;
    status: InvitationStatus;
    createdAt: Date;
    updatedAt: Date;
}

const GroupInvitationSchema = new Schema<IGroupInvitation>(
    {
        groupId: {
            type: Schema.Types.ObjectId,
            ref: "Group",
            required: true,
        },
        inviterId: {
            type: Schema.Types.ObjectId,
            ref: "User",
            required: true,
        },
        inviteeId: {
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
    { timestamps: true }
);

// Tránh mời trùng: 1 người chỉ có 1 lời mời pending cho 1 nhóm
GroupInvitationSchema.index({ groupId: 1, inviteeId: 1 }, { unique: true });

export default mongoose.model<IGroupInvitation>("GroupInvitation", GroupInvitationSchema);
