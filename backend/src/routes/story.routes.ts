import { Router } from "express";
import { authenticate } from "../middlewares/auth.middleware";
import { uploadFile } from "../middlewares/upload.middleware";
import { createStory, getFeedStories, viewStory, deleteStory } from "../controllers/story.controller";

const router = Router();
router.use(authenticate);

router.get("/",            getFeedStories);           // GET  /api/stories
router.post("/",           uploadFile, createStory);  // POST /api/stories  (field: "file")
router.get("/:storyId",    viewStory);                // GET  /api/stories/:id
router.delete("/:storyId", deleteStory);              // DELETE /api/stories/:id

export default router;
