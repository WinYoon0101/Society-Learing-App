import multer from "multer";
import { CloudinaryStorage } from "multer-storage-cloudinary";
import cloudinary from "../config/cloudinary";
import { MediaFileType } from "../models/media.model";
import path from "path";

// ─── Helpers ─────────────────────────────────────────────────────────────────

const getResourceType = (mimetype: string): "image" | "video" | "raw" => {
  if (mimetype.startsWith("image/")) return "image";
  if (mimetype.startsWith("video/")) return "video";
  return "raw";
};

export const resolveFileType = (mimetype: string): MediaFileType => {
  if (mimetype.startsWith("image/")) return "image";
  if (mimetype.startsWith("video/")) return "video";
  return "document";
};

// ─── Storage Configs ──────────────────────────────────────────────────────────

const imageStorage = new CloudinaryStorage({
  cloudinary,
  params: async (req: any, file: Express.Multer.File) => ({
    folder: "society/images",
    resource_type: "image",
    allowed_formats: ["jpg", "jpeg", "png", "webp", "gif", "heif", "heic"],
    transformation: [{ quality: "auto", fetch_format: "auto" }],
    public_id: `img_${Date.now()}`,
  }),
});

const videoStorage = new CloudinaryStorage({
  cloudinary,
  params: async (req: any, file: Express.Multer.File) => ({
    folder: "society/videos",
    resource_type: "video",
    allowed_formats: ["mp4", "mov", "avi", "webm"],
    public_id: `vid_${Date.now()}`,
  }),
});

// Phân tách logic cho ảnh và tài liệu raw
const documentStorage = new CloudinaryStorage({
  cloudinary,
  params: async (req: any, file: Express.Multer.File) => {
    const isImage = file.mimetype.startsWith("image/");
    
    if (isImage) {
      return {
        folder: "society/documents",
        resource_type: "image",
        allowed_formats: ["jpg", "jpeg", "png", "webp", "gif", "heic", "heif"],
        public_id: `doc_${Date.now()}_${file.originalname.split(".")[0]}`,
      };
    } else {
      // Đối với tài liệu (.docx, .pdf, .txt, ...)
      return {
        folder: "society/documents",
        resource_type: "raw", 
        // QUAN TRỌNG: KHÔNG dùng allowed_formats cho resource_type raw
        public_id: `doc_${Date.now()}_${file.originalname}`, // Bắt buộc giữ đuôi file
      };
    }
  },
});

const autoStorage = new CloudinaryStorage({
  cloudinary,
  params: async (req: any, file: Express.Multer.File) => {
    const rType = getResourceType(file.mimetype);
    return {
      folder: "society/media",
      resource_type: rType,
      public_id: rType === "raw" 
        ? `media_${Date.now()}_${file.originalname}` 
        : `media_${Date.now()}`,
      transformation: rType === "image"
        ? [{ quality: "auto", fetch_format: "auto" }]
        : undefined,
    };
  },
});

// ─── File Filters ─────────────────────────────────────────────────────────────

const imageFilter = (req: Express.Request, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
  if (file.mimetype.startsWith("image/")) {
    cb(null, true);
  } else {
    cb(new Error("Chỉ chấp nhận file ảnh (jpg, jpeg, png, webp, gif, heif, heic)."));
  }
};

const videoFilter = (req: Express.Request, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
  if (file.mimetype.startsWith("video/")) {
    cb(null, true);
  } else {
    cb(new Error("Chỉ chấp nhận file video (mp4, mov, avi, webm)."));
  }
};

const documentFilter = (req: Express.Request, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
  const ALLOWED = [
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain",
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/gif",
    "image/heic",
    "image/heif"
  ];

  const ext = path.extname(file.originalname).toLowerCase();
  const ALLOWED_EXTS = [".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx", ".txt", ".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".heif"];

  if (ALLOWED.includes(file.mimetype) || ALLOWED_EXTS.includes(ext)) {
    cb(null, true);
  } else {
    cb(new Error("Loại file không được hỗ trợ. Vui lòng upload pdf, docx, pptx, xlsx, txt hoặc ảnh."));
  }
};

const autoFilter = (req: Express.Request, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
  const ALLOWED_MIMES = [
    "image/jpeg", "image/png", "image/webp", "image/gif", "image/heif", "image/heic",
    "video/mp4", "video/quicktime", "video/x-msvideo", "video/webm",
    "application/pdf", "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain", "application/octet-stream",
  ];
  if (ALLOWED_MIMES.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error("Loại file không được hỗ trợ."));
  }
};

const postMediaFilter = (req: Express.Request, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
  if (file.mimetype.startsWith("image/") || file.mimetype.startsWith("video/")) {
    cb(null, true);
  } else {
    cb(new Error("Chi chap nhan anh hoac video cho bai viet."));
  }
};

// ─── Exports ──────────────────────────────────────────────────────────────────

export const uploadImage = multer({
  storage: imageStorage,
  fileFilter: imageFilter,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10 MB
}).single("image");

export const uploadVideo = multer({
  storage: videoStorage,
  fileFilter: videoFilter,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100 MB
}).single("video");

export const uploadDocument = multer({
  storage: documentStorage,
  fileFilter: documentFilter,
  limits: { fileSize: 50 * 1024 * 1024 }, // 50 MB
}).single("media");

export const uploadMedia = multer({
  storage: autoStorage,
  fileFilter: autoFilter,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100 MB
}).array("media", 5);

export const uploadPostMedia = multer({
  storage: autoStorage,
  fileFilter: postMediaFilter,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100 MB
}).fields([
  { name: "images", maxCount: 10 },
  { name: "videos", maxCount: 5 },
]);

export const uploadImages = multer({
  storage: imageStorage,
  fileFilter: imageFilter,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10 MB
}).array("images", 10);

export const uploadFile = multer({
  storage: imageStorage,
  fileFilter: imageFilter,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10 MB
}).single("file");

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, "uploads/");
  },
  filename: (req, file, cb) => {
    const uniqueName = Date.now() + path.extname(file.originalname);
    cb(null, uniqueName);
  },
});
export const upload = multer({ storage });
