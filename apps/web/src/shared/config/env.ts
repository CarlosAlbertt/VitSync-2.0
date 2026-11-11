import { z } from "zod";

// Falla rápido si el entorno está mal (guía, Anexo C).
const schema = z.object({
  VITE_API_URL: z.string().default(""),
  VITE_APP_ENV: z.enum(["local", "preview", "staging", "production"]).default("local"),
  // Qué responde la API simulada (MSW):
  //   missing  solo lo que el backend aún no tiene; lo demás va a la API real con la base de
  //            datos de demostración (pnpm dev, lo normal en desarrollo).
  //   all      todo, sin backend (pnpm dev:mocks, y los tests).
  //   none     nada; producción.
  VITE_API_MOCKS: z
    .enum(["all", "missing", "none"])
    .default(["mocks", "test"].includes(import.meta.env.MODE) ? "all" : import.meta.env.DEV ? "missing" : "none"),
});

export const env = schema.parse(import.meta.env);
