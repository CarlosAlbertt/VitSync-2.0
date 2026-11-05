import { setupWorker } from "msw/browser";
import { handlers, missingHandlers } from "./handlers";

/**
 * "all": todo simulado, sin backend.
 * "missing": la identidad (/auth, /me) la responde el backend real a través del proxy de Vite;
 * el worker solo simula lo que la API aún no tiene.
 */
export function startMocks(mode: "all" | "missing") {
  const worker = setupWorker(...(mode === "all" ? handlers : missingHandlers));
  return worker.start({ onUnhandledRequest: "bypass", quiet: true });
}
