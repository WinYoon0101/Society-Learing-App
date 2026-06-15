import { Response } from "express";
import mongoose from "mongoose";
import { AuthRequest } from "../middlewares/auth.middleware";
import Friend from "../models/friend.model";
import User from "../models/user.model";
import Notification from "../models/notification.model";


// Hàm hỗ trợ tạo thông báo nhanh
const createFriendNotification = async (
    senderId: string, 
    recipientId: string, 
    type: string, 
    content: string, 
    targetId: string
) => {
    try {
        const newNotification = new Notification({
            recipient: recipientId,
            sender: senderId,
            targetId: targetId,
            type: type,
            content: content
        });
        await newNotification.save();
    } catch (error) {
        console.error("Lỗi tạo thông báo:", error);
    }
};

// 1. Gửi lời mời kết bạn
export const sendFriendRequest = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    // 1. Lấy và ép kiểu ID an toàn
    const userA = req.user?.id as string; 
    const idParam = req.params.id;
    const userB = Array.isArray(idParam) ? idParam[0] : idParam; // Xử lý nếu là mảng

    if (!userA || !userB) {
      res.status(400).json({ success: false, message: "Thiếu thông tin người dùng." });
      return;
    }

    if (userA === userB) {
      res.status(400).json({ success: false, message: "Bạn không thể tự gửi lời mời kết bạn cho chính mình." });
      return;
    }

    const sender = await User.findById(userA); 
    const recipient = await User.findById(userB);
    
    if (!recipient) {
      res.status(404).json({ success: false, message: "Người dùng không tồn tại." });
      return;
    }

    const senderName = sender?.username || "Người dùng";

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
      
      if (existingFriendship.status === "declined") {
        existingFriendship.requester = userA as any;
        existingFriendship.recipient = userB as any;
        existingFriendship.status = "pending";
        await existingFriendship.save();

        // TẠO THÔNG BÁO GỬI LẠI LỜI MỜI 
        await createFriendNotification(
            userA, 
            userB, 
            "friend_request", 
            `${senderName} đã gửi lại lời mời kết bạn cho bạn`, 
            existingFriendship._id.toString() 
        );

        res.status(200).json({ 
          success: true, 
          message: "Đã gửi lại lời mời kết bạn.",
          data: existingFriendship 
        });
        return;
      }
    }

    const newRequest = new Friend({
      requester: userA,
      recipient: userB,
      status: "pending",
    });

    await newRequest.save();

    // TẠO THÔNG BÁO GỬI LỜI MỜI MỚI 
    await createFriendNotification(
        userA, 
        userB, 
        "friend_request", 
        `${senderName} đã gửi cho bạn một lời mời kết bạn`, 
        newRequest._id.toString()
    );

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
    const userA = req.params.id; // Người gửi lời mời (người nhận thông báo)
    const userB = req.user?.id;  // Người nhận lời mời (người chấp nhận - là mình)

    // 1. Tìm lời mời kết bạn
    const request = await Friend.findOne({
      requester: userA,
      recipient: userB,
      status: "pending",
    });

    if (!request) {
      res.status(404).json({ success: false, message: "Không tìm thấy lời mời kết bạn này." });
      return;
    }

    // 2. Cập nhật trạng thái
    request.status = "accepted";
    await request.save();

    // 3. Lấy tên người chấp nhận (là mình - userB) để tạo thông báo cá nhân hóa
    const me = await User.findById(userB);
    const myName = me?.username || "Người dùng";

    // 4. TẠO THÔNG BÁO ĐỒNG Ý KẾT BẠN
    // Gọi hàm hỗ trợ createFriendNotification (đã tạo ở bước trước)
    await createFriendNotification(
        userB as string,            // sender là mình (userB)
        userA as string,            // recipient là người gửi lời mời (userA)
        "friend_accept", 
        `${myName} đã chấp nhận lời mời kết bạn của bạn`, 
        request._id.toString()
    );

    res.status(200).json({
      success: true,
      message: "Đã chấp nhận lời mời kết bạn.",
    });
  } catch (error) {
    console.error("Lỗi acceptFriendRequest:", error);
    res.status(500).json({ success: false, message: "Lỗi server", error });
  }
};

// 3. Từ chối lời mời kết bạn/ Hủy lời mời đã gửi
export const declineFriendRequest = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const me = req.user?.id;
    const otherUser = req.params.id;

    // FIX: Thay findOneAndDelete bằng findOneAndUpdate
    const request = await Friend.findOneAndUpdate(
      {
        $or: [
          { requester: me, recipient: otherUser, status: "pending" },
          { requester: otherUser, recipient: me, status: "pending" }
        ]
      },
      { status: "declined" }, // Chuyển sang từ chối thay vì xóa sạch
      { new: true }
    );

    if (!request) {
      res.status(404).json({ success: false, message: "Không tìm thấy lời mời để xử lý." });
      return;
    }

    res.status(200).json({ success: true, message: "Đã từ chối/hủy lời mời kết bạn." });
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

    const friends = friendships
      .map((f: any) => {
        // Bỏ qua nếu populate trả null (user bị xóa)
        if (!f.requester || !f.recipient) return null;
        return f.requester._id.toString() === userId ? f.recipient : f.requester;
      })
      .filter(Boolean); // Loại bỏ null

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

        // FIX: Nếu tài khoản người gửi đã bị xóa, reqItem.requester sẽ null -> Bỏ qua
        if (!reqItem.requester) return null;


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


    // Lọc bỏ các giá trị null (những lời mời từ user đã bị xóa)
    const validRequests = requestsWithMutual.filter(Boolean);

    res.status(200).json({
      success: true,
      data: validRequests,
    });
  } catch (error) {
    console.error("Lỗi getPendingRequests:", error); // Thêm log để dễ debug backend

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
    { $eq: [{ $arrayElemAt: ["$connection.status", 0] }, "accepted"] },
    // CHỈ LOẠI BỎ nếu trạng thái là 'declined' VÀ mình là người nhận
    { $and: [
      { $eq: [{ $arrayElemAt: ["$connection.status", 0] }, "declined"] },
      { $eq: [{ $arrayElemAt: ["$connection.recipient", 0] }, userObjectId] }
    ]},
    { $and: [
      { $eq: [{ $arrayElemAt: ["$connection.status", 0] }, "pending"] },
      { $eq: [{ $arrayElemAt: ["$connection.requester", 0] }, "$_id"] }
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
