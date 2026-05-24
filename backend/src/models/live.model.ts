import mongoose, { Schema, Document } from "mongoose";

export interface ILive extends Document {
    hostId: mongoose.Types.ObjectId;
    liveId: string;
    title: string;
    status: 'streaming' | 'ended';
    createdAt: Date;
}

const LiveSchema: Schema = new Schema({
    hostId: { type: Schema.Types.ObjectId, ref: "User", required: true },
    liveId: { type: String, required: true, unique: true },
    title: { type: String, default: "Buổi phát trực tiếp mới" },
    status: { type: String, enum: ['streaming', 'ended'], default: 'streaming' },
    createdAt: { type: Date, default: Date.now }
});

export default mongoose.model<ILive>("Live", LiveSchema);