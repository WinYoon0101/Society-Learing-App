import { Response } from "express";
import { AuthRequest } from "../middlewares/auth.middleware";
import User from "../models/user.model";
import Friend from "../models/friend.model";
import Group from "../models/group.model";

export const updateUser = async (
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

    // Nhận file upload từ multer (field name: "file")
    // multer-storage-cloudinary tự động upload lên Cloudinary và gắn URL vào req.file.path
    const file = req.file as Express.Multer.File & { path?: string };

    if (!file || !file.path) {
      res.status(400).json({
        success: false,
        message: "Vui lòng upload file ảnh.",
      });
      return;
    }

    const avatarUrl = file.path; // Cloudinary URL

    const user = await User.findByIdAndUpdate(
      userId,
      { avatar: avatarUrl },
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

export const updateProfile = async (
  req: AuthRequest,
  res: Response,
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { bio, hometown, location, dateOfBirth, gender } = req.body;

    if (!bio && !hometown && !location && !dateOfBirth && !gender) {
      res.status(400).json({
        success: false,
        message: "Phải có ít nhất một thông tin để cập nhật.",
      });
      return;
    }

    const user = await User.findByIdAndUpdate(
      userId,
      {
        ...(bio !== undefined && { bio }),
        ...(hometown !== undefined && { hometown }),
        ...(location !== undefined && { location }),
        ...(dateOfBirth !== undefined && { dateOfBirth }),
        ...(gender !== undefined && { gender }),
      },
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
      message: "Cập nhật profile thành công!",
      data: {
        bio: user.bio,
        hometown: user.hometown,
        location: user.location,
        dateOfBirth: user.dateOfBirth,
        gender: user.gender,
      },
    });
  } catch (error) {
    console.error("updateProfile error:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi server.",
    });
  }
};

export const updateCover = async (
  req: AuthRequest,
  res: Response,
): Promise<void> => {
  try {
    const userId = req.user!.id;

    // Nhận file upload từ multer (field name: "file")
    const file = req.file as Express.Multer.File & { path?: string };

    if (!file || !file.path) {
      res.status(400).json({
        success: false,
        message: "Vui lòng upload file ảnh.",
      });
      return;
    }

    const coverUrl = file.path; // Cloudinary URL

    const user = await User.findByIdAndUpdate(userId, { cover: coverUrl }, { new: true });

    if (!user) {
      res.status(404).json({
        success: false,
        message: "Không tìm thấy user.",
      });
      return;
    }

    res.status(200).json({
      success: true,
      message: "Cập nhật cover thành công!",
      data: {
        cover: user.cover,
      },
    });
  } catch (error) {
    console.error("updateCover error:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi server.",
    });
  }
};

export const getMyProfile = async (
  req: AuthRequest,
  res: Response,
): Promise<void> => {
  try {
    const userId = req.user!.id;

    const user = await User.findById(userId);

    if (!user) {
      res.status(404).json({
        success: false,
        message: "Không tìm thấy user",
      });
      return;
    }

    const friendCount = await Friend.countDocuments({
      $or: [{ requester: userId }, { recipient: userId }],
      status: "accepted",
    });

    const groupCount = await Group.countDocuments({
      "member.userId": userId,
    });

    res.status(200).json({
      success: true,
      data: {
        id: user._id,
        username: user.username,
        email: user.email,
        bio: user.bio,
        avatar: user.avatar,
        cover: user.cover,
        hometown: user.hometown,
        location: user.location,
        dateOfBirth: user.dateOfBirth,
        gender: user.gender,
        friendCount,
        groupCount,
      },
    });
  } catch (error) {
    console.error("getMyProfile error:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi server",
    });
  }
};
