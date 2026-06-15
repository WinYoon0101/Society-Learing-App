import mongoose, { Document, Schema } from "mongoose"

export interface IPost extends Document {
    _id: mongoose.Types.ObjectId;
    authorId: mongoose.Types.ObjectId;
    groupId: mongoose.Types.ObjectId;
    sharedPostId?: mongoose.Types.ObjectId;
    content: string;
    privacy: string;
    tags: mongoose.Types.ObjectId[]; // 
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
        // Tagged users in the post
        tags: [{ type: Schema.Types.ObjectId, ref: 'User' }],
        countReaction: {
            type: Number,
            default: 0
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
        timestamps: true, // Tự tạo ra createdAt và updatedAt
    }
)

PostSchema.virtual('mediaFiles', {
    ref: 'Media',            // Chạy sang bảng Media để tìm
    localField: '_id',       // Dùng ID của bài Post này
    foreignField: 'targetId' // Tìm những dòng nào trong bảng Media có targetId khớp
});

// 1. Tối ưu khi load "Trang cá nhân" (Tìm bài viết theo người đăng)
PostSchema.index({ authorId: 1 });

// 2. Tối ưu khi load "Trang Nhóm" (Tìm bài viết theo nhóm)
PostSchema.index({ groupId: 1 });

// 3. Tối ưu khi load "Bảng tin" (Sắp xếp bài viết mới nhất lên đầu)
PostSchema.index({ createdAt: -1 });

// 4. Compound Index: Tối ưu khi vừa tìm theo người đăng, vừa sắp xếp giờ
PostSchema.index({ authorId: 1, createdAt: -1 });

const Post = mongoose.model<IPost>('Post', PostSchema);
export default Post;