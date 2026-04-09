import dotenv from "dotenv";

dotenv.config();

export const PORT = process.env.PORT || 3000;
export const NODE_ENV = process.env.NODE_ENV || "development";

// MongoDB
export const MONGO_URI =
  process.env.MONGO_URI_ATLAS || "mongodb://localhost:27017/vibely";

// JWT
export const JWT_SECRET = process.env.JWT_SECRET || "vibely_jwt_secret_key";
export const JWT_REFRESH_SECRET =
  process.env.JWT_REFRESH_SECRET || "vibely_jwt_refresh_secret_key";
export const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || "1d";
export const JWT_REFRESH_EXPIRES_IN =
  process.env.JWT_REFRESH_EXPIRES_IN || "7d";

// Cloudinary
export const CLOUDINARY_NAME = process.env.CLOUDINARY_NAME || "";
export const CLOUDINARY_API_KEY = process.env.CLOUDINARY_API_KEY || "";
export const CLOUDINARY_API_SECRET = process.env.CLOUDINARY_API_SECRET || "";