import { Request, Response } from "express";
import jwt from "jsonwebtoken";
import { OAuth2Client } from "google-auth-library";
import User from "../models/user.model";
import {
  JWT_SECRET,
  JWT_REFRESH_SECRET,
  JWT_EXPIRES_IN,
  JWT_REFRESH_EXPIRES_IN,
} from "../config/env";
import { AuthRequest } from "../middlewares/auth.middleware";

// ===== GOOGLE CLIENT =====
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID || "";
const client = new OAuth2Client(GOOGLE_CLIENT_ID);

// ===== TOKEN =====
const generateTokens = (payload: any) => {
  const accessToken = jwt.sign(payload, JWT_SECRET, {
    expiresIn: JWT_EXPIRES_IN as any,
  });

  const refreshToken = jwt.sign(payload, JWT_REFRESH_SECRET, {
    expiresIn: JWT_REFRESH_EXPIRES_IN as any,
  });

  return { accessToken, refreshToken };
};

const sanitizeUser = (user: any) => ({
  id: user._id,
  username: user.username,
  email: user.email,
  avatar: user.avatar,
  isVerified: user.isVerified,
});

export const googleLogin = async (req: Request, res: Response) => {
  try {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({
        success: false,
        message: "Missing idToken",
      });
    }

    const ticket = await client.verifyIdToken({
      idToken,
      audience: GOOGLE_CLIENT_ID,
    });

    const payload = ticket.getPayload();

    if (!payload || !payload.email) {
      return res.status(401).json({
        success: false,
        message: "Invalid Google token",
      });
    }

    const { email, name, picture } = payload;

    let user = await User.findOne({ email: email.toLowerCase() });

    if (!user) {
      user = await User.create({
        email: email.toLowerCase(),
        username: name,
        avatar: picture,
        isVerified: true,
      });
    }

    const { accessToken, refreshToken } = generateTokens({
      id: user._id,
      email: user.email,
      username: user.username,
    });

    await User.updateOne({ _id: user._id }, { refreshToken });

    return res.json({
      success: true,
      message: "Google login success",
      data: {
        user: sanitizeUser(user),
        accessToken,
        refreshToken,
      },
    });
  } catch (error) {
    console.error("Google login error:", error);
    return res.status(500).json({
        success: false,
        message: error instanceof Error ? error.message : "Google login failed", // Trả về lỗi thật để debug
    });
  }
};

/**
 * @route   POST /api/auth/register
 */
export const register = async (req: Request, res: Response): Promise<void> => {
  try {
    const { username, email, password, dateOfBirth, gender } = req.body;
    if (!password) {
      res.status(400).json({
        success: false,
        message: "Mật khẩu là bắt buộc"
      });
      return;
    }
    const existingEmail = await User.findOne({ email: email.toLowerCase() });
    if (existingEmail) {
      res.status(409).json({ success: false, message: "Email này đã được sử dụng." });
      return;
    }

    const user = await User.create({
      username,
      email: email.toLowerCase(),
      password,
      dateOfBirth,
      gender,
    });

    const { accessToken, refreshToken } = generateTokens({
      id: user._id.toString(),
      email: user.email,
      username: user.username,
    });

    await User.updateOne({ _id: user._id }, { refreshToken });

    res.status(201).json({
      success: true,
      message: "Đăng ký thành công!",
      data: {
        user: sanitizeUser(user),
        accessToken,
        refreshToken,
      },
    });
  } catch (error: any) {
    console.error("Register error:", error);
    res.status(500).json({ success: false, message: "Lỗi đăng ký tài khoản." });
  }
};

/**
 * @route   POST /api/auth/login
 */
export const login = async (req: Request, res: Response): Promise<void> => {
  try {
    const { email, password } = req.body;

    const user = await User.findOne({ email: email.toLowerCase() }).select("+password +refreshToken");

    if (!user || !(await user.comparePassword(password))) {
      res.status(401).json({ success: false, message: "Email hoặc mật khẩu không chính xác." });
      return;
    }

    if (!user.isActive) {
      res.status(403).json({ success: false, message: "Tài khoản đã bị vô hiệu hóa." });
      return;
    }

    const { accessToken, refreshToken } = generateTokens({
      id: user._id.toString(),
      email: user.email,
      username: user.username,
    });

    await User.updateOne({ _id: user._id }, { refreshToken });

    res.status(200).json({
      success: true,
      message: "Đăng nhập thành công!",
      data: {
        user: sanitizeUser(user),
        accessToken,
        refreshToken,
      },
    });
  } catch (error: any) {
    console.error("Login error:", error);
    res.status(500).json({ success: false, message: "Lỗi đăng nhập." });
  }
};

/**
 * @route   POST /api/auth/refresh-token
 */
export const refreshToken = async (req: Request, res: Response): Promise<void> => {
  try {
    const { refreshToken: token } = req.body;
    if (!token) {
      res.status(400).json({ success: false, message: "Refresh token là bắt buộc." });
      return;
    }

    let decoded: any;
    try {
      decoded = jwt.verify(token, JWT_REFRESH_SECRET);
    } catch {
      res.status(401).json({ success: false, message: "Token hết hạn.", code: "INVALID_REFRESH_TOKEN" });
      return;
    }

    const user = await User.findById(decoded.id).select("+refreshToken");
    if (!user || user.refreshToken !== token) {
      res.status(401).json({ success: false, message: "Token không hợp lệ." });
      return;
    }

    const { accessToken, refreshToken: newRefreshToken } = generateTokens({
      id: user._id.toString(),
      email: user.email,
      username: user.username,
    });

    await User.updateOne({ _id: user._id }, { refreshToken: newRefreshToken });

    res.status(200).json({
      success: true,
      data: { accessToken, refreshToken: newRefreshToken },
    });
  } catch (error: any) {
    res.status(500).json({ success: false, message: "Lỗi làm mới token." });
  }
};

/**
 * @route   POST /api/auth/logout
 */
export const logout = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    await User.findByIdAndUpdate(req.user?.id, { refreshToken: null });
    res.status(200).json({ success: true, message: "Đăng xuất thành công!" });
  } catch (error: any) {
    res.status(500).json({ success: false, message: "Lỗi đăng xuất." });
  }
};

/**
 * @route   GET /api/auth/me
 */
export const getMe = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const user = await User.findById(req.user?.id);
    if (!user) {
      res.status(404).json({ success: false, message: "Không tìm thấy user." });
      return;
    }
    res.status(200).json({ success: true, data: { user: sanitizeUser(user) } });
  } catch (error: any) {
    res.status(500).json({ success: false, message: "Lỗi lấy thông tin user." });
  }
};