import { Router } from "express";
import { body, param, query } from "express-validator";
import { authenticate } from "../middlewares/auth.middleware";
import { handleValidationErrors } from "../middlewares/validate.middleware"; 
import {
  createComment,
  getCommentsByPost,
  deleteComment,
  getReplies
} from "../controllers/comment.controller";

const router = Router();

// ─── Validators ────────────────────────────────────────────────────────────────

const createCommentValidators = [
  body("postId")
    .notEmpty()
    .withMessage("postId là bắt buộc")
    .isMongoId()
    .withMessage("postId không hợp lệ"),

  body("content")
    .trim()
    .notEmpty()
    .withMessage("Nội dung bình luận không được để trống")
    .isLength({ max: 500 })
    .withMessage("Bình luận không được vượt quá 500 ký tự"),

  body("parentId")
    .optional({ checkFalsy: true })
    .isMongoId()
    .withMessage("parentId không hợp lệ"),
];

const getCommentsValidators = [
  param("postId").isMongoId().withMessage("Post ID không hợp lệ"),
  query("page")
    .optional()
    .isInt({ min: 1 })
    .withMessage("page phải là số nguyên dương"),
  query("limit")
    .optional()
    .isInt({ min: 1, max: 50 })
    .withMessage("limit phải từ 1-50"),
];

const getRepliesValidators = [
  param("commentId").isMongoId().withMessage("Comment ID không hợp lệ"),
  query("page")
    .optional()
    .isInt({ min: 1 })
    .withMessage("page phải là số nguyên dương"),
  query("limit")
    .optional()
    .isInt({ min: 1, max: 50 })
    .withMessage("limit phải từ 1-50"),
];

const deleteCommentValidators = [
  param("id").isMongoId().withMessage("Comment ID không hợp lệ"),
];

// ─── Routes ────────────────────────────────────────────────────────────────────

// 👉 ĐÃ SỬA: Thêm authenticate vào route getCommentsByPost để Backend biết user nào đang xem
router.get(
  "/post/:postId",
  getCommentsValidators,
  handleValidationErrors,
  authenticate, 
  getCommentsByPost
);

// 👉 ĐÃ SỬA: Thêm authenticate vào route getReplies để Backend biết user nào đang xem
router.get(
  "/replies/:commentId",
  getRepliesValidators,
  handleValidationErrors,
  authenticate,
  getReplies
);

// Bật hàng rào bảo vệ: Các chức năng dưới đây bắt buộc phải đăng nhập
router.use(authenticate);

// 2. Viết comment mới
router.post(
  "/",
  createCommentValidators,
  handleValidationErrors,
  createComment
);

// 4. Xóa comment của mình
router.delete(
  "/:id",
  deleteCommentValidators,
  handleValidationErrors,
  deleteComment
);

export default router;