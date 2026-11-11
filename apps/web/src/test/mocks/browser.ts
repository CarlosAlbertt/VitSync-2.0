import { setupWorker } from "msw/browser";
import { handlers, missingHandlers } from "./handlers";

const RELOADED = "msw-reloaded";

/**
 * "all": todo simulado, sin backend.
 * "missing": identidad (/auth, /me) y catálogo los responde el backend real a través del proxy
 * de Vite; el worker solo simula lo que la API aún no tiene (agenda, citas, informes).
 */
export async function startMocks(mode: "all" | "missing") {
  const worker = setupWorker(...(mode === "all" ? handlers : missingHandlers));
  await worker.start({ onUnhandledRequest: "bypass", quiet: true });

  // Una recarga forzada (Ctrl+F5) carga la página sin service worker: las peticiones simuladas
  // irían al backend y las pantallas sin endpoint fallarían. Se recarga una vez, ya con el worker.
  if (!navigator.serviceWorker.controller && !sessionStorage.getItem(RELOADED)) {
    sessionStorage.setItem(RELOADED, "1");
    location.reload();
    await new Promise(() => {}); // no se pinta nada hasta que llegue la recarga
  }
  sessionStorage.removeItem(RELOADED);
}
