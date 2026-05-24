import { Router } from "express";
import { authenticate } from "../middlewares/auth.middleware";
import {
  getNotifications,
  markAsRead,
  markAllAsRead,
} from "../controllers/notification.controller";

const router = Router();

// Middleware xác thực tất cả route thông báo
router.use(authenticate);

// API Lấy danh sách thông báo (GET /api/notifications)
router.get("/", getNotifications);

// API Đánh dấu đã đọc tất cả (PUT /api/notifications/mark-all-read)
router.put("/mark-all-read", markAllAsRead);

// API Đánh dấu 1 thông báo là đã đọc (PUT /api/notifications/:id/read)
router.put("/:id/read", markAsRead);

export default router;