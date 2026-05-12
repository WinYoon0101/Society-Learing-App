import express from 'express';
import { 
    createPost, 
    getFeed, 
    deletePost, 
    toggleSavePost, 
    getSavedPosts 
} from '../controllers/post.controller';
import { authenticate } from '../middlewares/auth.middleware'; // Ktra đăng nhậpg
import { uploadImages } from '../middlewares/upload.middleware';  

const router = express.Router();

// Các API hiện tại
router.post('/create', authenticate, uploadImages, createPost);
router.get('/feed', authenticate, getFeed);
router.get('/my/saved', authenticate, getSavedPosts);
router.post('/:id/save', authenticate, toggleSavePost);
router.delete('/:id', authenticate, deletePost);

export default router;