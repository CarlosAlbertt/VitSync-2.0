import { z } from "zod";

// Falla rápido si el entorno está mal (guía, Anexo C).
const schema = z.object({
  VITE_API_URL: z.string().default(""),
  VITE_APP_ENV: z.enum(["local", "preview", "staging", "production"]).default("local"),
  // Sin backend todavía: en desarrollo, MSW responde con el contrato previsto.
  VITE_API_MOCKS: z.enum(["true", "false"]).default(import.meta.env.DEV ? "true" : "false"),
});

export const env = schema.parse(import.meta.env);
