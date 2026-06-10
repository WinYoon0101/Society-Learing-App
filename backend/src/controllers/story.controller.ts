import { Response } from "express";
import { AuthRequest } from "../middlewares/auth.middleware";
import Story from "../models/story.model";
import Friend from "../models/friend.model";

// POST /api/stories — Đăng tin mới (ảnh/video đã upload qua Cloudinary middleware)
export const createStory = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const authorId = req.user!.id;
    const { caption } = req.body;
    const file = req.file as any;

    if (!file) {
      res.status(400).json({ success: false, message: "Thiếu file media" });
      return;
    }

    const isVideo = file.mimetype?.includes("video");
    const story = new Story({
      authorId,
      mediaUrl:  file.path,          // Cloudinary URL
      mediaType: isVideo ? "video" : "image",
      caption:   caption || "",
    });
    await story.save();
    await story.populate("authorId", "_id username avatar");

    res.status(201).json({ success: true, data: story });
  } catch (err) {
    console.error("createStory error", err);
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// GET /api/stories — Lấy tin của bản thân + bạn bè (còn hạn)
export const getFeedStories = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.user!.id;

    const friendships = await Friend.find({
      $or: [
        { requester: userId, status: "accepted" },
        { recipient: userId, status: "accepted" },
      ],
    }).lean();

    const friendIds = friendships.map(f =>
      f.requester.toString() === userId ? f.recipient : f.requester
    );
    const allowedIds = [userId, ...friendIds];

    const stories = await Story.find({
      authorId: { $in: allowedIds },
      expiresAt: { $gt: new Date() },
    })
      .sort({ createdAt: -1 })
      .populate("authorId", "_id username avatar")
      .lean();

    // Nhóm theo authorId để FE hiển thị 1 avatar per người
    const grouped: Record<string, any> = {};
    for (const s of stories) {
      const aid = (s.authorId as any)._id.toString();
      if (!grouped[aid]) {
        grouped[aid] = {
          author: s.authorId,
          stories: [],
          latestStoryId: s._id,
          latestMediaUrl: s.mediaUrl,
          latestMediaType: s.mediaType,
        };
      }
      grouped[aid].stories.push(s);
    }

    res.status(200).json({ success: true, data: Object.values(grouped) });
  } catch (err) {
    console.error("getFeedStories error", err);
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// GET /api/stories/:storyId — Lấy 1 story + đánh dấu đã xem
export const viewStory = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { storyId } = req.params;
    const userId = req.user!.id;

    const story = await Story.findById(storyId).populate("authorId", "_id username avatar");
    if (!story) {
      res.status(404).json({ success: false, message: "Không tìm thấy tin" });
      return;
    }
    // Thêm viewer nếu chưa xem
    if (!story.viewers.map(v => v.toString()).includes(userId)) {
      story.viewers.push(userId as any);
      await story.save();
    }
    res.status(200).json({ success: true, data: story });
  } catch (err) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};

// DELETE /api/stories/:storyId — Xóa tin của mình
export const deleteStory = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { storyId } = req.params;
    const userId = req.user!.id;
    const story = await Story.findById(storyId);
    if (!story) {
      res.status(404).json({ success: false, message: "Không tìm thấy tin" });
      return;
    }
    if (story.authorId.toString() !== userId) {
      res.status(403).json({ success: false, message: "Không có quyền xóa" });
      return;
    }
    await story.deleteOne();
    res.status(200).json({ success: true, message: "Đã xóa tin" });
  } catch (err) {
    res.status(500).json({ success: false, message: "Lỗi server" });
  }
};
