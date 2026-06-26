import mongoose, { Document, Schema } from 'mongoose';

export interface IReport extends Document {
    reporterId: mongoose.Types.ObjectId; // Người tố cáo
    targetType: string;                  // Loại vi phạm: 'Post', 'Comment', 'User'
    targetId: mongoose.Types.ObjectId;   // ID của bài viết/bình luận/user bị report
    reason: string;                      // Lý do: 'Spam', 'Ngôn từ kích động', 'Lừa đảo'...
    status: string;                      // Trạng thái: 'pending' (chờ xử lý), 'resolved' (đã giải quyết), 'ignored' (bỏ qua)
    createdAt: Date;
    updatedAt: Date;
}

const ReportSchema: Schema = new Schema<IReport>(
    {
        reporterId: {
            type: Schema.Types.ObjectId,
            ref: 'User',
            required: true,
        },
        targetType: {
            type: String,
            required: true,
            enum: ['Post', 'Comment', 'User'], // Giới hạn các loại bị report
        },
        targetId: {
            type: Schema.Types.ObjectId,
            required: true,

        },
        reason: {
            type: String,
            required: true,
        },
        status: {
            type: String,
            default: 'pending',
            enum: ['pending', 'resolved', 'ignored'],
        },
    },
    {
        timestamps: true,
    }
);

// Đánh index để load danh sách báo cáo chờ xử lý nhanh hơn
ReportSchema.index({ status: 1, createdAt: -1 });

const Report = mongoose.model<IReport>('Report', ReportSchema);
export default Report;