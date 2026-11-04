import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll } from "vitest";
import { useSession } from "@/shared/api/session";
import { handlers, resetMocks } from "./mocks/handlers";

// jsdom trae su propio AbortSignal y el Request de Node lo rechaza al navegar con
// React Router. Solo afecta a los tests: en el navegador no existe ese choque.
const NativeRequest = globalThis.Request;
globalThis.Request = class extends NativeRequest {
  constructor(input: RequestInfo | URL, init: RequestInit = {}) {
    super(input, { ...init, signal: undefined });
  }
};

export const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));

afterEach(() => {
  cleanup();
  server.resetHandlers();
  resetMocks();
  useSession.setState({ status: "anonymous", accessToken: null, user: null });
});

afterAll(() => server.close());
