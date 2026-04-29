
import mongoose, { Schema, Document } from 'mongoose';

export interface IReaction extends Document {
    _id: mongoose.Types.ObjectId;
    userId: mongoose.Types.ObjectId;   
    targetId: mongoose.Types.ObjectId; 
    targetType: string;                
    type: string;                   
    createdAt: Date;
    updatedAt: Date;
}

const ReactionSchema: Schema = new Schema({
    userId: { 
        type: Schema.Types.ObjectId, 
        ref: 'User', 
        required: true 
    },

    targetId: { 
        type: Schema.Types.ObjectId, 
        required: true 
    },

    targetType: { 
        type: String, 
        enum: ['Post', 'Comment', 'Stories'], 
        required: true 
    },

    type: { 
        type: String, 
        enum: ['Like', 'Love', 'Haha', 'Wow', 'Angry', 'Sad'], 
        required: true 
    }

}, { timestamps: true }); 

// Đảm bảo một User (userId) chỉ được phép thả đúng 1 cảm xúc lên 1 mục tiêu (targetId).
ReactionSchema.index({ userId: 1, targetId: 1 }, { unique: true });

// Khi người dùng bấm vào xem "Danh sách những người đã thả tim bài viết này"
// Backend sẽ dùng lệnh find({ targetId: ... }), có index này nó sẽ lấy ra trong chớp mắt.
ReactionSchema.index({ targetId: 1 });

export default mongoose.model<IReaction>('Reaction', ReactionSchema);