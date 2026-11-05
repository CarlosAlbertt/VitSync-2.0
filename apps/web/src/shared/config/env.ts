import { z } from "zod";

// Falla rápido si el entorno está mal (guía, Anexo C).
const schema = z.object({
  VITE_API_URL: z.string().default(""),
  VITE_APP_ENV: z.enum(["local", "preview", "staging", "production"]).default("local"),
  // Qué responde la API simulada (MSW):
  //   all      todo; lo normal en desarrollo, sin backend (pnpm dev).
  //   missing  solo lo que el backend aún no tiene; /auth y /me van a la API real (pnpm dev:api).
  //   none     nada; producción.
  VITE_API_MOCKS: z.enum(["all", "missing", "none"]).default(import.meta.env.MODE === "api" ? "missing" : import.meta.env.DEV ? "all" : "none"),
});

export const env = schema.parse(import.meta.env);
