
import mongoose, { Schema, Document } from 'mongoose';

export interface IComment extends Document {
    _id: mongoose.Types.ObjectId;
    userId: mongoose.Types.ObjectId;   
    postId: mongoose.Types.ObjectId;   
    parentId?: mongoose.Types.ObjectId;
    content: string; 
    replies?: any[];                    
    createdAt: Date;
    updatedAt: Date;
}

const CommentSchema: Schema = new Schema({
    userId: {
         type: Schema.Types.ObjectId, 
         ref: 'User', 
         required: true 
    },
    postId: { 
        type: Schema.Types.ObjectId,
         ref: 'Post',
         required: true 
    },
    parentId: { type: Schema.Types.ObjectId, 
        ref: 'Comment', 
        default: null 
    },
    
    content: { 
        type: String,
        required: true
    },
}, 
{ timestamps: true });

CommentSchema.index({ postId: 1, createdAt: 1 }); 

CommentSchema.index({ parentId: 1, createdAt: 1 });

export default mongoose.model<IComment>('Comment', CommentSchema);