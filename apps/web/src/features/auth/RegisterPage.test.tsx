import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { DEMO_CODE, DEMO_EMAIL } from "@/test/mocks/data";
import { renderRoutes } from "@/test/render";
import { server } from "@/test/setup";
import { RegisterPage } from "./RegisterPage";
import { VerifyPage } from "./VerifyPage";

const routes = [
  { path: "/registro", Component: RegisterPage },
  { path: "/verificar", Component: VerifyPage },
  { path: "/entrar", element: <p>Pantalla de acceso</p> },
];

async function fillForm(overrides: { nif?: string; email?: string } = {}) {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Nombre"), "Lucía");
  await user.type(screen.getByLabelText("Apellidos"), "Ferrer");
  await user.type(screen.getByLabelText("NIF o NIE"), overrides.nif ?? "12345678Z");
  await user.type(screen.getByLabelText("Fecha de nacimiento"), "1991-03-14");
  await user.type(screen.getByLabelText("Email"), overrides.email ?? "nueva@vitsync.test");
  await user.type(screen.getByLabelText("Contraseña"), "Contraseña-Larga-1");
  await user.click(screen.getByRole("checkbox"));
  await user.click(screen.getByRole("button", { name: "Crear cuenta" }));
  return user;
}

describe("RegisterPage", () => {
  it("registra, verifica el código y lleva al acceso", async () => {
    renderRoutes(routes, "/registro");
    const user = await fillForm();

    await user.type(await screen.findByLabelText("Código"), DEMO_CODE);
    await user.click(screen.getByRole("button", { name: "Verificar" }));

    expect(await screen.findByText("Pantalla de acceso")).toBeInTheDocument();
  });

  it("envía el cuerpo que pide el contrato: NIF normalizado y sin el consentimiento", async () => {
    let body: unknown;
    server.use(
      http.post("*/api/v1/auth/register", async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ email: "nueva@vitsync.test", status: "PENDING_VERIFICATION" }, { status: 201 });
      }),
    );
    renderRoutes(routes, "/registro");

    await fillForm({ nif: "x1234567l" });

    expect(await screen.findByLabelText("Código")).toBeInTheDocument();
    expect(body).toEqual({
      firstName: "Lucía",
      lastName: "Ferrer",
      nif: "X1234567L",
      birthDate: "1991-03-14",
      email: "nueva@vitsync.test",
      password: "Contraseña-Larga-1",
    });
  });

  it("marca un NIF con la letra incorrecta sin llamar a la API", async () => {
    renderRoutes(routes, "/registro");

    await fillForm({ nif: "12345678A" });

    expect(await screen.findByText("Ese NIF no es válido: revisa la letra.")).toBeInTheDocument();
  });

  it("si la cuenta ya existe, no revela si el duplicado es el email o el NIF", async () => {
    renderRoutes(routes, "/registro");

    await fillForm({ email: DEMO_EMAIL });

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("No hemos podido crear la cuenta con esos datos.");
    expect(alert).not.toHaveTextContent(/NIF|email/i);
  });
});
