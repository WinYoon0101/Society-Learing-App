import swaggerUi from "swagger-ui-express";
import fs from "fs";
import path from "path";
import yaml from "yamljs";
import { Express } from "express";


const options = {
  definition: {
    openapi: "3.0.0",
    info: {
      title: "Society Mobile API",
      version: "1.0.0",
      description: "API documentation cho ứng dụng Society",
    },
    components: {
      securitySchemes: {
        bearerAuth: {
          type: "http",
          scheme: "bearer",
          bearerFormat: "JWT",
          description: "Nhập JWT token: Bearer {token}",
        },
      },
      schemas: {
        User: {
          type: "object",
          properties: {
            id: { type: "string" },
            username: { type: "string" },
            email: { type: "string" },
            dateOfBirth: { type: "string", nullable: true },
            gender: { type: "string", nullable: true },
            avatar: { type: "string", nullable: true },
            bio: { type: "string", nullable: true },
            isVerified: { type: "boolean" },
            createdAt: { type: "string", format: "date-time" },
          },
        },
      },
    },
    security: [{ bearerAuth: [] }],
  },
  apis: ["./src/routes/*.ts"],
};

const swaggerFile = path.join(__dirname, "swagger.yaml");
const swaggerDocument = yaml.load(swaggerFile);


const swaggerDocs = (app: Express) => {
  app.use("/docs", swaggerUi.serve, swaggerUi.setup(swaggerDocument));

  console.log("📘 Swagger docs: http://localhost:3000/docs");
};

export default swaggerDocs;

