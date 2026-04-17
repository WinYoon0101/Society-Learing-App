import mongoose, {Document, Schema} from "mongoose";

export type GroupPrivacy ="private"| "public";
export type MemberRole = "admin"| "member";

export interface IGroupMember{
    userId: mongoose.Types.ObjectId;
    role: MemberRole;
    joinAt: Date;
}

export interface IGroup extends Document{
    _id: mongoose.Types.ObjectId;
    groupName: String;
    description: String;
    avatarUrl: String;
    coverUrl: String;
    creatorId: mongoose.Types.ObjectId;
    member: IGroupMember[];
    privacy: GroupPrivacy;
    createAt: Date;
    updateAt: Date;
}

const GroupMemberSchema = new Schema<IGroupMember>({
    userId: {
        type: Schema.Types.ObjectId,
        ref: "User",
        required: true,
    },
    role:{
        type: String,
        enum: ["admin", "member"],
        joinAt: Date,
        require: true,
    },
    joinAt:{
        type: Date,
        default: Date.now,
    }, 
},
 { _id: false,}
);

const GroupSchema = new Schema<IGroup>(
{
    groupName:{
        type: String,
        require: true,
        trim: true,
    },
    description:{
        type: String,
        default: '',
    },
    avatarUrl:{
        type: String,
        default:'',
    },
    coverUrl:{
        type: String,
        default:'',
    },
    creatorId:{
        type: Schema.Types.ObjectId,
        ref: "User",
        require: true,
    },
    member:[GroupMemberSchema],
    privacy:{
        type: String,
        enum: ["private", "public"],
        require: true,
    },
},
{timestamps:true},
)

GroupSchema.index({ groupName: 'text' });
// 2. Phục vụ màn hình "Nhóm do bạn quản lý"
GroupSchema.index({ creatorId: 1 });
// 3. Phục vụ màn hình "Nhóm bạn đã tham gia": 
GroupSchema.index({ 'members.userId': 1 });
export default mongoose.model<IGroup>('Group', GroupSchema);