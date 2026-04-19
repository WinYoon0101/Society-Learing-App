import { Response } from "express";
import mongoose from "mongoose";
import cloudinary from "../config/cloudinary";
import Media from "../models/media.model";
import { AuthRequest } from "../middlewares/auth.middleware";
import { resolveFileType } from "../middlewares/upload.middleware";
import { MediaSourceType } from "../models/media.model";

// ─── Controllers ─────────────────────────────────────────────────────────────

/**
 * @route   POST /api/media/upload
 * @access  Private
 * @desc    Upload 1 hoặc nhiều file lên Cloudinary, lưu metadata vào DB
 *
 * Body (multipart/form-data):
 *   - media: file(s) — tối đa 5
 *   - sourceType: 'post' | 'story' | 'message'
 *   - targetId: ObjectId (ID của post/story/message)
 */
export const uploadMediaFiles = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { sourceType, targetId } = req.body;

    // Multer đã xử lý upload lên Cloudinary trước khi vào đây
    const files = req.files as Express.Multer.File[];
    if (!files || files.length === 0) {
      res.status(400).json({ success: false, message: "Chưa có file nào được upload." });
      return;
    }

    if (!sourceType || !["post", "story", "message"].includes(sourceType)) {
      res.status(400).json({
        success: false,
        message: "sourceType không hợp lệ. Phải là post, story hoặc message.",
      });
      return;
    }

    if (!targetId || !mongoose.Types.ObjectId.isValid(targetId as string)) {
      res.status(400).json({ success: false, message: "targetId không hợp lệ." });
      return;
    }

    // Tạo Media records cho từng file
    const mediaRecords = files.map((file: any) => ({
      userId,
      url: file.path,          // Cloudinary trả về URL trong file.path
      fileType: resolveFileType(file.mimetype),
      sourceType: sourceType as MediaSourceType,
      targetId: new mongoose.Types.ObjectId(targetId as string),
    }));

    const created = await Media.insertMany(mediaRecords);

    res.status(201).json({
      success: true,
      message: `Upload thành công ${created.length} file.`,
      data: created,
    });
  } catch (error: any) {
    console.error("uploadMediaFiles error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi khi upload, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   POST /api/media/upload/single
 * @access  Private
 * @desc    Upload 1 file duy nhất (image / video / document)
 *
 * Body (multipart/form-data):
 *   - media: file
 *   - sourceType: 'post' | 'story' | 'message'
 *   - targetId: ObjectId
 */
export const uploadSingleFile = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { sourceType, targetId } = req.body;
    const file = req.file as any;

    if (!file) {
       res.status(400).json({ success: false, message: "Chưa có file nào được upload." });
       return;
    }

    // Logic targetId 
    const finalTargetId = targetId && mongoose.Types.ObjectId.isValid(targetId) 
                          ? new mongoose.Types.ObjectId(targetId as string)
                          : new mongoose.Types.ObjectId(userId);

    const media = await Media.create({
      userId,
      url: file.path, // Link Cloudinary
      fileType: resolveFileType(file.mimetype),
      sourceType: sourceType || "post",
      targetId: finalTargetId,
    });

    res.status(201).json({ success: true, message: "Upload thành công!", data: media });
  } catch (error: any) {
    console.error("uploadSingleFile error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi khi upload, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   GET /api/media/:sourceType/:targetId
 * @access  Public
 * @desc    Lấy tất cả media của một post/story/message
 */
export const getMediaByTarget = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const sourceType = req.params.sourceType as string;
    const targetId = req.params.targetId as string;

    if (!["post", "story", "message"].includes(sourceType)) {
      res.status(400).json({
        success: false,
        message: "sourceType không hợp lệ.",
      });
      return;
    }

    if (!mongoose.Types.ObjectId.isValid(targetId)) {
      res.status(400).json({ success: false, message: "targetId không hợp lệ." });
      return;
    }

    const media = await Media.find({ sourceType, targetId })
      .populate("userId", "username avatar")
      .sort({ createdAt: 1 });

    res.status(200).json({
      success: true,
      data: media,
    });
  } catch (error: any) {
    console.error("getMediaByTarget error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   DELETE /api/media/:id
 * @access  Private (chủ media)
 * @desc    Xoá media khỏi Cloudinary và DB
 */
export const deleteMedia = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const id = req.params.id as string;
    const userId = req.user!.id;

    if (!mongoose.Types.ObjectId.isValid(id)) {
      res.status(400).json({ success: false, message: "ID không hợp lệ." });
      return;
    }

    const media = await Media.findById(id);
    if (!media) {
      res.status(404).json({ success: false, message: "Media không tìm thấy." });
      return;
    }

    if (media.userId.toString() !== userId) {
      res.status(403).json({
        success: false,
        message: "Bạn không có quyền xoá media này.",
      });
      return;
    }

    // Xoá file khỏi Cloudinary
    try {
      // Lấy public_id từ URL Cloudinary
      // URL format: https://res.cloudinary.com/<cloud>/image/upload/v123456/<folder>/<public_id>.<ext>
      const urlParts = media.url.split("/");
      const uploadIndex = urlParts.indexOf("upload");

      if (uploadIndex !== -1) {
        // Bỏ version (v123456) nếu có
        const parts = urlParts.slice(uploadIndex + 1);
        if (parts[0].startsWith("v") && /^v\d+$/.test(parts[0])) parts.shift();
        const publicIdWithExt = parts.join("/");
        const publicId = publicIdWithExt.replace(/\.[^.]+$/, ""); // bỏ extension

        // Xác định resource_type từ fileType
        const resourceType =
          media.fileType === "image"
            ? "image"
            : media.fileType === "video"
            ? "video"
            : "raw";

        await cloudinary.uploader.destroy(publicId, { resource_type: resourceType });
      }
    } catch (cloudErr) {
      // Không block việc xoá DB nếu Cloudinary lỗi
      console.warn("Cloudinary destroy warning:", cloudErr);
    }

    await Media.findByIdAndDelete(id);

    res.status(200).json({
      success: true,
      message: "Xoá media thành công!",
    });
  } catch (error: any) {
    console.error("deleteMedia error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   GET /api/media/me
 * @access  Private
 * @desc    Lấy tất cả media do user hiện tại upload
 */
export const getMyMedia = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const fileType = req.query.fileType as string | undefined;

    const query: any = { userId };
    if (fileType && ["image", "video", "document"].includes(fileType)) {
      query.fileType = fileType;
    }

    const media = await Media.find(query).sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      data: media,
    });
  } catch (error: any) {
    console.error("getMyMedia error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};
