import { Router } from "express";
import { body, param, query } from "express-validator";
import { authenticate } from "../middlewares/auth.middleware";
import { handleValidationErrors } from "../middlewares/validate.middleware";
import {
  createDocument,
  getDocuments,
  getDocumentById,
  updateDocument,
  deleteDocument,
  incrementDownload,
  getMyDocuments,
  getDocumentsByUser,
  toggleSaveDocument,
  getSavedDocuments,
} from "../controllers/document.controller";

const router = Router();

// ─── Validators ────────────────────────────────────────────────────────────────

const createDocumentValidators = [
  body("mediaId")
    .notEmpty()
    .withMessage("mediaId là bắt buộc")
    .isMongoId()
    .withMessage("mediaId không hợp lệ"),

  body("title")
    .trim()
    .notEmpty()
    .withMessage("Tiêu đề là bắt buộc")
    .isLength({ min: 2, max: 200 })
    .withMessage("Tiêu đề phải từ 2-200 ký tự"),

  body("description")
    .optional({ checkFalsy: true })
    .isLength({ max: 1000 })
    .withMessage("Mô tả không được vượt quá 1000 ký tự"),

  body("fileType")
    .optional({ checkFalsy: true })
    .isString()
    .withMessage("fileType phải là chuỗi"),

  body("subject")
    .trim()
    .notEmpty()
    .withMessage("Môn học / Chủ đề là bắt buộc")
    .isLength({ max: 100 })
    .withMessage("Môn học không được vượt quá 100 ký tự"),

  body("visibility")
    .optional()
    .isIn(["public", "private", "group"])
    .withMessage("visibility phải là public, private hoặc group"),

  body("groupId")
    .optional({ checkFalsy: true })
    .isMongoId()
    .withMessage("groupId không hợp lệ"),
];

const updateDocumentValidators = [
  param("id").isMongoId().withMessage("Document ID không hợp lệ"),

  body("title")
    .optional()
    .trim()
    .isLength({ min: 2, max: 200 })
    .withMessage("Tiêu đề phải từ 2-200 ký tự"),

  body("description")
    .optional({ checkFalsy: true })
    .isLength({ max: 1000 })
    .withMessage("Mô tả không được vượt quá 1000 ký tự"),

  body("subject")
    .optional()
    .trim()
    .isLength({ max: 100 })
    .withMessage("Môn học không được vượt quá 100 ký tự"),

  body("visibility")
    .optional()
    .isIn(["public", "private", "group"])
    .withMessage("visibility phải là public, private hoặc group"),

  body("groupId")
    .optional({ checkFalsy: true })
    .isMongoId()
    .withMessage("groupId không hợp lệ"),
];

const listQueryValidators = [
  query("page")
    .optional()
    .isInt({ min: 1 })
    .withMessage("page phải là số nguyên dương"),

  query("limit")
    .optional()
    .isInt({ min: 1, max: 50 })
    .withMessage("limit phải từ 1-50"),

  query("sortBy")
    .optional()
    .isIn(["newest", "views", "downloads"])
    .withMessage("sortBy phải là newest, views hoặc downloads"),
];

const mongoIdParamValidator = [
  param("id").isMongoId().withMessage("ID không hợp lệ"),
];

const userIdParamValidator = [
  param("userId").isMongoId().withMessage("User ID không hợp lệ"),
];

// ─── Routes ────────────────────────────────────────────────────────────────────

// Public routes
router.get(
  "/",
  listQueryValidators,
  handleValidationErrors,
  getDocuments
);

router.get(
  "/:id",
  mongoIdParamValidator,
  handleValidationErrors,
  getDocumentById
);

router.get(
  "/user/:userId",
  userIdParamValidator,
  listQueryValidators,
  handleValidationErrors,
  getDocumentsByUser
);

// Protected routes
router.use(authenticate);

router.post(
  "/",
  createDocumentValidators,
  handleValidationErrors,
  createDocument
);

router.patch(
  "/:id",
  updateDocumentValidators,
  handleValidationErrors,
  updateDocument
);

router.delete(
  "/:id",
  mongoIdParamValidator,
  handleValidationErrors,
  deleteDocument
);

router.post(
  "/:id/download",
  mongoIdParamValidator,
  handleValidationErrors,
  incrementDownload
);

router.post(
  "/:id/save",
  mongoIdParamValidator,
  handleValidationErrors,
  toggleSaveDocument
);

router.get(
  "/me/list",
  listQueryValidators,
  handleValidationErrors,
  getMyDocuments
);

router.get(
  "/me/saved",
  listQueryValidators,
  handleValidationErrors,
  getSavedDocuments
);

export default router;
