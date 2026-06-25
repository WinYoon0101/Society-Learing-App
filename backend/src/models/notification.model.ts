import mongoose, { Schema, Document } from 'mongoose';

export interface INotification extends Document {
    recipient: mongoose.Types.ObjectId;  // ID người nhận thông báo
    sender: mongoose.Types.ObjectId;     // ID người gửi (người thả tim/comment)
    type: string;                        // Loại: 'post_reaction', 'post_comment', 'comment_reply'
    targetId: mongoose.Types.ObjectId;   // ID đích để điều hướng (bài viết / nhóm tuỳ type)
    postId?: mongoose.Types.ObjectId;    // (tuỳ chọn) ID bài cần làm nổi bật, vd group_post_pending → highlight trong màn duyệt
    content: string;                     // Nội dung: "đã bình luận về bài viết của bạn"
    isRead: boolean;                     // Đã đọc chưa?
    createdAt: Date;
}

const NotificationSchema: Schema = new Schema(
    {
        recipient: { type: Schema.Types.ObjectId, ref: 'User', required: true },
        sender: { type: Schema.Types.ObjectId, ref: 'User', required: true },
        type: { type: String, required: true },
        targetId: { type: Schema.Types.ObjectId, required: true },
        postId: { type: Schema.Types.ObjectId, required: false },
        content: { type: String, required: true },
        isRead: { type: Boolean, default: false }
    },
    { timestamps: true }
);

export default mongoose.model<INotification>('Notification', NotificationSchema);