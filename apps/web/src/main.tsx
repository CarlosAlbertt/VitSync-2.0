import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "@/app/App";
import { env } from "@/shared/config/env";
import "@/app/styles.css";

async function start() {
  if (env.VITE_API_MOCKS !== "none") {
    const { startMocks } = await import("@/test/mocks/browser");
    await startMocks(env.VITE_API_MOCKS);
  }

  createRoot(document.getElementById("root")!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
}

void start();
