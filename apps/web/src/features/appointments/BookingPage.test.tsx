import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { useSession } from "@/shared/api/session";
import { demoUser } from "@/test/mocks/data";
import { renderRoutes } from "@/test/render";
import { server } from "@/test/setup";
import { BookingPage } from "./BookingPage";

const routes = [
  { path: "/reservar", Component: BookingPage },
  { path: "/panel", element: <p>Panel</p> },
];

const SLOT = "2030-03-04T09:20:00.000Z";

async function reachConfirmation() {
  const user = userEvent.setup();
  await user.click(await screen.findByRole("button", { name: /Cardiología/ }));
  await user.click(await screen.findByRole("button", { name: /Marta Bellver Ros/ }));
  const hours = await screen.findByRole("list", { name: "Horas disponibles" });
  await user.click(within(hours).getAllByRole("button")[0]!);
  await screen.findByRole("heading", { name: "Revisa y confirma" });
  return user;
}

describe("BookingPage", () => {
  beforeEach(() => {
    useSession.setState({ status: "authenticated", accessToken: `mock.${demoUser.email}`, user: demoUser });
    server.use(http.get("*/api/v1/doctors/:id/slots", () => HttpResponse.json([{ startsAt: SLOT, durationMin: 20 }])));
  });

  it("reserva recorriendo los cuatro pasos y vuelve al panel", async () => {
    renderRoutes(routes, "/reservar");
    const user = await reachConfirmation();

    await user.click(screen.getByRole("button", { name: "Confirmar cita" }));

    expect(await screen.findByText("Panel")).toBeInTheDocument();
  });

  it("si el hueco se ocupa mientras elijo, avisa y vuelve al calendario", async () => {
    server.use(
      http.post("*/api/v1/appointments", () =>
        HttpResponse.json({ status: 409, code: "APPOINTMENT_SLOT_TAKEN" }, { status: 409 }),
      ),
    );
    renderRoutes(routes, "/reservar");
    const user = await reachConfirmation();

    await user.click(screen.getByRole("button", { name: "Confirmar cita" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Alguien acaba de reservar ese hueco");
    expect(screen.getByRole("list", { name: "Horas disponibles" })).toBeInTheDocument();
  });

  it("sin centro seleccionable en una videoconsulta, no pide centro", async () => {
    renderRoutes(routes, "/reservar");
    const user = await reachConfirmation();

    expect(screen.getByLabelText("Centro")).toBeInTheDocument();
    await user.click(screen.getByLabelText("Por vídeo"));

    expect(screen.queryByLabelText("Centro")).not.toBeInTheDocument();
  });
});
