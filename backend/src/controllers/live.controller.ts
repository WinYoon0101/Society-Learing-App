import { Request, Response } from "express";
import Live from "../models/live.model";


export const startLive = async (req: Request, res: Response) => {
    try {
        const { liveId, title } = req.body;
        const userId = (req as any).user?.id; 

        const liveSession = await Live.findOneAndUpdate(
            { liveId: liveId }, 
            { 
                hostId: userId, 
                title: title, 
                status: 'streaming',
                createdAt: new Date() 
            },
            { 
                upsert: true, 
                new: true,    
                runValidators: true 
            }
        ).populate("hostId", "username avatar email"); // QUAN TRỌNG: Phải có dòng này

      
        res.status(201).json({ success: true, data: liveSession });
    } catch (err: any) {
        console.error("BACKEND_LIVE_ERROR:", err.message);
        res.status(500).json({ 
            success: false, 
            error: "Lỗi tạo phiên live", 
            detail: err.message 
        });
    }
};

// 2. Lấy danh sách live 
export const getActiveLives = async (req: Request, res: Response) => {
    try {
        const lives = await Live.find({ status: 'streaming' })
            .populate("hostId", "username avatar email") 
            .sort({ createdAt: -1 });

        res.status(200).json({ success: true, data: lives });
    } catch (err: any) {
        res.status(500).json({ success: false, error: "Lỗi lấy danh sách live" });
    }
};

// 3. Kết thúc live
export const endLive = async (req: Request, res: Response) => {

    try {
        const { liveId } = req.params;

        if (!liveId) {
            return res.status(400).json({ success: false, message: "Thiếu liveId" });
        }

        const updatedLive = await Live.findOneAndUpdate(
            { liveId: liveId }, 
            { status: 'ended' }, 
            { returnDocument: 'after' } 
        );

        if (!updatedLive) {
            return res.status(404).json({ success: false, message: "Phòng live không tồn tại" });
        }
        res.status(200).json({ success: true, message: "Live đã kết thúc" });
    } catch (err: any) {
        res.status(500).json({ success: false, error: err.message });
    }
};