import { Request, Response } from "express";
import jwt from "jsonwebtoken";
import User from "../models/user.model";
import {
  JWT_SECRET,
  JWT_REFRESH_SECRET,
  JWT_EXPIRES_IN,
  JWT_REFRESH_EXPIRES_IN,
} from "../config/env";
import { AuthRequest } from "../middlewares/auth.middleware";

// ─── Helpers ────────────────────────────────────────────────────────────────

const generateTokens = (payload: {
  id: string;
  email: string;
  username: string;
}) => {
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
  dateOfBirth: user.dateOfBirth,
  gender: user.gender,
  avatar: user.avatar,
  bio: user.bio,
  isVerified: user.isVerified,
  createdAt: user.createdAt,
});

// ─── Controllers ─────────────────────────────────────────────────────────────

/**
 * @route   POST /api/auth/register
 * @access  Public
 */
export const register = async (req: Request, res: Response): Promise<void> => {
  try {
    const { username, email, password, dateOfBirth, gender } = req.body;

    // Kiểm tra email đã tồn tại chưa
    const existingEmail = await User.findOne({ email: email.toLowerCase() });
    if (existingEmail) {
      res.status(409).json({
        success: false,
        message: "Email này đã được sử dụng. Vui lòng dùng email khác.",
      });
      return;
    }

    // Tạo user mới
    const user = await User.create({
      username,
      email: email.toLowerCase(),
      password,
      dateOfBirth,
      gender,
    });

    // Sinh tokens
    const { accessToken, refreshToken } = generateTokens({
      id: user._id.toString(),
      email: user.email,
      username: user.username,
    });

    // Lưu refresh token vào DB
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
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   POST /api/auth/login
 * @access  Public
 */
export const login = async (req: Request, res: Response): Promise<void> => {
  try {
    const { email, password } = req.body;

    // Tìm user và lấy cả password (vì select: false)
    const user = await User.findOne({ email: email.toLowerCase() }).select(
      "+password +refreshToken"
    );

    if (!user) {
      res.status(401).json({
        success: false,
        message: "Email hoặc mật khẩu không chính xác.",
      });
      return;
    }

    // Kiểm tra tài khoản có bị khóa không
    if (!user.isActive) {
      res.status(403).json({
        success: false,
        message: "Tài khoản của bạn đã bị vô hiệu hóa.",
      });
      return;
    }

    // Kiểm tra password
    const isPasswordValid = await user.comparePassword(password);
    if (!isPasswordValid) {
      res.status(401).json({
        success: false,
        message: "Email hoặc mật khẩu không chính xác.",
      });
      return;
    }

    // Sinh tokens mới
    const { accessToken, refreshToken } = generateTokens({
      id: user._id.toString(),
      email: user.email,
      username: user.username,
    });

    // Cập nhật refresh token trong DB
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
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   POST /api/auth/refresh-token
 * @access  Public
 */
export const refreshToken = async (
  req: Request,
  res: Response
): Promise<void> => {
  try {
    const { refreshToken: token } = req.body;

    if (!token) {
      res.status(400).json({
        success: false,
        message: "Refresh token là bắt buộc.",
      });
      return;
    }

    // Verify refresh token
    let decoded: any;
    try {
      decoded = jwt.verify(token, JWT_REFRESH_SECRET);
    } catch {
      res.status(401).json({
        success: false,
        message: "Refresh token không hợp lệ hoặc đã hết hạn.",
        code: "INVALID_REFRESH_TOKEN",
      });
      return;
    }

    // Tìm user và so sánh refresh token
    const user = await User.findById(decoded.id).select("+refreshToken");
    if (!user || user.refreshToken !== token) {
      res.status(401).json({
        success: false,
        message: "Refresh token không hợp lệ.",
        code: "INVALID_REFRESH_TOKEN",
      });
      return;
    }

    if (!user.isActive) {
      res.status(403).json({
        success: false,
        message: "Tài khoản đã bị vô hiệu hóa.",
      });
      return;
    }

    // Sinh tokens mới
    const { accessToken, refreshToken: newRefreshToken } = generateTokens({
      id: user._id.toString(),
      email: user.email,
      username: user.username,
    });

    // Cập nhật refresh token mới (rotation)
    await User.updateOne({ _id: user._id }, { refreshToken: newRefreshToken });

    res.status(200).json({
      success: true,
      message: "Làm mới token thành công!",
      data: {
        accessToken,
        refreshToken: newRefreshToken,
      },
    });
  } catch (error: any) {
    console.error("Refresh token error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   POST /api/auth/logout
 * @access  Private
 */
export const logout = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user?.id;

    // Xóa refresh token trong DB
    await User.findByIdAndUpdate(userId, { refreshToken: null });

    res.status(200).json({
      success: true,
      message: "Đăng xuất thành công!",
    });
  } catch (error: any) {
    console.error("Logout error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   GET /api/auth/me
 * @access  Private
 */
export const getMe = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user?.id;

    const user = await User.findById(userId);
    if (!user) {
      res.status(404).json({
        success: false,
        message: "Không tìm thấy người dùng.",
      });
      return;
    }

    res.status(200).json({
      success: true,
      data: {
        user: sanitizeUser(user),
      },
    });
  } catch (error: any) {
    console.error("GetMe error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};
