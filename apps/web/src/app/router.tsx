import type { ComponentType } from "react";
import { createBrowserRouter, type RouteObject } from "react-router";
import { Loading } from "@/shared/ui/feedback";
import { AppLayout, AuthLayout, PublicLayout, RequireAuth } from "./layouts";
import { NotFoundPage } from "./NotFoundPage";

// Cada pantalla es un trozo aparte: la portada no carga el código del panel.
const page = (load: () => Promise<ComponentType>): Pick<RouteObject, "lazy"> => ({
  lazy: async () => ({ Component: await load() }),
});

export const routes: RouteObject[] = [
  {
    Component: PublicLayout,
    HydrateFallback: () => <Loading />,
    children: [
      { index: true, ...page(async () => (await import("@/features/home/HomePage")).HomePage) },
      { path: "especialidades", ...page(async () => (await import("@/features/catalog/SpecialtiesPage")).SpecialtiesPage) },
      { path: "profesionales", ...page(async () => (await import("@/features/catalog/DoctorsPage")).DoctorsPage) },
      { path: "contacto", ...page(async () => (await import("@/features/contact/ContactPage")).ContactPage) },
      { path: "*", Component: NotFoundPage },
    ],
  },
  {
    // Entrar y registrarse van a pantalla completa, sin la cabecera pública.
    Component: AuthLayout,
    HydrateFallback: () => <Loading />,
    children: [
      { path: "entrar", ...page(async () => (await import("@/features/auth/LoginPage")).LoginPage) },
      { path: "registro", ...page(async () => (await import("@/features/auth/RegisterPage")).RegisterPage) },
      { path: "verificar", ...page(async () => (await import("@/features/auth/VerifyPage")).VerifyPage) },
      { path: "recuperar", ...page(async () => (await import("@/features/auth/RecoverPage")).RecoverPage) },
    ],
  },
  {
    // El área del paciente tiene su propio marco de aplicación.
    Component: RequireAuth,
    HydrateFallback: () => <Loading />,
    children: [
      {
        Component: AppLayout,
        children: [
          { path: "reservar", ...page(async () => (await import("@/features/appointments/BookingPage")).BookingPage) },
          {
            path: "panel",
            children: [
              { index: true, ...page(async () => (await import("@/features/dashboard/DashboardPage")).DashboardPage) },
              { path: "citas", ...page(async () => (await import("@/features/appointments/AppointmentsPage")).AppointmentsPage) },
              { path: "videoconsulta", ...page(async () => (await import("@/features/appointments/VideoPage")).VideoPage) },
              { path: "mensajes", ...page(async () => (await import("@/features/messages/MessagesPage")).MessagesPage) },
              { path: "informes", ...page(async () => (await import("@/features/reports/ReportsPage")).ReportsPage) },
              { path: "medicacion", ...page(async () => (await import("@/features/health/MedicationPage")).MedicationPage) },
              { path: "historial", ...page(async () => (await import("@/features/health/HistoryPage")).HistoryPage) },
              { path: "seguimiento", ...page(async () => (await import("@/features/health/TrackingPage")).TrackingPage) },
              { path: "documentos", ...page(async () => (await import("@/features/documents/DocumentsPage")).DocumentsPage) },
              { path: "familia", ...page(async () => (await import("@/features/family/FamilyPage")).FamilyPage) },
              { path: "pagos", ...page(async () => (await import("@/features/billing/BillingPage")).BillingPage) },
              { path: "perfil", ...page(async () => (await import("@/features/profile/ProfilePage")).ProfilePage) },
              { path: "seguridad", ...page(async () => (await import("@/features/settings/SecurityPage")).SecurityPage) },
              { path: "notificaciones", ...page(async () => (await import("@/features/settings/NotificationsPage")).NotificationsPage) },
            ],
          },
        ],
      },
    ],
  },
];

export const router = createBrowserRouter(routes);
