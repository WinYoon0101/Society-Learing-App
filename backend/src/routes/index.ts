import { Router } from "express";
import { getHello } from "../controllers/example.controller";

const router = Router();

/**
 * @swagger
 * /api/hello:
 *   get:
 *     summary: Hello API
 *     responses:
 *       200:
 *         description: Success
 */
router.get("/hello", getHello);

export default router;