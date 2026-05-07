import { Router } from 'express';
import { toggleReaction, getReactionsOfTarget } from '../controllers/reaction.controller';
import { authenticate } from '../middlewares/auth.middleware'; 

const router = Router();

router.post('/toggle', authenticate, toggleReaction);
router.get('/:targetId', authenticate, getReactionsOfTarget); // Mở đường GET danh sách

export default router;