import { Router } from "express";
import { body } from "express-validator";
import {
  register,
  login,
  refreshToken,
  logout,
  getMe,
  googleLogin,
} from "../controllers/auth.controller";
import { authenticate } from "../middlewares/auth.middleware";
import { handleValidationErrors } from "../middlewares/validate.middleware";
import { resetPassword, sendOtp, verifyOtp } from "../controllers/otp.controller";

const router = Router();

// ─── Validators ───────────────────────────────────────────────────────────────

const registerValidators = [
  body("username")
    .trim()
    .notEmpty()
    .withMessage("Tên người dùng (Họ tên) là bắt buộc")
    .isLength({ min: 2, max: 100 })
    .withMessage("Tên người dùng phải từ 2-100 ký tự"),

  body("email")
    .trim()
    .notEmpty()
    .withMessage("Email là bắt buộc")
    .isEmail()
    .withMessage("Email không hợp lệ")
    .normalizeEmail(),

  body("password")
    .notEmpty()
    .withMessage("Mật khẩu là bắt buộc")
    .isLength({ min: 6 })
    .withMessage("Mật khẩu phải có nhất 6 ký tự"),

  body("dateOfBirth")
    .optional({ checkFalsy: true })
    .isString(),

  body("gender")
    .optional({ checkFalsy: true })
    .isString(),
];

const loginValidators = [
  body("email")
    .trim()
    .notEmpty()
    .withMessage("Email là bắt buộc")
    .isEmail()
    .withMessage("Email không hợp lệ"),

  body("password").notEmpty().withMessage("Mật khẩu là bắt buộc"),
];

const refreshTokenValidators = [
  body("refreshToken")
    .notEmpty()
    .withMessage("Refresh token là bắt buộc"),
];

// ─── Routes ───────────────────────────────────────────────────────────────────


router.post(
  "/register",
  registerValidators,
  handleValidationErrors,
  register
);


router.post("/login", loginValidators, handleValidationErrors, login);


router.post(
  "/refresh-token",
  refreshTokenValidators,
  handleValidationErrors,
  refreshToken
);


router.post("/logout", authenticate, logout);


router.get("/me", authenticate, getMe);

router.post("/send-otp", sendOtp);
router.post("/verify-otp", verifyOtp);
router.post("/reset-password", resetPassword);
router.post("/google-login", googleLogin);

export default router;
