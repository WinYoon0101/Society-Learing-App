import express from 'express';
import {
    createPost,
    getFeed,
    deletePost,
    toggleSavePost,
    getSavedPosts,
    getMyPosts,
    getPostsByUser,
    getPostById,
    approvePost,
    rejectPost,
} from '../controllers/post.controller';
import { authenticate } from '../middlewares/auth.middleware';
import { uploadImages } from '../middlewares/upload.middleware';

const router = express.Router();

router.post('/create', authenticate, uploadImages, createPost);
router.get('/feed', authenticate, getFeed);
router.get('/me', authenticate, getMyPosts);
router.get('/my/saved', authenticate, getSavedPosts);
router.post('/:id/save', authenticate, toggleSavePost);
router.delete('/:id', authenticate, deletePost);
router.patch('/:id/approve', authenticate, approvePost);
router.patch('/:id/reject', authenticate, rejectPost);
router.get('/user/:userId', authenticate, getPostsByUser);
router.get('/:id', authenticate, getPostById);
export default router;