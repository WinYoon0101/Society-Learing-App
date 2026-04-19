import {Response} from "express";
import { AuthRequest } from '../middlewares/auth.middleware';
import Post from '../models/post.model';
import Media from '../models/media.model';
import Group from '../models/group.model';

//API đăng bài
export const createPost = async (req: AuthRequest, res: Response) =>{
    try {
        const{content, privacy, groupId} = req.body;
        // Lấy id người đăng
        const authorId = req.user?.id;
        // Tạo 1 post mới
        const newPost = new Post({
            authorId: authorId,
            groupId: groupId || null,
            content: content,
            privacy: privacy || "public",
        });
        // bắt db lưu lại và chờ lưu
        const savePost = await newPost.save();
        // Kiểm tra xem có đăng ảnh/video không
        if(req.files && Array.isArray(req.files) && req.files.length >0)
        {   
            // Biển dổi file ban đầu => mảng Media
            const mediaDocument = req.files.map((file: any) =>{
                // kt xem là ảnh hay video
                const isVideo = file.mimetype.includes('video');

                return{
                    userId: authorId,
                    url: file.path,
                    fileType: isVideo ? 'video' : 'image',
                    sourceType: 'post',
                    targetId: savePost._id
                };
            }
            
        )
         await Media.insertMany(mediaDocument);
        };

        res.status(201).json({
            sucess: true,
            message: "Đăng bài thành công",
            PostId: savePost._id,
        });
        
    }
    catch(error){
        console.error("Lỗi đăng bài", error);
        res.status(500).json({
            success: false,
            message: "Lỗi hệ thống khi đăng bài"
        })
    }
};

//API Lấy bảng tin

export const getFeed = async (req: AuthRequest, res: Response) =>{
    try{
    const feed = await Post.find({privacy:'public', groupId:null})
    // lấy bài đăng mới nhất đưa lên đầu
    .sort({createAt: -1})
    .populate('authorId', 'username avatar') 
    .populate('mediaFiles')
    .limit(20)

    // tra danh sach bai viet
    res.status(200).json({
        success: true,
        data: getFeed,
    });
} catch(error){
    res.status(500).json({
        success: false,
        message: "Loi khong the load bang tin"
    })
}
}

// API xoa bai
export const deletePost = async (req: AuthRequest, res: Response) => {
    try {
        // Lấy ID bài viết từ trên thanh UR
        const postId = req.params.id; 
        const userId = req.user?.id;

        // Tìm bài viết 
        const post = await Post.findById(postId);
        
        if (!post) {
            return res.status(404).json({ success: false, message: "Không tìm thấy bài viết" }); 
        }
        let hasPermission = false;
        // Bài viết chính chủ
        if (post.authorId.toString() === userId) {
            hasPermission = true; 
}       // Bài đăng trong group và khp chính chủ
        if (!hasPermission && post.groupId) {
            const group = await Group.findById(post.groupId);
        if (group && group.creatorId.toString() === userId) {
        hasPermission = true;
    }

    if (!hasPermission) {
    return res.status(403).json({ success: false, message: "Bạn không có quyền xóa bài này!" });
}
}
        await Media.deleteMany({ targetId: postId });
        await Post.findByIdAndDelete(postId);

        res.status(200).json({ success: true, message: "Đã xóa bài viết!" });
    } catch (error) {
        res.status(500).json({ success: false, message: "Lỗi hệ thống khi xóa bài" });
    }
};