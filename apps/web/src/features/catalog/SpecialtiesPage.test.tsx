import { act, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { renderRoutes } from "@/test/render";
import { server } from "@/test/setup";
import { SpecialtiesPage } from "./SpecialtiesPage";

const routes = [
  { path: "/especialidades", element: <SpecialtiesPage /> },
  { path: "/otra", element: <p>Otra pantalla</p> },
];

/** Cuenta las peticiones a /api/v1/specialties que llegan al servidor simulado. */
function countRequests() {
  const counter = { value: 0 };
  server.events.on("request:start", ({ request }) => {
    if (new URL(request.url).pathname === "/api/v1/specialties") counter.value += 1;
  });
  return counter;
}

afterEach(() => server.events.removeAllListeners());

describe("Especialidades (VIT-80)", () => {
  it("al volver a la lista se pinta desde caché y se revalida en segundo plano", async () => {
    const requests = countRequests();
    const { router } = renderRoutes(routes, "/especialidades");
    expect((await screen.findAllByText("Cardiología")).length).toBeGreaterThan(0);
    expect(requests.value).toBe(1);

    await act(() => router.navigate("/otra"));
    expect(screen.getByText("Otra pantalla")).toBeInTheDocument();
    await act(() => router.navigate("/especialidades"));

    // Sin esperar a la red: los datos ya están en pantalla...
    expect(screen.getAllByText("Cardiología").length).toBeGreaterThan(0);
    // ...y mientras tanto se piden otra vez para refrescarlos.
    await waitFor(() => expect(requests.value).toBe(2));
  });

  it("busca sin distinguir tildes, como la API real", async () => {
    const user = userEvent.setup();
    renderRoutes(routes, "/especialidades");
    await screen.findAllByText("Cardiología");

    await user.type(screen.getByRole("searchbox", { name: "Buscar especialidad" }), "cardiologia");

    await waitFor(() => expect(screen.queryByText("Dermatología")).not.toBeInTheDocument());
    expect(screen.getAllByText("Cardiología").length).toBeGreaterThan(0);
  });
});
