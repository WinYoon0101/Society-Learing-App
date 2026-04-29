import { Request, Response } from "express";
import Otp from "../models/otp.model";
import User from "../models/user.model";
import { sendOtpEmail } from "../utils/mailer";

// gửi OTP
export const sendOtp = async (req: Request, res: Response) => {
  const { email } = req.body;

  const user = await User.findOne({ email });
  if (!user) {
    return res.json({ success: false, message: "Email không tồn tại" });
  }

  const otp = Math.floor(100000 + Math.random() * 900000).toString();

  await Otp.create({
    email,
    otp,
    expiresAt: new Date(Date.now() + 5 * 60 * 1000), // 5 phút
  });

  await sendOtpEmail(email, otp);

  res.json({ success: true, message: "OTP đã gửi" });
};

// verify OTP
export const verifyOtp = async (req: Request, res: Response) => {
  const { email, otp } = req.body;

  const record = await Otp.findOne({ email, otp });

  if (!record || record.expiresAt < new Date()) {
    return res.json({ success: false, message: "OTP sai hoặc hết hạn" });
  }

  res.json({ success: true });
};

// reset password
import bcrypt from "bcrypt";

// reset password
export const resetPassword = async (req: Request, res: Response) => {
  const { email, newPassword } = req.body;

  const user = await User.findOne({ email }).select("+password");

  if (!user) {
    return res.json({
      success: false,
      message: "Không tìm thấy user"
    });
  }

  // hash password
  const hashedPassword = await bcrypt.hash(newPassword, 10);

  user.password = hashedPassword;
  await user.save();

  await Otp.deleteMany({ email });

  res.json({
    success: true,
    message: "Đổi mật khẩu thành công"
  });
};