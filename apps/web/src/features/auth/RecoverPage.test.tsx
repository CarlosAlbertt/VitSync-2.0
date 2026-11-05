import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { DEMO_CODE, DEMO_EMAIL } from "@/test/mocks/data";
import { renderRoutes } from "@/test/render";
import { server } from "@/test/setup";
import { RecoverPage } from "./RecoverPage";

const routes = [
  { path: "/recuperar", Component: RecoverPage },
  { path: "/entrar", element: <p>Pantalla de acceso</p> },
];

async function requestCode() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Email"), DEMO_EMAIL);
  await user.click(screen.getByRole("button", { name: "Enviar código" }));
  return user;
}

describe("RecoverPage", () => {
  it("cambia la contraseña con el código y manda la nueva como newPassword", async () => {
    let body: unknown;
    server.use(
      http.post("*/api/v1/auth/password-reset", async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderRoutes(routes, "/recuperar");

    const user = await requestCode();
    await user.type(await screen.findByLabelText("Código"), DEMO_CODE);
    await user.type(screen.getByLabelText("Contraseña nueva"), "una frase bastante larga");
    await user.click(screen.getByRole("button", { name: "Cambiar contraseña" }));

    expect(await screen.findByText("Pantalla de acceso")).toBeInTheDocument();
    expect(body).toEqual({ email: DEMO_EMAIL, code: DEMO_CODE, newPassword: "una frase bastante larga" });
  });

  it("tras quemar el código ofrece pedir otro", async () => {
    renderRoutes(routes, "/recuperar");

    const user = await requestCode();
    const code = await screen.findByLabelText("Código");
    await user.type(screen.getByLabelText("Contraseña nueva"), "una frase bastante larga");
    // La API simulada, como la real, quema el código tras 5 fallos.
    for (let i = 0; i < 6; i++) {
      await user.clear(code);
      await user.type(code, "000000");
      await user.click(screen.getByRole("button", { name: "Cambiar contraseña" }));
    }

    await user.click(await screen.findByRole("button", { name: "Pedir otro código" }));
    expect(await screen.findByRole("button", { name: "Enviar código" })).toBeInTheDocument();
  });
});
