import { Response } from "express";
import { AuthRequest } from "../middlewares/auth.middleware";
import Friend from "../models/friend.model";
import User from "../models/user.model";

// 1. Gửi lời mời kết bạn
export const sendFriendRequest = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userA = req.user?.id; // Người gửi
    const userB = req.params.id; // Người nhận

    if (userA === userB) {
      res.status(400).json({
        success: false,
        message: "Bạn không thể tự gửi lời mời kết bạn cho chính mình.",
      });
      return;
    }

    const recipient = await User.findById(userB);
    if (!recipient) {
      res.status(404).json({ success: false, message: "Người dùng không tồn tại." });
      return;
    }

    // Kiểm tra xem đã có lời mời kết bạn nào giữa 2 người chưa (chiều đi hoặc chiều về)
    const existingFriendship = await Friend.findOne({
      $or: [
        { requester: userA, recipient: userB },
        { requester: userB, recipient: userA },
      ],
    });

    if (existingFriendship) {
      if (existingFriendship.status === "accepted") {
        res.status(400).json({ success: false, message: "Hai bạn đã là bạn bè." });
        return;
      }
      if (existingFriendship.status === "pending") {
        res.status(400).json({ success: false, message: "Đã tồn tại lời mời kết bạn đang chờ xử lý." });
        return;
      }
      // Nếu trạng thái là declined, có thể cho phép gửi lại bằng cách xoá record cũ và tạo mới hoặc update.
      // Ở đây ta update lại trạng thái thành pending và đặt người gửi là userA.
      if (existingFriendship.status === "declined") {
        existingFriendship.requester = userA as any;
        existingFriendship.recipient = userB as any;
        existingFriendship.status = "pending";
        await existingFriendship.save();
        res.status(200).json({ success: true, message: "Đã gửi lại lời mời kết bạn." });
        return;
      }
    }

    const newRequest = new Friend({
      requester: userA,
      recipient: userB,
      status: "pending",
    });

    await newRequest.save();

    res.status(201).json({
      success: true,
      message: "Đã gửi lời mời kết bạn thành công.",
      data: newRequest,
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};

// 2. Chấp nhận lời mời kết bạn
export const acceptFriendRequest = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userA = req.params.id; // Người gửi lời mời
    const userB = req.user?.id; // Người nhận (mình)

    const request = await Friend.findOne({
      requester: userA,
      recipient: userB,
      status: "pending",
    });

    if (!request) {
      res.status(404).json({ success: false, message: "Không tìm thấy lời mời kết bạn này." });
      return;
    }

    request.status = "accepted";
    await request.save();

    res.status(200).json({
      success: true,
      message: "Đã chấp nhận lời mời kết bạn.",
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};

// 3. Từ chối lời mời kết bạn
export const declineFriendRequest = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userA = req.params.id; // Người gửi
    const userB = req.user?.id; // Người nhận (mình)

    const request = await Friend.findOne({
      requester: userA,
      recipient: userB,
      status: "pending",
    });

    if (!request) {
      res.status(404).json({ success: false, message: "Không tìm thấy lời mời kết bạn này." });
      return;
    }

    // Ta có thể update status = 'declined' hoặc xoá record. Thường Facebook ẩn đi nên có thể xoá luôn cho gọn DB hoặc set declined.
    await Friend.findByIdAndDelete(request._id);

    res.status(200).json({
      success: true,
      message: "Đã từ chối lời mời kết bạn.",
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};

// 4. Huỷ kết bạn
export const removeFriend = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userA = req.user?.id;
    const userB = req.params.id; // Người muốn huỷ kết bạn

    const friendship = await Friend.findOneAndDelete({
      $or: [
        { requester: userA, recipient: userB, status: "accepted" },
        { requester: userB, recipient: userA, status: "accepted" },
      ],
    });

    if (!friendship) {
      res.status(404).json({ success: false, message: "Không tìm thấy thông tin bạn bè." });
      return;
    }

    res.status(200).json({
      success: true,
      message: "Đã huỷ kết bạn thành công.",
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};

// 5. Lấy danh sách bạn bè
export const getFriends = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user?.id;

    const friendships = await Friend.find({
      $or: [{ requester: userId }, { recipient: userId }],
      status: "accepted",
    })
      .populate("requester", "_id username email avatar")
      .populate("recipient", "_id username email avatar");

    const friends = friendships.map((f: any) => {
      // Lọc ra người kia
      if (f.requester._id.toString() === userId) {
        return f.recipient;
      } else {
        return f.requester;
      }
    });

    res.status(200).json({
      success: true,
      data: friends,
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};

// 6. Lấy danh sách lời mời kết bạn (chưa xử lý) dành cho mình
export const getPendingRequests = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user?.id;

    const requests = await Friend.find({
      recipient: userId,
      status: "pending",
    }).populate("requester", "_id username email avatar");

    res.status(200).json({
      success: true,
      data: requests,
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};
