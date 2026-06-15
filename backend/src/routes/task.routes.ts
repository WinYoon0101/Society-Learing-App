import express from "express";
import {
  createTask,
  getTasks,
  deleteTask,
  updateTask,
  getTasksByDate,
} from "../controllers/task.controller";

const router = express.Router();

router.post("/", createTask);
router.get("/", getTasks);
router.delete("/:id", deleteTask);
router.put("/:id", updateTask);
router.get("/date/:date", getTasksByDate);

export default router;
