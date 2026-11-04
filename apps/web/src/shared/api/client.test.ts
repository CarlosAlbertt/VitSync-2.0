import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { server } from "@/test/setup";
import { demoUser } from "@/test/mocks/data";
import { ApiError, request } from "./client";
import { useSession } from "./session";

const API = "*/api/v1";

describe("request", () => {
  it("convierte un ProblemDetail en ApiError con su code", async () => {
    server.use(
      http.get(`${API}/x`, () =>
        HttpResponse.json({ status: 409, detail: "taken", code: "APPOINTMENT_SLOT_TAKEN", traceId: "abc" }, { status: 409 }),
      ),
    );

    const error = await request("/x").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 409, code: "APPOINTMENT_SLOT_TAKEN", traceId: "abc" });
  });

  it("con N peticiones en 401 solo emite un refresh y las reintenta todas", async () => {
    let refreshes = 0;
    server.use(
      http.post(`${API}/auth/refresh`, () => {
        refreshes += 1;
        return HttpResponse.json({ accessToken: "nuevo", user: demoUser });
      }),
      http.get(`${API}/privado`, ({ request: req }) =>
        req.headers.get("Authorization") === "Bearer nuevo"
          ? HttpResponse.json({ ok: true })
          : HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );

    const results = await Promise.all([request("/privado"), request("/privado"), request("/privado")]);

    expect(results).toEqual([{ ok: true }, { ok: true }, { ok: true }]);
    expect(refreshes).toBe(1);
    expect(useSession.getState().accessToken).toBe("nuevo");
  });

  it("si el refresh falla, cierra la sesión y propaga el 401", async () => {
    useSession.setState({ status: "authenticated", accessToken: "caducado", user: demoUser });
    server.use(http.get(`${API}/privado`, () => HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 })));

    await expect(request("/privado")).rejects.toMatchObject({ status: 401 });

    expect(useSession.getState().status).toBe("anonymous");
  });
});
