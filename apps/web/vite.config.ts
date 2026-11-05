import { fileURLToPath, URL } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// En modo API (pnpm dev:api) el navegador solo habla con Vite, y Vite reenvía /api al backend.
// Al ser el mismo origen no hace falta CORS, y la cookie del refresh (SameSite=Lax) funciona sin más.
const proxy = { "/api": "http://localhost:8080" };

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  server: { port: 5173, proxy },
  preview: { proxy },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    css: false,
  },
});
