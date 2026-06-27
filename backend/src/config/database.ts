import mongoose from "mongoose";
import { MONGO_URI } from "./env";
import dns from "node:dns";

dns.setServers(["8.8.8.8", "8.8.4.4"]);

const connectDB = async (): Promise<void> => {
  try {
    const conn = await mongoose.connect(MONGO_URI);
    console.log(`✅ MongoDB Connected`);
  } catch (error: any) {
    console.error(`❌ MongoDB Connection Error: ${error.message}`);
    process.exit(1);
  }
};

export default connectDB;
