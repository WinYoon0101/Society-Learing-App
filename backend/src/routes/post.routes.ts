import express from 'express';
import { 
    createPost, 
    getFeed, 
    deletePost, 
    toggleSavePost, 
    getSavedPosts,
    getMyPosts,
    getPostsByUser,
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
router.get('/user/:userId', authenticate, getPostsByUser);

export default router;