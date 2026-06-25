import express from 'express';
import { authenticate } from '../middlewares/auth.middleware';
import { getTrendingTopics, searchEverything } from '../controllers/search.controller';

const router = express.Router();

/**
 * @route   GET /api/search/trending
 * @desc    Lấy danh sách top 10 hashtag hot nhất trong 7 ngày qua
 * @access  Private (Yêu cầu đăng nhập)
 */
router.get('/trending', authenticate, getTrendingTopics);

/**
 * @route   GET /api/search/results
 * @desc    Tìm kiếm tổng hợp bài viết, người dùng, nhóm theo từ khóa hoặc hashtag
 * @access  Private (Yêu cầu đăng nhập)
 * @query   ?q=từ_khóa          (Tìm kiếm thông thường)
 * @query   ?hashtag=%23TenTag  (Tìm kiếm chính xác theo hashtag khi click chọn)
 */
router.get('/results', authenticate, searchEverything);

export default router;