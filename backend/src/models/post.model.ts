import mongoose, {Document, Schema} from "mongoose"
import User from "./user.model";

export interface IPost extends Document{
    _id: mongoose.Types.ObjectId;
    authorId: mongoose.Types.ObjectId;
    groupId: mongoose.Types.ObjectId;
    sharedPostId?: mongoose.Types.ObjectId;
    content: String;
    mediaUrl: String[];
    mediaType: String;
    privacy: String;
    countReaction: Number;
    countComment: Number;
    countShare: Number;
    createAt: Date;
    upDateAt: Date;
}

const PostSchema: Schema = new Schema<IPost>(
    {
        authorId:{
           type: Schema.Types.ObjectId,
           ref: "User",
           required: true,
        },

        groupId:{
            type: Schema.Types.ObjectId,
            ref: "Group",
            default: "null"
        },
        sharedPostId: { type: Schema.Types.ObjectId, 
            ref: 'Post', 
            default: null 
        },
        content:{
            type: String,
            default: "",
        },
        mediaType: {
            type : String,
            enum : ["image", "video", "document"],
        },
        mediaUrl:[{
            type: String,
        }],
        privacy:{
            type: String,
            default: "Public",
            enum: ['Public', 'Private','Friends'],
        },
        countReaction:{
            type: Number,
        },
        countComment:{
            type: Number,
        },
        countShare:{
            type: Number,
        },        
    },
    {
            timestamps: true,
    }
)
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

