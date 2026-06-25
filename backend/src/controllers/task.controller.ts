import { Response } from "express";
import Task from "../models/task.model";
import { AuthRequest } from "../middlewares/auth.middleware";

export const createTask = async (req: AuthRequest, res: Response) => {
  try {
    const { title, description, dueDate, priority } = req.body;

    if (!req.user) {
      return res.status(401).json({
        message: "Unauthorized",
      });
    }

    if (!title || !dueDate) {
      return res.status(400).json({
        message: "Title and dueDate are required",
      });
    }

    const task = await Task.create({
      userId: req.user.id,
      title,
      description,
      dueDate,
      priority,
    });

    res.status(201).json(task);
  } catch (error) {
    console.error(error);

    res.status(500).json({
      message: "Create task failed",
    });
  }
};

export const getTasks = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        message: "Unauthorized",
      });
    }

    const tasks = await Task.find({
      userId: req.user.id,
    }).sort({
      dueDate: 1,
    });

    res.status(200).json(tasks);
  } catch (error) {
    console.error(error);

    res.status(500).json({
      message: "Get tasks failed",
    });
  }
};

export const getTaskById = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        message: "Unauthorized",
      });
    }

    const task = await Task.findOne({
      _id: req.params.id,
      userId: req.user.id,
    });

    if (!task) {
      return res.status(404).json({
        message: "Task not found",
      });
    }

    res.status(200).json(task);
  } catch (error) {
    console.error(error);

    res.status(500).json({
      message: "Get task failed",
    });
  }
};

export const updateTask = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        message: "Unauthorized",
      });
    }

    const task = await Task.findOneAndUpdate(
      {
        _id: req.params.id,
        userId: req.user.id,
      },
      req.body,
      {
        new: true,
        runValidators: true,
      },
    );

    if (!task) {
      return res.status(404).json({
        message: "Task not found",
      });
    }

    res.status(200).json(task);
  } catch (error) {
    console.error(error);

    res.status(500).json({
      message: "Update task failed",
    });
  }
};

export const deleteTask = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        message: "Unauthorized",
      });
    }

    const task = await Task.findOneAndDelete({
      _id: req.params.id,
      userId: req.user.id,
    });

    if (!task) {
      return res.status(404).json({
        message: "Task not found",
      });
    }

    res.status(200).json({
      message: "Task deleted successfully",
    });
  } catch (error) {
    console.error(error);

    res.status(500).json({
      message: "Delete task failed",
    });
  }
};

export const getTasksByDate = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        message: "Unauthorized",
      });
    }

    const date = req.params.date as string;

    const startDate = new Date(date);
    if (isNaN(startDate.getTime())) {
      return res.status(400).json({
        message: "Invalid date",
      });
    }

    startDate.setHours(0, 0, 0, 0);

    const endDate = new Date(date);
    endDate.setHours(23, 59, 59, 999);

    const tasks = await Task.find({
      userId: req.user.id,
      dueDate: {
        $gte: startDate,
        $lte: endDate,
      },
    }).sort({
      dueDate: 1,
    });

    res.status(200).json(tasks);
  } catch (error) {
    console.error(error);

    res.status(500).json({
      message: "Get tasks by date failed",
    });
  }
};

export const toggleTaskStatus = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        message: "Unauthorized",
      });
    }

    const task = await Task.findOne({
      _id: req.params.id,
      userId: req.user.id,
    });

    if (!task) {
      return res.status(404).json({
        message: "Task not found",
      });
    }

    task.status = task.status === "pending" ? "completed" : "pending";

    await task.save();

    res.status(200).json(task);
  } catch (error) {
    console.error(error);

    res.status(500).json({
      message: "Update task status failed",
    });
  }
};
