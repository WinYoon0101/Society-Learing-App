import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";
import { JWT_SECRET } from "../config/env";
import User from "../models/user.model";

export interface AuthRequest extends Request {
  user?: {
    id: string;
    email: string;
    username: string;
  };
}

export const authenticate = async (
  req: AuthRequest,
  res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      res.status(401).json({
        success: false,
        message: "Không tìm thấy token xác thực. Vui lòng đăng nhập.",
      });
      return;
    }

    const token = authHeader.split(" ")[1];

    const decoded = jwt.verify(token, JWT_SECRET) as {
      id: string;
      email: string;
      username: string;
    };

    // Kiểm tra user vẫn tồn tại trong DB
    const user = await User.findById(decoded.id);
    if (!user || !user.isActive) {
      res.status(401).json({
        success: false,
        message: "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa.",
      });
      return;
    }

    req.user = {
      id: decoded.id,
      email: decoded.email,
      username: decoded.username,
    };

    next();
  } catch (error: any) {
    if (error.name === "TokenExpiredError") {
      res.status(401).json({
        success: false,
        message: "Token đã hết hạn. Vui lòng đăng nhập lại.",
        code: "TOKEN_EXPIRED",
      });
      return;
    }

    if (error.name === "JsonWebTokenError") {
      res.status(401).json({
        success: false,
        message: "Token không hợp lệ.",
        code: "INVALID_TOKEN",
      });
      return;
    }

    res.status(500).json({
      success: false,
      message: "Lỗi xác thực, vui lòng thử lại sau.",
    });
  }
};
