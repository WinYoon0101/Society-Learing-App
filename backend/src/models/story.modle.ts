import mongoose, { Schema, Document } from 'mongoose';

export interface IStory extends Document {
    _id: mongoose.Types.ObjectId
    userId: mongoose.Types.ObjectId;  
    mediaUrl: string;                 
    mediaType: string;                 
    viewer: mongoose.Types.ObjectId[];
    numberReaction: number;           
    expiresAt: Date;               
    createdAt: Date;
    updatedAt: Date;
}

const StorySchema: Schema = new Schema({
    userId: { type: Schema.Types.ObjectId, 
        ref: 'User', 
        required: true 
    },
    mediaUrl: { type: String,
         required: true 
    },
    mediaType: { type: String, 
        enum: ['image', 'video', 'Text'], 
        required: true 
    },
    
    viewer: [
             { type: Schema.Types.ObjectId, 
               ref: 'User'
             }
            ],
    
    numberReaction: { type: Number, 
        default: 0 
    },

    expiresAt: { type: Date, required: true }

}, { timestamps: true });

// Hết giờ xóa in
StorySchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

// Lấy những Story chưa hết hạn
StorySchema.index({ userId: 1, expiresAt: 1 });

export default mongoose.model<IStory>('Story', StorySchema);