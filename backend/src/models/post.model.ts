import mongoose, { Document, Schema } from "mongoose"

export interface IPost extends Document {
    _id: mongoose.Types.ObjectId;
    authorId: mongoose.Types.ObjectId;
    groupId: mongoose.Types.ObjectId;
    sharedPostId?: mongoose.Types.ObjectId;
    content: string;
    privacy: string;
    feeling: string; // 👉 BỔ SUNG CẢM XÚC
    status: string; // "approved" | "pending"
    tags: mongoose.Types.ObjectId[];
    hashtags: string[];
    countReaction: number;
    countComment: number;
    countShare: number;
    createdAt: Date;
    updatedAt: Date; 
}

const PostSchema: Schema = new Schema<IPost>(
    {
        authorId: {
            type: Schema.Types.ObjectId,
            ref: "User",
            required: true,
        },
        groupId: {
            type: Schema.Types.ObjectId,
            ref: "Group",
            default: null
        },
        sharedPostId: { 
            type: Schema.Types.ObjectId, 
            ref: 'Post', 
            default: null 
        },
        content: {
            type: String,
            default: "",
        },
        privacy: {
            type: String,
            default: "Public",
            enum: ['Public', 'Private', 'Friends'],
        },
        feeling: { 
            type: String,
            default: "",
        },
        status: {
            type: String,
            default: "approved",
            enum: ['approved', 'pending'],
        },
        tags: [{ type: Schema.Types.ObjectId, ref: 'User' }],
        countReaction: {
            type: Number,
            default: 0
        },
        hashtags: {
            type: [String],
            default: []
        },
        countComment: {
            type: Number,
            default: 0
        },
        countShare: {
            type: Number,
            default: 0
        },        
    },
    {
        timestamps: true,
    }
)

PostSchema.index({ hashtags: 1, createdAt: -1 });

PostSchema.virtual('mediaFiles', {
    ref: 'Media',            
    localField: '_id',       
    foreignField: 'targetId' 
});

PostSchema.index({ authorId: 1 });
PostSchema.index({ groupId: 1 });
PostSchema.index({ createdAt: -1 });
PostSchema.index({ authorId: 1, createdAt: -1 });

const Post = mongoose.model<IPost>('Post', PostSchema);
export default Post;