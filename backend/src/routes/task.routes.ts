import { Router } from "express";
import { authenticate } from "../middlewares/auth.middleware";
import {
  createTask,
  getTasks,
  getTaskById,
  updateTask,
  deleteTask,
  getTasksByDate,
  toggleTaskStatus,
} from "../controllers/task.controller";

const router = Router();
router.use(authenticate);

router.post("/", createTask);
router.get("/", getTasks);
router.get("/date/:date", getTasksByDate);
router.get("/:id", getTaskById);
router.put("/:id", updateTask);
router.patch("/:id/status", toggleTaskStatus);
router.delete("/:id", deleteTask);

export default router;
