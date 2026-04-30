import mongoose, { Document, Schema } from "mongoose";
import bcrypt from "bcryptjs";

export interface IUser extends Document {
  _id: mongoose.Types.ObjectId;
  username: string;
  email: string;
  password: string;
  dateOfBirth?: string;
  gender?: string;
  avatar?: string;
  profileCover?: string;
  bio?: string;
  isVerified: boolean;
  isActive: boolean;
  refreshToken?: string;
  savedDocument: mongoose.Types.ObjectId[];
  createdAt: Date;
  updatedAt: Date;
  comparePassword(candidatePassword: string): Promise<boolean>;
  hometown?: string;
  location?: string;
  cover?: string;
}

const UserSchema = new Schema<IUser>(
  {
    username: {
      type: String,
      required: [true, "Tên người dùng là bắt buộc"],
      trim: true,
      maxlength: [100, "Tên người dùng không được vượt quá 100 ký tự"],
    },
    email: {
      type: String,
      required: [true, "Email là bắt buộc"],
      unique: true,
      trim: true,
      lowercase: true,
      match: [/^\S+@\S+\.\S+$/, "Email không hợp lệ"],
    },
    password: {
      type: String,
      required: false, // Cho phép password có thể null (dành cho đăng ký bằng OAuth)
      minlength: [6, "Mật khẩu phải có ít nhất 6 ký tự"],
      select: false, // Không trả về password trong query
    },
    dateOfBirth: {
      type: String,
      default: null,
    },
    gender: {
      type: String,
      default: null,
    },
    avatar: {
      type: String,
      default: null,
    },
    profileCover: {
      type: String,
      default: null,
    },
    bio: {
      type: String,
      maxlength: [160, "Bio không được vượt quá 160 ký tự"],
      default: null,
    },
    isVerified: {
      type: Boolean,
      default: false,
    },
    isActive: {
      type: Boolean,
      default: true,
    },
    refreshToken: {
      type: String,
      default: null,
      select: false,
    },
    savedDocument: {
      type: [Schema.Types.ObjectId],
      ref: "Document",
      default: [],
    },
    hometown: {
      type: String,
      default: null,
    },
    location: {
      type: String,
      default: null,
    },
    cover: {
      type: String,
      default: null,
    },
  },
  {
    timestamps: true,
  },
);

// Hash password trước khi save
UserSchema.pre<IUser>("save", async function () {
  //Google login → không có password → bỏ qua
  if (!this.password) return;

  //Không đổi password → bỏ qua
  if (!this.isModified("password")) return;

  const salt = await bcrypt.genSalt(12);
  this.password = await bcrypt.hash(this.password, salt);
});

// Method so sánh password
UserSchema.methods.comparePassword = async function (
  candidatePassword: string,
): Promise<boolean> {
  return bcrypt.compare(candidatePassword, this.password);
};

const User = mongoose.model<IUser>("User", UserSchema);

export default User;
