import { env } from "@/shared/config/env";

/** Dónde encontrar el código de un solo uso, según contra qué API se esté trabajando. */
export function codeHint() {
  if (env.VITE_API_MOCKS === "all") return "Demostración: usa 123456.";
  if (env.VITE_APP_ENV === "local") return "En local, el email llega a Mailpit: http://localhost:8025";
  return undefined;
}
