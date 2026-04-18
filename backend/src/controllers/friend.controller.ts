import { Response } from "express";
import mongoose from "mongoose";
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

// 3. Từ chối lời mời kết bạn/ Hủy lời mời đã gửi
export const declineFriendRequest = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const me = req.user?.id;
    const otherUser = req.params.id;

    // Tìm và xóa bản ghi pending giữa 2 người (không quan trọng ai gửi)
    const request = await Friend.findOneAndDelete({
      $or: [
        { requester: me, recipient: otherUser, status: "pending" },
        { requester: otherUser, recipient: me, status: "pending" }
      ]
    });

    if (!request) {
      res.status(404).json({ success: false, message: "Không tìm thấy lời mời để xóa." });
      return;
    }

    res.status(200).json({ success: true, message: "Đã xóa/hủy lời mời kết bạn." });
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

    if (!userId) {
  res.status(401).json({ success: false, message: "Không tìm thấy thông tin xác thực." });
  return;
}

    const friendships = await Friend.find({
      $or: [{ requester: userId }, { recipient: userId }],
      status: "accepted",
    })
      .populate("requester", "_id username avatar")
      .populate("recipient", "_id username avatar");

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

    if (!userId) {
  res.status(401).json({ success: false, message: "Không tìm thấy thông tin xác thực." });
  return;
}

    // 1. Lấy danh sách lời mời
    const requests = await Friend.find({
      recipient: userId,
      status: "pending",
    }).populate("requester", "_id username email avatar");

    // 2. Lấy danh sách ID bạn bè của user hiện tại (để chuẩn bị so sánh bạn chung)
    const myFriends = await Friend.find({
      $or: [{ requester: userId }, { recipient: userId }],
      status: "accepted",
    });
    const myFriendIds = myFriends.map((f) =>
      f.requester.toString() === userId ? f.recipient.toString() : f.requester.toString()
    );

    // 3. Đếm bạn chung cho từng lời mời
    const requestsWithMutual = await Promise.all(
      requests.map(async (reqItem: any) => {
        const otherUserId = reqItem.requester._id.toString();

        // Lấy bạn bè của người kia
        const theirFriends = await Friend.find({
          $or: [{ requester: otherUserId }, { recipient: otherUserId }],
          status: "accepted",
        });
        const theirFriendIds = theirFriends.map((f) =>
          f.requester.toString() === otherUserId ? f.recipient.toString() : f.requester.toString()
        );

        // Giao của 2 mảng ID chính là số bạn chung
        const mutualCount = myFriendIds.filter((id) => theirFriendIds.includes(id)).length;

        // Trả về object mới có thêm trường mutualFriends
        return {
          _id: reqItem.requester._id, // Trả ID của người gửi để Mobile gọi API Chấp nhận
          username: reqItem.requester.username,
          email: reqItem.requester.email,
          avatar: reqItem.requester.avatar,
          mutualFriends: mutualCount,
        };
      })
    );

    res.status(200).json({
      success: true,
      data: requestsWithMutual,
    });
  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};

export const checkFriendStatus = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const me = req.user?.id;
    const otherUser = req.params.id;

    if (me === otherUser) {
      res.status(200).json({ success: true, status: "self" });
      return;
    }

    const friendship = await Friend.findOne({
      $or: [
        { requester: me, recipient: otherUser },
        { requester: otherUser, recipient: me },
      ],
    });

    if (!friendship) {
      res.status(200).json({ success: true, status: "none" }); // Chưa kết bạn -> Hiện nút "Thêm bạn bè"
      return;
    }

    if (friendship.status === "accepted") {
      res.status(200).json({ success: true, status: "friends" }); // Đã là bạn -> Hiện nút "Bạn bè / Hủy kết bạn"
      return;
    }

    if (friendship.status === "pending") {
      if (friendship.requester.toString() === me) {
        res.status(200).json({ success: true, status: "request_sent" }); // Mình gửi -> Hiện nút "Đã gửi lời mời / Hủy"
      } else {
        res.status(200).json({ success: true, status: "request_received" }); // Họ gửi -> Hiện nút "Chấp nhận / Từ chối"
      }
      return;
    }

  } catch (error) {
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};


export const getFriendSuggestions = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.user?.id;
    if (!userId) {
      res.status(401).json({ success: false, message: "Không tìm thấy user" });
      return;
    }
    const userObjectId = new mongoose.Types.ObjectId(userId);

    const suggestions = await User.aggregate([
      // 1. Loại chính mình
      { $match: { _id: { $ne: userObjectId } } },

      // 2. Tìm kết nối (lời mời/bạn bè) giữa 2 người
      {
        $lookup: {
          from: "friends",
          let: { candidateId: "$_id" },
          pipeline: [
            {
              $match: {
                $expr: {
                  $or: [
                    { $and: [{ $eq: ["$requester", userObjectId] }, { $eq: ["$recipient", "$$candidateId"] }] },
                    { $and: [{ $eq: ["$requester", "$$candidateId"] }, { $eq: ["$recipient", userObjectId] }] }
                  ]
                }
              }
            }
          ],
          as: "connection"
        }
      },

      // 3. Gắn nhãn isPending và Xác định đối tượng cần lọc bỏ
      {
        $addFields: {
          isPending: {
            $cond: [
              { $and: [
                { $gt: [{ $size: "$connection" }, 0] },
                { $eq: [{ $arrayElemAt: ["$connection.status", 0] }, "pending"] },
                { $eq: [{ $arrayElemAt: ["$connection.requester", 0] }, userObjectId] }
              ]},
              true, false
            ]
          },
          shouldExclude: {
            $or: [
              { $eq: [{ $arrayElemAt: ["$connection.status", 0] }, "accepted"] }, // Đã là bạn
              { $and: [
                { $eq: [{ $arrayElemAt: ["$connection.status", 0] }, "pending"] },
                { $eq: [{ $arrayElemAt: ["$connection.requester", 0] }, "$_id"] } // Họ gửi cho mình
              ]}
            ]
          }
        }
      },

      // 4. Lọc bỏ người đã là bạn hoặc người đã gửi lời mời cho mình
      { $match: { shouldExclude: false } },

      // 5. Lấy danh sách bạn bè của TÔI (để tính bạn chung)
      {
        $lookup: {
          from: "friends",
          let: { me: userObjectId },
          pipeline: [
            { $match: { $expr: { $and: [
              { $or: [{ $eq: ["$requester", "$$me"] }, { $eq: ["$recipient", "$$me"] }] },
              { $eq: ["$status", "accepted"] }
            ]}}},
            { $project: { friendId: { $cond: [{ $eq: ["$requester", "$$me"] }, "$recipient", "$requester"] } } }
          ],
          as: "myFriends"
        }
      },

      // 6. Lấy danh sách bạn bè của NGƯỜI ĐƯỢC GỢI Ý
      {
        $lookup: {
          from: "friends",
          let: { other: "$_id" },
          pipeline: [
            { $match: { $expr: { $and: [
              { $or: [{ $eq: ["$requester", "$$other"] }, { $eq: ["$recipient", "$$other"] }] },
              { $eq: ["$status", "accepted"] }
            ]}}},
            { $project: { friendId: { $cond: [{ $eq: ["$requester", "$$other"] }, "$recipient", "$requester"] } } }
          ],
          as: "theirFriends"
        }
      },

      // 7. Tính Mutual Friends
      {
        $addFields: {
          mutualFriends: {
            $size: { $setIntersection: ["$myFriends.friendId", "$theirFriends.friendId"] }
          }
        }
      },

      // 8. Trả kết quả về Android
      { $sort: { mutualFriends: -1 } },
      { $limit: 15 },
      {
        $project: {
          _id: 1,
          username: 1,
          avatar: 1,
          mutualFriends: 1,
          isPending: 1
        }
      }
    ]);

    res.status(200).json({ success: true, data: suggestions });
  } catch (error: any) {
    console.error("Aggregation Error:", error.message);
    res.status(500).json({ success: false, message: "Lỗi server", error: error.message });
  }
};
