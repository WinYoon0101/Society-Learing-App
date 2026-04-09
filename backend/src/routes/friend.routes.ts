import { Router } from "express";
import { authenticate } from "../middlewares/auth.middleware";
import {
  sendFriendRequest,
  acceptFriendRequest,
  declineFriendRequest,
  removeFriend,
  getFriends,
  getPendingRequests,
} from "../controllers/friend.controller";

const router = Router();

// Middleware xác thực tất cả route bên dưới
router.use(authenticate);

// Các route cho danh sách và pending
router.get("/", getFriends); // Lấy danh sách bạn bè
router.get("/pending", getPendingRequests); // Lấy danh sách lời mời tới mình

// Các route action với ID (ID của user kia)
router.post("/request/:id", sendFriendRequest); // Gửi lời mời tới id
router.put("/accept/:id", acceptFriendRequest); // Chấp nhận lời mời từ id
router.delete("/decline/:id", declineFriendRequest); // Từ chối lời mời từ id
router.delete("/remove/:id", removeFriend); // Huỷ kết bạn với id

export default router;
