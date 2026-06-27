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
    const { username, bio, hometown, location, dateOfBirth, gender } = req.body;

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
        ...(username !== undefined && { username }),
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
        username: user.username,
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

    const user = await User.findByIdAndUpdate(
      userId,
      { cover: coverUrl },
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

// GET /api/user/search?q=keyword  – tìm kiếm người dùng theo tên
export const searchUsers = async (
  req: AuthRequest,
  res: Response,
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const q = ((req.query.q as string) || "").trim();
    if (!q) {
      res.status(200).json({ success: true, data: [] });
      return;
    }
    const users = await User.find({
      _id: { $ne: userId },
      username: { $regex: q, $options: "i" },
    })
      .select("_id username avatar")
      .limit(20)
      .lean();
    res.status(200).json({ success: true, data: users });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi tìm kiếm" });
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

    // Chỉ đếm bạn bè mà tài khoản còn tồn tại (tránh đếm user đã bị xóa)
    const friendships = await Friend.find({
      $or: [{ requester: userId }, { recipient: userId }],
      status: "accepted",
    })
      .populate("requester", "_id")
      .populate("recipient", "_id")
      .lean();

    const friendCount = friendships.filter(
      (f: any) => f.requester && f.recipient,
    ).length;

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

export const getUserById = async (req: AuthRequest, res: Response) => {
  try {
    const user = await User.findById(req.params.id).select("-password");

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy người dùng",
      });
    }

    res.json({
      success: true,
      data: user,
    });
  } catch (err) {
    res.status(500).json({
      success: false,
    });
  }
};

export const deleteAvatar = async (req: AuthRequest, res: Response) => {
  const userId = req.user!.id;
  const user = await User.findById(userId);
  if (!user) {
    return res.status(404).json({
      success: false,
      message: "User not found",
    });
  }
  user.avatar = null;
  await user.save();
  console.log("Avatar sau khi lưu:", user.avatar);
  res.json({
    success: true,
  });
};

export const deleteCover = async (req: AuthRequest, res: Response) => {
  const userId = req.user!.id;
  const user = await User.findById(userId);
  if (!user) {
    return res.status(404).json({
      success: false,
      message: "User not found",
    });
  }
  user.cover = null;
  await user.save();
  res.json({
    success: true,
  });
};
