import { loadEnvConfig } from "@next/env";

const projectDir = process.cwd(); // Root directory of your project
loadEnvConfig(projectDir, process.env.NODE_ENV !== "production");
