import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { useSession } from "@/shared/api/session";
import { renderRoutes } from "@/test/render";
import { DEMO_EMAIL, DEMO_PASSWORD } from "@/test/mocks/data";
import { LoginPage } from "./LoginPage";

const routes = [
  { path: "/entrar", Component: LoginPage },
  { path: "/panel", element: <p>Panel</p> },
];

async function submit(email: string, password: string) {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Email"), email);
  await user.type(screen.getByLabelText("Contraseña"), password);
  await user.click(screen.getByRole("button", { name: "Entrar" }));
}

describe("LoginPage", () => {
  it("con credenciales válidas abre sesión y lleva al panel", async () => {
    renderRoutes(routes, "/entrar");

    await submit(DEMO_EMAIL, DEMO_PASSWORD);

    expect(await screen.findByText("Panel")).toBeInTheDocument();
    expect(useSession.getState().status).toBe("authenticated");
  });

  it("con credenciales inválidas muestra un mensaje genérico", async () => {
    renderRoutes(routes, "/entrar");

    await submit(DEMO_EMAIL, "incorrecta");

    expect(await screen.findByRole("alert")).toHaveTextContent("El email o la contraseña no son correctos.");
    expect(useSession.getState().accessToken).toBeNull();
  });

  it("valida en cliente, marca el campo y le pasa el foco", async () => {
    renderRoutes(routes, "/entrar");

    await userEvent.setup().click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true"));
    expect(screen.getByLabelText("Email")).toHaveFocus();
  });
});
