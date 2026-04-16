import { Response } from "express";
import mongoose from "mongoose";
import DocumentModel from "../models/document.model";
import Media from "../models/media.model";
import User from "../models/user.model";
import { AuthRequest } from "../middlewares/auth.middleware";

// ─── Helpers ────────────────────────────────────────────────────────────────

const DEFAULT_PAGE_SIZE = 10;
const MAX_PAGE_SIZE = 50;

const getPagination = (page: any, limit: any) => {
  const p = Math.max(1, parseInt(page) || 1);
  const l = Math.min(MAX_PAGE_SIZE, Math.max(1, parseInt(limit) || DEFAULT_PAGE_SIZE));
  return { page: p, limit: l, skip: (p - 1) * l };
};

// ─── Controllers ─────────────────────────────────────────────────────────────

/**
 * @route   POST /api/documents
 * @access  Private
 * @desc    Tạo document mới (đã có mediaId từ upload trước)
 */
export const createDocument = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const uploaderId = req.user!.id;
    const { mediaId, title, description, fileType, subject, visibility, groupId } =
      req.body;

    // Kiểm tra media tồn tại và thuộc về uploader
    const media = await Media.findById(mediaId);
    if (!media) {
      res.status(404).json({ success: false, message: "Media không tìm thấy." });
      return;
    }
    if (media.userId.toString() !== uploaderId) {
      res.status(403).json({
        success: false,
        message: "Bạn không có quyền sử dụng media này.",
      });
      return;
    }

    const document = await DocumentModel.create({
      uploaderId,
      mediaId,
      groupId: groupId || null,
      title,
      description,
      fileType: fileType || media.fileType,
      subject,
      visibility: visibility || "public",
    });

    const populated = await document.populate([
      { path: "uploaderId", select: "username avatar" },
      { path: "mediaId", select: "url fileType" },
    ]);

    res.status(201).json({
      success: true,
      message: "Tạo tài liệu thành công!",
      data: populated,
    });
  } catch (error: any) {
    console.error("createDocument error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   GET /api/documents
 * @access  Public
 * @desc    Lấy danh sách tài liệu public (có filter + search + pagination)
 */
export const getDocuments = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const { page, limit, subject, search, sortBy } = req.query;
    const { page: p, limit: l, skip } = getPagination(page, limit);

    const query: any = { visibility: "public" };

    if (subject) {
      query.subject = { $regex: subject, $options: "i" };
    }

    if (search) {
      query.$text = { $search: search as string };
    }

    let sort: any = { createdAt: -1 };
    if (sortBy === "views") sort = { numberView: -1 };
    if (sortBy === "downloads") sort = { numberDownload: -1 };
    if (sortBy === "newest") sort = { createdAt: -1 };

    const [documents, total] = await Promise.all([
      DocumentModel.find(query)
        .sort(sort)
        .skip(skip)
        .limit(l)
        .populate("uploaderId", "username avatar")
        .populate("mediaId", "url fileType"),
      DocumentModel.countDocuments(query),
    ]);

    res.status(200).json({
      success: true,
      data: {
        documents,
        pagination: {
          page: p,
          limit: l,
          total,
          totalPages: Math.ceil(total / l),
        },
      },
    });
  } catch (error: any) {
    console.error("getDocuments error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   GET /api/documents/:id
 * @access  Public / Private (tùy visibility)
 * @desc    Lấy chi tiết tài liệu + tăng lượt view
 */
export const getDocumentById = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const id = req.params.id as string;

    if (!mongoose.Types.ObjectId.isValid(id)) {
      res.status(400).json({ success: false, message: "ID không hợp lệ." });
      return;
    }

    const document = await DocumentModel.findById(id)
      .populate("uploaderId", "username avatar bio")
      .populate("mediaId", "url fileType");

    if (!document) {
      res.status(404).json({ success: false, message: "Tài liệu không tìm thấy." });
      return;
    }

    // Kiểm tra quyền truy cập
    const userId = req.user?.id;
    if (document.visibility === "private") {
      if (!userId || document.uploaderId._id.toString() !== userId) {
        res.status(403).json({
          success: false,
          message: "Bạn không có quyền xem tài liệu này.",
        });
        return;
      }
    }

    // Tăng lượt xem (không chặn response)
    DocumentModel.findByIdAndUpdate(id, { $inc: { numberView: 1 } }).exec();

    res.status(200).json({
      success: true,
      data: document,
    });
  } catch (error: any) {
    console.error("getDocumentById error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   PATCH /api/documents/:id
 * @access  Private (chủ document)
 * @desc    Cập nhật thông tin tài liệu
 */
export const updateDocument = async (
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

    const document = await DocumentModel.findById(id);
    if (!document) {
      res.status(404).json({ success: false, message: "Tài liệu không tìm thấy." });
      return;
    }

    if (document.uploaderId.toString() !== userId) {
      res.status(403).json({
        success: false,
        message: "Bạn không có quyền chỉnh sửa tài liệu này.",
      });
      return;
    }

    const allowedFields = ["title", "description", "subject", "visibility", "groupId"];
    const updates: any = {};
    for (const field of allowedFields) {
      if (req.body[field] !== undefined) {
        updates[field] = req.body[field];
      }
    }

    const updated = await DocumentModel.findByIdAndUpdate(
      id,
      { $set: updates },
      { new: true, runValidators: true }
    )
      .populate("uploaderId", "username avatar")
      .populate("mediaId", "url fileType");

    res.status(200).json({
      success: true,
      message: "Cập nhật tài liệu thành công!",
      data: updated,
    });
  } catch (error: any) {
    console.error("updateDocument error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   DELETE /api/documents/:id
 * @access  Private (chủ document)
 * @desc    Xoá tài liệu
 */
export const deleteDocument = async (
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

    const document = await DocumentModel.findById(id);
    if (!document) {
      res.status(404).json({ success: false, message: "Tài liệu không tìm thấy." });
      return;
    }

    if (document.uploaderId.toString() !== userId) {
      res.status(403).json({
        success: false,
        message: "Bạn không có quyền xoá tài liệu này.",
      });
      return;
    }

    await Promise.all([
      DocumentModel.findByIdAndDelete(id),
      // Xoá document khỏi savedDocument của tất cả user đã lưu
      User.updateMany(
        { savedDocument: document._id },
        { $pull: { savedDocument: document._id } }
      ),
    ]);

    res.status(200).json({
      success: true,
      message: "Xoá tài liệu thành công!",
    });
  } catch (error: any) {
    console.error("deleteDocument error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   POST /api/documents/:id/download
 * @access  Public
 * @desc    Tăng lượt download của tài liệu
 */
export const incrementDownload = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const id = req.params.id as string;

    if (!mongoose.Types.ObjectId.isValid(id)) {
      res.status(400).json({ success: false, message: "ID không hợp lệ." });
      return;
    }

    const document = await DocumentModel.findByIdAndUpdate(
      id,
      { $inc: { numberDownload: 1 } },
      { new: true }
    );

    if (!document) {
      res.status(404).json({ success: false, message: "Tài liệu không tìm thấy." });
      return;
    }

    res.status(200).json({
      success: true,
      message: "Đã ghi nhận lượt tải.",
      data: {
        numberDownload: document.numberDownload,
      },
    });
  } catch (error: any) {
    console.error("incrementDownload error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   GET /api/documents/me
 * @access  Private
 * @desc    Lấy tài liệu của chính mình (bao gồm private)
 */
export const getMyDocuments = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { page, limit, visibility } = req.query;
    const { page: p, limit: l, skip } = getPagination(page, limit);

    const query: any = { uploaderId: userId };
    if (visibility) query.visibility = visibility;

    const [documents, total] = await Promise.all([
      DocumentModel.find(query)
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(l)
        .populate("mediaId", "url fileType"),
      DocumentModel.countDocuments(query),
    ]);

    res.status(200).json({
      success: true,
      data: {
        documents,
        pagination: {
          page: p,
          limit: l,
          total,
          totalPages: Math.ceil(total / l),
        },
      },
    });
  } catch (error: any) {
    console.error("getMyDocuments error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   GET /api/documents/user/:userId
 * @access  Public
 * @desc    Lấy tài liệu public của một user cụ thể
 */
export const getDocumentsByUser = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.params.userId as string;
    const { page, limit } = req.query;
    const { page: p, limit: l, skip } = getPagination(page, limit);

    if (!mongoose.Types.ObjectId.isValid(userId)) {
      res.status(400).json({ success: false, message: "User ID không hợp lệ." });
      return;
    }

    const [documents, total] = await Promise.all([
      DocumentModel.find({ uploaderId: userId, visibility: "public" })
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(l)
        .populate("mediaId", "url fileType"),
      DocumentModel.countDocuments({ uploaderId: userId, visibility: "public" }),
    ]);

    res.status(200).json({
      success: true,
      data: {
        documents,
        pagination: {
          page: p,
          limit: l,
          total,
          totalPages: Math.ceil(total / l),
        },
      },
    });
  } catch (error: any) {
    console.error("getDocumentsByUser error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   POST /api/documents/:id/save
 * @access  Private
 * @desc    Lưu / bỏ lưu tài liệu vào danh sách của user
 */
export const toggleSaveDocument = async (
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

    const docExists = await DocumentModel.exists({ _id: id });
    if (!docExists) {
      res.status(404).json({ success: false, message: "Tài liệu không tìm thấy." });
      return;
    }

    const user = await User.findById(userId).select("savedDocument");
    if (!user) {
      res.status(404).json({ success: false, message: "Người dùng không tìm thấy." });
      return;
    }

    const docId = new mongoose.Types.ObjectId(id);
    const isSaved = user.savedDocument.some((d) => d.equals(docId));

    if (isSaved) {
      await User.findByIdAndUpdate(userId, { $pull: { savedDocument: docId } });
      res.status(200).json({
        success: true,
        message: "Đã bỏ lưu tài liệu.",
        data: { saved: false },
      });
    } else {
      await User.findByIdAndUpdate(userId, { $addToSet: { savedDocument: docId } });
      res.status(200).json({
        success: true,
        message: "Đã lưu tài liệu.",
        data: { saved: true },
      });
    }
  } catch (error: any) {
    console.error("toggleSaveDocument error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};

/**
 * @route   GET /api/documents/saved
 * @access  Private
 * @desc    Lấy danh sách tài liệu đã lưu của user
 */
export const getSavedDocuments = async (
  req: AuthRequest,
  res: Response
): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { page, limit } = req.query;
    const { page: p, limit: l, skip } = getPagination(page, limit);

    const user = await User.findById(userId).select("savedDocument");
    if (!user) {
      res.status(404).json({ success: false, message: "Người dùng không tìm thấy." });
      return;
    }

    const total = user.savedDocument.length;
    // Lấy slice từ savedDocument array để paginate
    const ids = user.savedDocument.slice(skip, skip + l);

    const documents = await DocumentModel.find({ _id: { $in: ids } })
      .populate("uploaderId", "username avatar")
      .populate("mediaId", "url fileType");

    res.status(200).json({
      success: true,
      data: {
        documents,
        pagination: {
          page: p,
          limit: l,
          total,
          totalPages: Math.ceil(total / l),
        },
      },
    });
  } catch (error: any) {
    console.error("getSavedDocuments error:", error);
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi, vui lòng thử lại sau.",
    });
  }
};
