import multer from "multer";
import { CloudinaryStorage } from "multer-storage-cloudinary";
import cloudinary from "../config/cloudinary";
import { MediaFileType } from "../models/media.model";

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Xác định resource_type cho Cloudinary dựa theo mimetype
 */
const getResourceType = (
  mimetype: string
): "image" | "video" | "raw" => {
  if (mimetype.startsWith("image/")) return "image";
  if (mimetype.startsWith("video/")) return "video";
  return "raw"; // pdf, docx, pptx, ...
};

/**
 * Map mimetype → fileType theo enum của Media model
 */
export const resolveFileType = (mimetype: string): MediaFileType => {
  if (mimetype.startsWith("image/")) return "image";
  if (mimetype.startsWith("video/")) return "video";
  return "document";
};

// ─── Storage Configs ──────────────────────────────────────────────────────────

/**
 * Storage dành cho ảnh (avatar, cover, post image...)
 */
const imageStorage = new CloudinaryStorage({
  cloudinary,
  params: (req: any, file: Express.Multer.File) => ({
    folder: "society/images",
    resource_type: "image",
    // ĐÃ THÊM heif, heic VÀO ĐÂY
    allowed_formats: ["jpg", "jpeg", "png", "webp", "gif", "heif", "heic"],
    transformation: [{ quality: "auto", fetch_format: "auto" }],
    public_id: `img_${Date.now()}`,
  }),
});

/**
 * Storage dành cho video
 */
const videoStorage = new CloudinaryStorage({
  cloudinary,
  params: (req: any, file: Express.Multer.File) => ({
    folder: "society/videos",
    resource_type: "video",
    allowed_formats: ["mp4", "mov", "avi", "webm"],
    public_id: `vid_${Date.now()}`,
  }),
});

/**
 * Storage dành cho tài liệu (pdf, docx, pptx...)
 */
const documentStorage = new CloudinaryStorage({
  cloudinary,
  params: (req: any, file: Express.Multer.File) => ({
    folder: "society/documents",
    resource_type: "auto",
    allowed_formats: ["pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt"],
    public_id: `doc_${Date.now()}_${file.originalname.split('.')[0]}`,
  }),
});

/**
 * Storage tự động phát hiện loại file (đa năng)
 */
const autoStorage = new CloudinaryStorage({
  cloudinary,
  params: (req: any, file: Express.Multer.File) => ({
    folder: "society/media",
    resource_type: getResourceType(file.mimetype),
    public_id: `media_${Date.now()}`,
    transformation:
      file.mimetype.startsWith("image/")
        ? [{ quality: "auto", fetch_format: "auto" }]
        : undefined,
  }),
});

// ─── File Filters ─────────────────────────────────────────────────────────────

const imageFilter = (
  req: Express.Request,
  file: Express.Multer.File,
  cb: multer.FileFilterCallback
) => {
  if (file.mimetype.startsWith("image/")) {
    cb(null, true);
  } else {
    // Cập nhật câu báo lỗi để hiện heif, heic
    cb(new Error("Chỉ chấp nhận file ảnh (jpg, jpeg, png, webp, gif, heif, heic)."));
  }
};

const videoFilter = (
  req: Express.Request,
  file: Express.Multer.File,
  cb: multer.FileFilterCallback
) => {
  if (file.mimetype.startsWith("video/")) {
    cb(null, true);
  } else {
    cb(new Error("Chỉ chấp nhận file video (mp4, mov, avi, webm)."));
  }
};

const documentFilter = (
  req: Express.Request,
  file: Express.Multer.File,
  cb: multer.FileFilterCallback
) => {
  const ALLOWED = [
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain",
  ];
  if (ALLOWED.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error("Loại file không được hỗ trợ. Vui lòng upload pdf, docx, pptx, xlsx hoặc txt."));
  }
};

const autoFilter = (
  req: Express.Request,
  file: Express.Multer.File,
  cb: multer.FileFilterCallback
) => {
  // Chấp nhận image + video + document
  const ALLOWED_MIMES = [
    "image/jpeg", "image/png", "image/webp", "image/gif", 
    "image/heif", "image/heic", // ĐÃ THÊM 2 ĐỊNH DẠNG NÀY
    "video/mp4", "video/quicktime", "video/x-msvideo", "video/webm",
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain",
    "application/octet-stream",
  ];
  console.log("Định dạng file:", file.mimetype, " | Tên file:", file.originalname);
  if (ALLOWED_MIMES.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error("Loại file không được hỗ trợ."));
  }
};

// ─── Exports ──────────────────────────────────────────────────────────────────

/** Upload 1 ảnh duy nhất, field name: "image" */
export const uploadImage = multer({
  storage: imageStorage,
  fileFilter: imageFilter,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10 MB
}).single("image");

/** Upload 1 video duy nhất, field name: "video" */
export const uploadVideo = multer({
  storage: videoStorage,
  fileFilter: videoFilter,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100 MB
}).single("video");

/** Upload 1 tài liệu duy nhất, field name: "file" */
export const uploadDocument = multer({
  storage: documentStorage,
  fileFilter: documentFilter,
  limits: { fileSize: 50 * 1024 * 1024 }, // 50 MB
}).single("media");

/** Upload bất kỳ loại file, field name: "media" (tối đa 5 file) */
export const uploadMedia = multer({
  storage: autoStorage,
  fileFilter: autoFilter,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100 MB
}).array("media", 5);

/** Upload ảnh nhiều file, field name: "images" (tối đa 10 ảnh) */
export const uploadImages = multer({
  storage: imageStorage,
  fileFilter: imageFilter,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10 MB
}).array("images", 10);