import { Response } from "express";
import mongoose from "mongoose";
import DocumentModel from "../models/document.model";
import Media from "../models/media.model";
import User from "../models/user.model";
import { AuthRequest } from "../middlewares/auth.middleware";
import cloudinary from "../config/cloudinary";

import { GoogleGenerativeAI, SchemaType, Schema } from "@google/generative-ai";
import axios from "axios";
// Đã xóa import pdfParse - Không cần thiết nữa!

// ─── Helpers ────────────────────────────────────────────────────────────────

const DEFAULT_PAGE_SIZE = 10;
const MAX_PAGE_SIZE = 50;

const getPagination = (page: any, limit: any) => {
  const p = Math.max(1, parseInt(page) || 1);
  const l = Math.min(MAX_PAGE_SIZE, Math.max(1, parseInt(limit) || DEFAULT_PAGE_SIZE));
  return { page: p, limit: l, skip: (p - 1) * l };
};

// ─── Controllers ─────────────────────────────────────────────────────────────

export const createDocument = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const uploaderId = req.user!.id;
    const { mediaId, title, description, fileType, subject, visibility, groupId } = req.body;

    const media = await Media.findById(mediaId);
    if (!media) {
      res.status(404).json({ success: false, message: "Media không tìm thấy." });
      return;
    }
    if (media.userId.toString() !== uploaderId) {
      res.status(403).json({ success: false, message: "Bạn không có quyền sử dụng media này." });
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

    res.status(201).json({ success: true, message: "Tạo tài liệu thành công!", data: populated });
  } catch (error: any) {
    console.error("createDocument error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

export const getDocuments = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { page, limit, subject, search, sortBy } = req.query;
    const { page: p, limit: l, skip } = getPagination(page, limit);

    const query: any = { visibility: "public" };

    if (subject) query.subject = { $regex: subject, $options: "i" };

    if (search) {
      query.$or = [
        { title: { $regex: search, $options: "i" } },
        { description: { $regex: search, $options: "i" } },
        { subject: { $regex: search, $options: "i" } }
      ];
    }

    let sort: any = { createdAt: -1 };
    if (sortBy === "views") sort = { numberView: -1 };
    if (sortBy === "downloads") sort = { numberDownload: -1 };
    if (sortBy === "newest") sort = { createdAt: -1 };
    if (sortBy === "oldest") sort = { createdAt: 1 };

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
        pagination: { page: p, limit: l, total, totalPages: Math.ceil(total / l) },
      },
    });
  } catch (error: any) {
    console.error("getDocuments error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

export const getDocumentById = async (req: AuthRequest, res: Response): Promise<void> => {
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

    const userId = req.user?.id;
    if (document.visibility === "private") {
      if (!userId || document.uploaderId._id.toString() !== userId) {
        res.status(403).json({ success: false, message: "Bạn không có quyền xem tài liệu này." });
        return;
      }
    }

    DocumentModel.findByIdAndUpdate(id, { $inc: { numberView: 1 } }).exec();

    res.status(200).json({ success: true, data: document });
  } catch (error: any) {
    console.error("getDocumentById error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

export const updateDocument = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const id = req.params.id as string;
    const userId = req.user!.id;
    const { mediaId: newMediaId } = req.body;

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
      res.status(403).json({ success: false, message: "Bạn không có quyền chỉnh sửa." });
      return;
    }

    const updates: any = {};
    const allowedFields = ["title", "description", "subject", "visibility", "groupId"];
    
    for (const field of allowedFields) {
      if (req.body[field] !== undefined) updates[field] = req.body[field];
    }

    if (newMediaId && newMediaId !== document.mediaId.toString()) {
      const newMedia = await Media.findById(newMediaId);
      if (!newMedia || newMedia.userId.toString() !== userId) {
        res.status(400).json({ success: false, message: "Media mới không hợp lệ." });
        return;
      }
      await cleanupOldMedia(document.mediaId.toString());
      updates.mediaId = newMediaId;
      updates.fileType = newMedia.fileType;
    }

    const updated = await DocumentModel.findByIdAndUpdate(id, { $set: updates }, { new: true, runValidators: true })
      .populate("uploaderId", "username avatar")
      .populate("mediaId", "url fileType");

    res.status(200).json({ success: true, message: "Cập nhật tài liệu thành công!", data: updated });
  } catch (error: any) {
    console.error("updateDocument error:", error);
    res.status(500).json({ success: false, message: "Lỗi hệ thống, thử lại sau." });
  }
};

const cleanupOldMedia = async (mediaId: string) => {
  try {
    const media = await Media.findById(mediaId);
    if (!media) return;

    const urlParts = media.url.split("/");
    const uploadIndex = urlParts.indexOf("upload");
    if (uploadIndex !== -1) {
      const parts = urlParts.slice(uploadIndex + 1);
      if (parts[0].startsWith("v")) parts.shift();
      const publicId = parts.join("/").replace(/\.[^.]+$/, "");
      
      const resourceType = media.fileType === "image" || media.url.endsWith(".pdf") ? "image" : "raw";
      await cloudinary.uploader.destroy(publicId, { resource_type: resourceType });
    }
    await Media.findByIdAndDelete(mediaId);
  } catch (err) {
    console.warn("Cleanup media failed:", err);
  }
};

export const deleteDocument = async (req: AuthRequest, res: Response): Promise<void> => {
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
      res.status(403).json({ success: false, message: "Bạn không có quyền xoá tài liệu này." });
      return;
    }

    await Promise.all([
      DocumentModel.findByIdAndDelete(id),
      User.updateMany({ savedDocument: document._id }, { $pull: { savedDocument: document._id } }),
    ]);

    res.status(200).json({ success: true, message: "Xoá tài liệu thành công!" });
  } catch (error: any) {
    console.error("deleteDocument error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

export const incrementDownload = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const id = req.params.id as string;

    if (!mongoose.Types.ObjectId.isValid(id)) {
      res.status(400).json({ success: false, message: "ID không hợp lệ." });
      return;
    }

    const document = await DocumentModel.findByIdAndUpdate(id, { $inc: { numberDownload: 1 } }, { new: true });

    if (!document) {
      res.status(404).json({ success: false, message: "Tài liệu không tìm thấy." });
      return;
    }

    res.status(200).json({ success: true, message: "Đã ghi nhận lượt tải.", data: { numberDownload: document.numberDownload } });
  } catch (error: any) {
    console.error("incrementDownload error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

export const getMyDocuments = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.user!.id;
    const { page, limit, visibility } = req.query;
    const { page: p, limit: l, skip } = getPagination(page, limit);

    const query: any = { uploaderId: userId };
    if (visibility) query.visibility = visibility;

    const [documents, total] = await Promise.all([
      DocumentModel.find(query).sort({ createdAt: -1 }).skip(skip).limit(l).populate("mediaId", "url fileType").populate("uploaderId", "username avatar"),
      DocumentModel.countDocuments(query),
    ]);

    res.status(200).json({ success: true, data: { documents, pagination: { page: p, limit: l, total, totalPages: Math.ceil(total / l) } } });
  } catch (error: any) {
    console.error("getMyDocuments error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

export const getDocumentsByUser = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.params.userId as string;
    const { page, limit } = req.query;
    const { page: p, limit: l, skip } = getPagination(page, limit);

    if (!mongoose.Types.ObjectId.isValid(userId)) {
      res.status(400).json({ success: false, message: "User ID không hợp lệ." });
      return;
    }

    const [documents, total] = await Promise.all([
      DocumentModel.find({ uploaderId: userId, visibility: "public" }).sort({ createdAt: -1 }).skip(skip).limit(l).populate("mediaId", "url fileType"),
      DocumentModel.countDocuments({ uploaderId: userId, visibility: "public" }),
    ]);

    res.status(200).json({ success: true, data: { documents, pagination: { page: p, limit: l, total, totalPages: Math.ceil(total / l) } } });
  } catch (error: any) {
    console.error("getDocumentsByUser error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

export const toggleSaveDocument = async (req: AuthRequest, res: Response): Promise<void> => {
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
      res.status(200).json({ success: true, message: "Đã bỏ lưu tài liệu.", data: { saved: false } });
    } else {
      await User.findByIdAndUpdate(userId, { $addToSet: { savedDocument: docId } });
      res.status(200).json({ success: true, message: "Đã lưu tài liệu.", data: { saved: true } });
    }
  } catch (error: any) {
    console.error("toggleSaveDocument error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

export const getSavedDocuments = async (req: AuthRequest, res: Response): Promise<void> => {
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
    const ids = user.savedDocument.slice(skip, skip + l);

    const rawDocuments = await DocumentModel.find({ _id: { $in: ids } })
      .populate("uploaderId", "username avatar")
      .populate("mediaId", "url fileType");

    const documents = ids.map((id) => rawDocuments.find((doc) => doc._id.equals(id))).filter((doc): doc is NonNullable<typeof doc> => !!doc);

    res.status(200).json({ success: true, data: { documents, pagination: { page: p, limit: l, total, totalPages: Math.ceil(total / l) } } });
  } catch (error: any) {
    console.error("getSavedDocuments error:", error);
    res.status(500).json({ success: false, message: "Đã xảy ra lỗi, vui lòng thử lại sau." });
  }
};

// =========================================================================
// ───  AI SUMMARIZER & MINDMAP (CÓ CACHE) ───────────────────
// =========================================================================

export const generateDocumentMindmap = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const id = req.params.id as string;

    if (!mongoose.Types.ObjectId.isValid(id)) {
      res.status(400).json({ success: false, message: "ID không hợp lệ." });
      return;
    }

    const document: any = await DocumentModel.findById(id).populate("mediaId");
    if (!document) {
      res.status(404).json({ success: false, message: "Tài liệu không tìm thấy." });
      return;
    }

    // KIỂM TRA CACHE TỪ DATABASE
    if (document.mindmapData) {
      console.log(`[Cache Hit] Mindmap có sẵn cho document: ${id}`);
      res.status(200).json({
        success: true,
        message: "Lấy sơ đồ tư duy thành công!",
        data: document.mindmapData,
        isCached: true 
      });
      return;
    }

    console.log(`[Cache Miss] Bắt đầu gọi AI tạo Mindmap cho document: ${id}...`);

    const media: any = document.mediaId;
    if (!media || !media.url) {
      res.status(400).json({ success: false, message: "Tài liệu không đính kèm file hợp lệ." });
      return;
    }

    if (media.fileType !== "pdf" && !media.url.endsWith(".pdf")) {
      res.status(400).json({ success: false, message: "Hiện tại AI chỉ hỗ trợ trích xuất từ file PDF." });
      return;
    }

    // Tải file về dưới dạng Buffer
    const fileResponse = await axios.get(media.url, { responseType: "arraybuffer" });
    const buffer = Buffer.from(fileResponse.data);

    const apiKey = process.env.GEMINI_API_KEY; 
    if (!apiKey) {
      res.status(500).json({ success: false, message: "Chưa cấu hình GEMINI_API_KEY trong file .env" });
      return;
    }

    const genAI = new GoogleGenerativeAI(apiKey);
    
    const mindmapSchema: Schema = {
      type: SchemaType.OBJECT,
      properties: {
        topic: { type: SchemaType.STRING, description: "Chủ đề chính" },
        summary: { type: SchemaType.STRING, description: "Tóm tắt ngắn gọn 2-3 câu" },
        nodes: {
          type: SchemaType.ARRAY,
          items: {
            type: SchemaType.OBJECT,
            properties: {
              title: { type: SchemaType.STRING },
              details: { type: SchemaType.STRING },
              subNodes: {
                type: SchemaType.ARRAY,
                items: {
                  type: SchemaType.OBJECT,
                  properties: {
                    title: { type: SchemaType.STRING },
                    details: { type: SchemaType.STRING }
                  },
                  required: ["title", "details"]
                }
              }
            },
            required: ["title", "details"]
          }
        }
      },
      required: ["topic", "summary", "nodes"],
    };

    const model = genAI.getGenerativeModel({
      model: "gemini-2.5-flash",
      generationConfig: {
        responseMimeType: "application/json",
        responseSchema: mindmapSchema,
        temperature: 0.2, 
      },
    });

    // CHUYỂN ĐỔI PDF SANG DẠNG BASE64
    const pdfPart = {
      inlineData: {
        data: buffer.toString("base64"),
        mimeType: "application/pdf"
      }
    };

    const prompt = "Bạn là một trợ lý học tập. Đọc toàn bộ nội dung trong file PDF đính kèm và trích xuất thành sơ đồ tư duy phân cấp.";
    
    // TRUYỀN CẢ PROMPT VÀ FILE PDF GỐC CHO AI
    const aiResult = await model.generateContent([prompt, pdfPart]);
    
    let rawText = aiResult.response.text();
    rawText = rawText.replace(/```json/gi, "").replace(/```/g, "").trim();

    const mindmapJSON = JSON.parse(rawText);

    // LƯU KẾT QUẢ VÀO DATABASE
    document.mindmapData = mindmapJSON;
    await document.save();

    res.status(200).json({
      success: true,
      message: "Tạo sơ đồ tư duy bằng AI thành công!",
      data: mindmapJSON,
      isCached: false 
    });

  } catch (error: any) {
    console.error("generateDocumentMindmap error:", error);
    
    // 1. Xử lý lỗi API quá tải từ Google (Lỗi 503)
    if (error.status === 503 || (error.message && error.message.includes("503"))) {
       res.status(503).json({
         success: false,
         message: "Hệ thống AI của Google hiện đang quá tải. Vui lòng thử lại sau ít phút.",
       });
       return;
    }

    // 2. Xử lý lỗi AI trả về định dạng sai
    if (error instanceof SyntaxError) {
       res.status(500).json({
         success: false,
         message: "Lỗi định dạng dữ liệu từ AI. Vui lòng thử lại.",
       });
       return;
    }

    // 3. Các lỗi còn lại
    res.status(500).json({
      success: false,
      message: "Đã xảy ra lỗi khi tạo sơ đồ tư duy, vui lòng thử lại sau.",
    });
  }
};