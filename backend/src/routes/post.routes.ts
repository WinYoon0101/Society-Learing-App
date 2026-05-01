import express from 'express';
import { createPost, getFeed, deletePost } from '../controllers/post.controller';
import { authenticate } from '../middlewares/auth.middleware'; // Ktra đăng nhập
import { uploadImages } from '../middlewares/upload.middleware';  

const router = express.Router();
router.post('/create', authenticate, uploadImages, createPost);
router.get('/feed', authenticate, getFeed);
router.delete('/:id', authenticate, deletePost);

export default router;