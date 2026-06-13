import { Router } from "express";
import * as notificationController from "../controllers/notification.controller";
import { authenticate } from "../middlewares/auth.middleware";

const router = Router();

// Lấy danh sách thông báo
router.get("/", authenticate, notificationController.getNotifications);

// Đánh dấu một thông báo là đã đọc
router.put("/:id/read", authenticate, notificationController.markAsRead);

// Đánh dấu tất cả là đã đọc
router.put("/read-all", authenticate, notificationController.markAllAsRead);

// Xóa thông báo
router.delete("/:id", authenticate, notificationController.deleteNotification);

export default router;
