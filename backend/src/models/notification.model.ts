import mongoose, { Document, Schema } from "mongoose";

export interface INotification extends Document {
    receiverId: mongoose.Types.ObjectId;
    senderId: mongoose.Types.ObjectId;
    targetId: mongoose.Types.ObjectId;
    targetType: string;
    type: string;
    isRead: boolean;
}

const NotificationSchema: Schema = new Schema<INotification>(
    {
        receiverId: {
            type: Schema.Types.ObjectId,
            ref: "User",
            required: true,
        },
        senderId: {
            type: Schema.Types.ObjectId,
            ref: "User",
            required: true,
        },
        targetId: {
            type: Schema.Types.ObjectId,
            required: true, 
        },
        targetType: {
            type: String,
            required: true,
        },
        type: {
            type: String,
            required: true,
        },
        isRead: {
            type: Boolean,
            default: false,
        },
    },
    {
        timestamps: true, 
    }
);

NotificationSchema.index({ receiverId: 1, createdAt: -1 });

export default mongoose.model<INotification>("Notification", NotificationSchema);