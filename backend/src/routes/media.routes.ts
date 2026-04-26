import { Router, Request, Response, NextFunction } from "express";
import { param, body } from "express-validator";
import { authenticate } from "../middlewares/auth.middleware";
import { handleValidationErrors } from "../middlewares/validate.middleware";
import {
  uploadMedia,
  uploadImage,
  uploadVideo,
  uploadDocument,
} from "../middlewares/upload.middleware";
import {
  uploadMediaFiles,
  uploadSingleFile,
  getMediaByTarget,
  deleteMedia,
  getMyMedia,
} from "../controllers/media.controller";

const router = Router();

// ─── Validators ───────────────────────────────────────────────────────────────

const targetValidators = [
  body("sourceType")
    .notEmpty()
    .withMessage("sourceType là bắt buộc")
    .isIn(["post", "story", "message"])
    .withMessage("sourceType phải là post, story hoặc message"),
  body("targetId")
    .notEmpty()
    .withMessage("targetId là bắt buộc")
    .isMongoId()
    .withMessage("targetId không hợp lệ"),
];

const idParamValidator = [
  param("id").isMongoId().withMessage("ID không hợp lệ"),
];

const targetParamValidators = [
  param("sourceType")
    .isIn(["post", "story", "message"])
    .withMessage("sourceType không hợp lệ"),
  param("targetId").isMongoId().withMessage("targetId không hợp lệ"),
];

// ─── Public Routes ────────────────────────────────────────────────────────────

/** Lấy tất cả media của một post/story/message */
router.get(
  "/:sourceType/:targetId",
  targetParamValidators,
  handleValidationErrors,
  getMediaByTarget
);

// ─── Protected Routes ─────────────────────────────────────────────────────────

router.use(authenticate);

/** Upload nhiều file (image / video / document), tối đa 5, field: "media" */
router.post(
  "/upload",
  (req: Request, res: Response, next: NextFunction) => {
    uploadMedia(req, res, (err) => {
      if (err) {
        return res.status(400).json({ success: false, message: err.message });
      }
      next();
    });
  },
  targetValidators,
  handleValidationErrors,
  uploadMediaFiles
);

/** Upload 1 ảnh đơn, field: "image" */
router.post(
  "/upload/image",
  (req: Request, res: Response, next: NextFunction) => {
    uploadImage(req, res, (err) => {
      if (err) {
        return res.status(400).json({ success: false, message: err.message });
      }
      next();
    });
  },
  targetValidators,
  handleValidationErrors,
  uploadSingleFile
);

/** Upload 1 video đơn, field: "video" */
router.post(
  "/upload/video",
  (req: Request, res: Response, next: NextFunction) => {
    uploadVideo(req, res, (err) => {
      if (err) {
        return res.status(400).json({ success: false, message: err.message });
      }
      next();
    });
  },
  targetValidators,
  handleValidationErrors,
  uploadSingleFile
);

/** Upload 1 tài liệu đơn, field: "media" */
router.post(
  "/upload/document",
  (req: Request, res: Response, next: NextFunction) => {
    uploadDocument(req, res, (err) => {
      if (err) {
        return res.status(400).json({ success: false, message: err.message });
      }
      next();
    });
  },
  targetValidators,
  handleValidationErrors,
  uploadSingleFile
);

/** Lấy media của chính mình (có filter theo fileType) */
router.get("/me", getMyMedia);

/** Xoá media (cleanup Cloudinary + DB) */
router.delete(
  "/:id",
  idParamValidator,
  handleValidationErrors,
  deleteMedia
);

export default router;
