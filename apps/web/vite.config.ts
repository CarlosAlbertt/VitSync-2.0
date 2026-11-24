import { fileURLToPath, URL } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import type { ProxyOptions } from "vite";
import { defineConfig } from "vitest/config";

// Con pnpm dev el navegador solo habla con Vite, y Vite reenvía /api al backend.
// Al ser el mismo origen no hace falta CORS, y la cookie del refresh (SameSite=Lax) funciona sin más.
const api: ProxyOptions = {
  target: "http://localhost:8080",
  // Si la API no está arrancada, Vite respondería un 500 que parece un fallo del backend. Se responde
  // un 503 con el mismo formato de error que la API (RFC 9457) y lo que hay que hacer.
  configure(server) {
    server.on("error", (_error, _request, response) => {
      if (!("writeHead" in response) || response.headersSent) return;
      response.writeHead(503, { "Content-Type": "application/problem+json" });
      response.end(
        JSON.stringify({
          status: 503,
          title: "Backend unavailable",
          code: "BACKEND_UNAVAILABLE",
          detail: "La API no responde en localhost:8080. Arráncala con make api, o usa pnpm dev:mocks.",
        }),
      );
    });
  },
};
const proxy = { "/api": api };

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
