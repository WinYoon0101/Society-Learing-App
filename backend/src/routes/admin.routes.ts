import express from 'express';
import {
    getDashboardStats,
    getAllUsersAdmin,
    getAllPostsAdmin,
    deletePostByAdmin,
    getAllReportsAdmin,       
    updateReportStatusAdmin
} from '../controllers/admin.controller';

const router = express.Router();


// 1. Thống kê Dashboard
router.get('/dashboard', getDashboardStats);

// 2. Quản lý Người dùng
router.get('/users', getAllUsersAdmin);

// 3. Quản lý Bài viết
router.get('/posts', getAllPostsAdmin);
router.delete('/posts/:id', deletePostByAdmin);

// 4. Quản lý Báo cáo vi phạm
router.get('/reports', getAllReportsAdmin); 
router.patch('/reports/:id/status', updateReportStatusAdmin); // Xử lý (đổi status)

export default router;