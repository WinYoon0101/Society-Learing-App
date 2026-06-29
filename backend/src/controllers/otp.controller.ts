import { Request, Response } from "express";
import bcrypt from "bcrypt";
import Otp from "../models/otp.model";
import User from "../models/user.model";
import { sendOtpEmail } from "../utils/mailer";

// gửi OTP
export const sendOtp = async (req: Request, res: Response) => {
  try {
    const { email } = req.body;

    if (!email || typeof email !== "string") {
      return res.status(400).json({ success: false, message: "Email là bắt buộc" });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const user = await User.findOne({ email: normalizedEmail });
    if (!user) {
      return res.status(404).json({ success: false, message: "Email không tồn tại trong hệ thống" });
    }

    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    await Otp.deleteMany({ email: normalizedEmail });

    await Otp.create({
      email: normalizedEmail,
      otp,
      expiresAt: new Date(Date.now() + 5 * 60 * 1000), // 5 phút
    });

    sendOtpEmail(normalizedEmail, otp).catch((mailError) => {
  console.error("Lỗi gửi mail chạy ngầm:", mailError);
});

// Trả về kết quả ngay lập tức cho client
return res.json({ success: true, message: "OTP đã gửi" });


  } catch (error) {
    console.error("Lỗi gửi OTP:", error);
    return res.status(500).json({
      success: false,
      message: "Không gửi được OTP. Kiểm tra cấu hình email trên server.",
    });
  }
};

// verify OTP
export const verifyOtp = async (req: Request, res: Response) => {
  try {
    const { email, otp } = req.body;

    if (!email || !otp) {
      return res.status(400).json({ success: false, message: "Email và OTP là bắt buộc" });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const record = await Otp.findOne({ email: normalizedEmail, otp: String(otp).trim() });

    if (!record || record.expiresAt < new Date()) {
      return res.status(400).json({ success: false, message: "OTP sai hoặc hết hạn" });
    }

    return res.json({ success: true, message: "Xác thực OTP thành công" });
  } catch (error) {
    console.error("Lỗi xác thực OTP:", error);
    return res.status(500).json({ success: false, message: "Lỗi hệ thống khi xác thực OTP" });
  }
};

// reset password
export const resetPassword = async (req: Request, res: Response) => {
  try {
    const { email, newPassword } = req.body;

    if (!email || !newPassword) {
      return res.status(400).json({ success: false, message: "Email và mật khẩu mới là bắt buộc" });
    }

    if (String(newPassword).length < 6) {
      return res.status(400).json({ success: false, message: "Mật khẩu phải có ít nhất 6 ký tự" });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const user = await User.findOne({ email: normalizedEmail }).select("+password");

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy user",
      });
    }

  user.password = newPassword; 
    await user.save();

    await Otp.deleteMany({ email: normalizedEmail });

    return res.json({
      success: true,
      message: "Đổi mật khẩu thành công",
    });
  } catch (error) {
    console.error("Lỗi reset password:", error);
    return res.status(500).json({ success: false, message: "Lỗi hệ thống khi đổi mật khẩu" });
  }
};
