import express from "express";
import cors from "cors";
import routes from "./routes";
import swaggerDocs from "./swagger/swagger";

const app = express();

app.use(cors());
app.use(express.json());

app.use("/api", routes);

swaggerDocs(app);

export default app;