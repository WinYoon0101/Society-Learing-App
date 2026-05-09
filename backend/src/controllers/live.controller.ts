import { Request, Response } from "express";
import Live from "../models/live.model";

// Khi bắt đầu phát
export const startLive = async (req: Request, res: Response) => {
    try {
        const { liveId, title } = req.body;
        const userId = (req as any).user?.id;

        const newLive = new Live({
            hostId: userId,
            liveId,
            title,
            status: 'streaming'
        });

        await newLive.save();
        res.status(201).json({ success: true, data: newLive });
    } catch (err: any) {
        res.status(500).json({ error: "Lỗi tạo phiên live", detail: err.message });
    }
};

// Lấy danh sách các phòng đang live để hiển thị lên Feed
export const getActiveLives = async (req: Request, res: Response) => {
    try {
        const lives = await Live.find({ status: 'streaming' })
            .populate("hostId", "username avatar") // Lấy thêm info người live
            .sort({ createdAt: -1 });

        res.status(200).json({ success: true, data: lives });
    } catch (err: any) {
        res.status(500).json({ error: "Lỗi lấy danh sách live" });
    }
};