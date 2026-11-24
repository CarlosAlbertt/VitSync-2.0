import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { useSession } from "@/shared/api/session";
import { useTheme } from "@/shared/lib/theme";
import { demoUser } from "@/test/mocks/data";
import { renderRoutes } from "@/test/render";
import { AppLayout } from "./layouts";

const PANEL = ["", "citas", "videoconsulta", "mensajes", "informes", "medicacion", "historial", "seguimiento", "documentos", "pagos", "familia", "perfil", "seguridad", "notificaciones"];

const routes = [
  {
    Component: AppLayout,
    children: [
      { path: "/reservar", element: <h1>Reservar</h1> },
      ...PANEL.map((path) => ({ path: `/panel/${path}`, element: <h1>Pantalla {path || "inicio"}</h1> })),
    ],
  },
];

const sidebar = () => screen.getByRole("navigation", { name: "Tu área" });

// ScrollRestoration llama a scrollTo, que jsdom no implementa.
beforeAll(() => {
  window.scrollTo = vi.fn() as unknown as typeof window.scrollTo;
});
beforeEach(() => useSession.setState({ status: "authenticated", accessToken: "token", user: demoUser }));
afterEach(() => useTheme.getState().setPreference("system"));

describe("Navegación del área del paciente (VIT-111)", () => {
  it("muestra siete destinos de primer nivel y deja la cuenta fuera de la barra", () => {
    renderRoutes(routes, "/panel");
    const nav = within(sidebar());

    const topLevel = ["Inicio", "Citas", "Mensajes", "Mi salud", "Documentos", "Pagos y seguro", "Familia"];
    for (const name of topLevel) expect(nav.getByText(name)).toBeInTheDocument();
    for (const name of ["Videoconsulta", "Mis datos", "Seguridad y privacidad", "Notificaciones"]) {
      expect(nav.queryByText(name)).not.toBeInTheDocument();
    }
    expect(nav.getByRole("link", { name: "Inicio" })).toHaveAttribute("aria-current", "page");
  });

  it("Mi salud empieza cerrado fuera de sus pantallas y se abre al pulsarlo", async () => {
    const user = userEvent.setup();
    renderRoutes(routes, "/panel");
    const toggle = within(sidebar()).getByRole("button", { name: "Mi salud" });

    expect(toggle).toHaveAttribute("aria-expanded", "false");
    expect(within(sidebar()).queryByRole("link", { name: "Medicación" })).not.toBeInTheDocument();

    await user.click(toggle);

    expect(toggle).toHaveAttribute("aria-expanded", "true");
    await user.click(within(sidebar()).getByRole("link", { name: "Medicación" }));
    expect(await screen.findByRole("heading", { name: "Pantalla medicacion" })).toBeInTheDocument();
    expect(within(sidebar()).getByRole("link", { name: "Medicación" })).toHaveAttribute("aria-current", "page");
  });

  it("al entrar directamente en una pantalla de Mi salud el grupo aparece abierto", () => {
    renderRoutes(routes, "/panel/historial");

    expect(within(sidebar()).getByRole("button", { name: "Mi salud" })).toHaveAttribute("aria-expanded", "true");
    expect(within(sidebar()).getByRole("link", { name: "Historial clínico" })).toHaveAttribute("aria-current", "page");
  });

  it("la sala de videoconsulta cuenta como Citas", () => {
    renderRoutes(routes, "/panel/videoconsulta");

    expect(within(sidebar()).getByRole("link", { name: "Citas" })).toHaveAttribute("aria-current", "page");
  });

  it("la cuenta, los avisos y la apariencia están en el menú de usuario", async () => {
    const user = userEvent.setup();
    renderRoutes(routes, "/panel");

    await user.click(screen.getByRole("button", { name: /Menú de tu cuenta/ }));

    expect(screen.getByRole("link", { name: "Mis datos" })).toHaveAttribute("href", "/panel/perfil");
    expect(screen.getByRole("link", { name: "Seguridad y privacidad" })).toHaveAttribute("href", "/panel/seguridad");
    expect(screen.getByRole("link", { name: "Preferencias de avisos" })).toHaveAttribute("href", "/panel/notificaciones");

    const appearance = within(screen.getByRole("group", { name: "Apariencia" }));
    await user.click(appearance.getByRole("button", { name: "Oscuro" }));
    expect(appearance.getByRole("button", { name: "Oscuro" })).toHaveAttribute("aria-pressed", "true");
    expect(document.documentElement.dataset.theme).toBe("dark");
  });

  it("la ayuda telefónica se puede pulsar para llamar", () => {
    renderRoutes(routes, "/panel");

    expect(within(sidebar().parentElement!).getByRole("link", { name: "900 000 000" })).toHaveAttribute("href", "tel:900000000");
  });
});
