import { Response } from "express";
import mongoose from "mongoose";
import { AuthRequest } from "../middlewares/auth.middleware";
import User from "../models/user.model";

export const updateProfile = async (
  req: AuthRequest,
  res: Response,
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { username, bio } = req.body;

    const user = await User.findByIdAndUpdate(
      userId,
      { username, bio },
      { new: true },
    );

    res.status(200).json({
      success: true,
      message: "Cập nhật thông tin thành công!",
      data: {
        user: {
          id: user!._id,
          username: user!.username,
          email: user!.email,
          bio: user!.bio,
          avatar: user!.avatar,
        },
      },
    });
  } catch (error) {
    res.status(500).json({ success: false });
  }
};

export const updateAvatar = async (
  req: AuthRequest,
  res: Response,
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { avatar } = req.body;

    if (!avatar) {
      res.status(400).json({
        success: false,
        message: "Avatar URL là bắt buộc.",
      });
      return;
    }

    const user = await User.findByIdAndUpdate(
      userId,
      { avatar },
      { new: true },
    );

    if (!user) {
      res.status(404).json({
        success: false,
        message: "Không tìm thấy user.",
      });
      return;
    }

    res.status(200).json({
      success: true,
      message: "Cập nhật avatar thành công!",
      data: {
        avatar: user.avatar,
      },
    });
  } catch (error) {
    console.error("updateAvatar error:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi server.",
    });
  }
};
