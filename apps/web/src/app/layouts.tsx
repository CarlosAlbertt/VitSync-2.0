import { useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  Bell,
  CalendarDays,
  ChevronDown,
  ClipboardList,
  CreditCard,
  FlaskConical,
  FolderOpen,
  LayoutDashboard,
  LogOut,
  type LucideIcon,
  Menu,
  MessageSquare,
  Pill,
  Plus,
  Search,
  ShieldCheck,
  UserRound,
  Users,
  Video,
  X,
} from "lucide-react";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, NavLink, Navigate, Outlet, ScrollRestoration, useLocation, useNavigate } from "react-router";
import { request } from "@/shared/api/client";
import { useSession } from "@/shared/api/session";
import type { Role } from "@/shared/api/types";
import { CONTACT } from "@/shared/config/contact";
import { conversations, family, notices } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatRelative, fullName } from "@/shared/lib/format";
import { Avatar } from "@/shared/ui/avatar";
import { ButtonLink } from "@/shared/ui/button";
import { Loading } from "@/shared/ui/feedback";
import { Logo } from "@/shared/ui/logo";
import { ThemeToggle } from "@/shared/ui/theme-toggle";

const NAV = [
  { to: "/profesionales", label: "Cuadro médico" },
  { to: "/especialidades", label: "Especialidades" },
  { to: "/contacto", label: "Contacto" },
];

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `font-semibold transition-colors hover:text-accent ${isActive ? "text-accent" : "text-ink-soft"}`;

function useLogout() {
  const signOut = useSession((s) => s.signOut);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  return async () => {
    await request("/auth/logout", { method: "POST" }).catch(() => undefined);
    signOut();
    queryClient.clear(); // datos de salud: no se quedan en memoria tras cerrar sesión
    navigate("/");
  };
}

/** Al navegar, el foco va al contenido: si no, un lector de pantalla se queda en el menú. */
function useRouteFocus() {
  const main = useRef<HTMLElement>(null);
  const { pathname } = useLocation();
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    main.current?.focus({ preventScroll: true });
  }, [pathname]);
  return main;
}

/** Menú desplegable: se cierra al pulsar fuera, con Escape o al cambiar de pantalla. */
function usePopover() {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const { pathname } = useLocation();

  useEffect(() => setOpen(false), [pathname]);
  useEffect(() => {
    if (!open) return;
    const onPointer = (event: MouseEvent) => {
      if (!ref.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return { open, setOpen, ref };
}

function SkipLink() {
  return (
    <a href="#contenido" className="sr-only rounded-lg bg-raised focus:not-sr-only focus:absolute focus:z-50 focus:m-3 focus:p-3">
      Saltar al contenido
    </a>
  );
}

function Header() {
  const status = useSession((s) => s.status);
  const location = useLocation();
  const [open, setOpen] = useState(false);

  useEffect(() => setOpen(false), [location.pathname]);

  // Mientras se comprueba la sesión no se pinta nada: así no parpadea "Entrar" a quien ya entró.
  const actions =
    status === "checking" ? null : status === "authenticated" ? (
      <>
        <ButtonLink to="/panel" variant="ghost" size="sm">
          Mi área
        </ButtonLink>
        <ButtonLink to="/reservar" size="sm">
          Pedir cita
        </ButtonLink>
      </>
    ) : (
      <>
        <ButtonLink to="/entrar" variant="ghost" size="sm">
          Entrar
        </ButtonLink>
        <ButtonLink to="/registro" size="sm">
          Crear cuenta
        </ButtonLink>
      </>
    );

  return (
    <header className="sticky top-0 z-30 border-b border-line bg-raised">
      <div className="mx-auto flex h-18 max-w-[90rem] items-center justify-between gap-4 px-5 lg:px-8">
        <div className="flex items-center gap-10">
          <Link to="/" aria-label="VitSync, inicio">
            <Logo />
          </Link>
          <nav aria-label="Principal" className="hidden items-center gap-8 md:flex">
            {NAV.map((item) => (
              <NavLink key={item.to} to={item.to} className={linkClass}>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
        <div className="flex items-center gap-1.5">
          <ThemeToggle />
          <div className="hidden items-center gap-2 md:flex">{actions}</div>
          <button
            type="button"
            className="grid size-10 cursor-pointer place-items-center rounded-xl hover:bg-subtle md:hidden"
            aria-expanded={open}
            aria-label={open ? "Cerrar menú" : "Abrir menú"}
            onClick={() => setOpen(!open)}
          >
            {open ? <X className="size-5" aria-hidden /> : <Menu className="size-5" aria-hidden />}
          </button>
        </div>
      </div>
      {open && (
        <div className="grid gap-3 border-t border-line p-5 md:hidden">
          {NAV.map((item) => (
            <NavLink key={item.to} to={item.to} className={linkClass}>
              {item.label}
            </NavLink>
          ))}
          <div className="flex gap-2 pt-1">{actions}</div>
        </div>
      )}
    </header>
  );
}

const FOOTER_LINKS: { title: string; links: [string, string][] }[] = [
  { title: "Pacientes", links: [["Cuadro médico", "/profesionales"], ["Especialidades", "/especialidades"], ["Pedir cita", "/reservar"], ["Mi área", "/panel"]] },
  { title: "Ayuda", links: [["Contacto", "/contacto"], ["Preguntas frecuentes", "/contacto"], ["Crear cuenta", "/registro"], ["Recuperar contraseña", "/recuperar"]] },
];

function Footer() {
  return (
    <footer className="bg-brand text-white">
      <div className="mx-auto grid max-w-[90rem] gap-10 px-5 py-14 md:grid-cols-2 lg:grid-cols-[1.6fr_1fr_1fr_1.3fr] lg:px-8">
        <div>
          <Logo onDark />
          <p className="mt-5 max-w-xs text-sm leading-relaxed text-white/80">Tu salud, gestionada en un solo lugar: especialistas, citas, resultados y videoconsulta.</p>
        </div>
        {FOOTER_LINKS.map(({ title, links }) => (
          <nav key={title} aria-label={title}>
            <h2 className="font-semibold">{title}</h2>
            <ul className="mt-4 grid gap-2.5 text-sm text-white/80">
              {links.map(([label, to]) => (
                <li key={label}>
                  <Link to={to} className="hover:text-white hover:underline">
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        ))}
        <div>
          <h2 className="font-semibold">Atención al paciente</h2>
          <ul className="mt-4 grid gap-2.5 text-sm text-white/80">
            <li>{CONTACT.phone}</li>
            <li>{CONTACT.email}</li>
            <li>De lunes a viernes, de 8:00 a 20:00</li>
          </ul>
        </div>
      </div>
      <div className="border-t border-white/15">
        <p className="mx-auto max-w-[90rem] px-5 py-5 text-sm text-white/70 lg:px-8">© 2026 VitSync · Demostración: centros, profesionales y pacientes son ficticios. No introduzcas datos de salud reales.</p>
      </div>
    </footer>
  );
}

export function PublicLayout() {
  const main = useRouteFocus();
  return (
    <div className="flex min-h-dvh flex-col">
      <SkipLink />
      <Header />
      <main id="contenido" ref={main} tabIndex={-1} className="flex-1 pb-20 outline-none">
        <Outlet />
      </main>
      <Footer />
      <ScrollRestoration />
    </div>
  );
}

export function AuthLayout() {
  const main = useRouteFocus();
  return (
    <>
      <SkipLink />
      <main id="contenido" ref={main} tabIndex={-1} className="outline-none">
        <Outlet />
      </main>
      <ScrollRestoration />
    </>
  );
}

// Esto es experiencia de usuario, no seguridad: la autorización real la hace la API.
export function RequireAuth() {
  const status = useSession((s) => s.status);
  const location = useLocation();

  if (status === "checking") {
    return (
      <div className="mx-auto max-w-6xl px-5 py-12">
        <Loading label="Comprobando tu sesión" />
      </div>
    );
  }
  if (status === "anonymous") return <Navigate to="/entrar" replace state={{ from: location.pathname + location.search }} />;
  return <Outlet />;
}

// ─── Área del paciente ──────────────────────────────────────────────────────

type NavItem = { to: string; label: string; icon: LucideIcon; end?: boolean; badge?: number };

const unreadMessages = conversations.reduce((total, c) => total + c.unread, 0);
const unreadNotices = notices.filter((n) => !n.read);

const GROUPS: { label: string; items: NavItem[] }[] = [
  {
    label: "Principal",
    items: [
      { to: "/panel", label: "Inicio", icon: LayoutDashboard, end: true },
      { to: "/panel/citas", label: "Citas", icon: CalendarDays },
      { to: "/panel/videoconsulta", label: "Videoconsulta", icon: Video },
      { to: "/panel/mensajes", label: "Mensajes", icon: MessageSquare, badge: unreadMessages },
    ],
  },
  {
    label: "Mi salud",
    items: [
      { to: "/panel/informes", label: "Resultados e informes", icon: FlaskConical },
      { to: "/panel/medicacion", label: "Medicación", icon: Pill },
      { to: "/panel/historial", label: "Historial clínico", icon: ClipboardList },
      { to: "/panel/seguimiento", label: "Seguimiento", icon: Activity },
    ],
  },
  {
    label: "Gestión",
    items: [
      { to: "/panel/documentos", label: "Documentos", icon: FolderOpen },
      { to: "/panel/familia", label: "Familia", icon: Users },
      { to: "/panel/pagos", label: "Pagos y seguro", icon: CreditCard },
    ],
  },
  {
    label: "Cuenta",
    items: [
      { to: "/panel/perfil", label: "Mis datos", icon: UserRound },
      { to: "/panel/seguridad", label: "Seguridad y privacidad", icon: ShieldCheck },
      { to: "/panel/notificaciones", label: "Notificaciones", icon: Bell, badge: unreadNotices.length },
    ],
  },
];

function Sidebar() {
  return (
    <div className="flex h-full flex-col">
      <Link to="/panel" aria-label="VitSync, tu área" className="flex h-16 shrink-0 items-center border-b border-line px-6">
        <Logo />
      </Link>
      <nav aria-label="Tu área" className="flex-1 overflow-y-auto px-3 py-4">
        <div className="grid gap-4">
          {GROUPS.map((group) => (
            <div key={group.label}>
              <p className="mb-1 px-3 text-xs font-bold text-ink-soft">{group.label}</p>
              <ul className="grid gap-0.5">
                {group.items.map(({ to, label, icon: Icon, end, badge }) => (
                  <li key={to}>
                    <NavLink
                      to={to}
                      end={end}
                      className={({ isActive }) =>
                        `flex h-9 items-center gap-3 rounded-xl px-3 text-[0.95rem] font-semibold transition-colors ${isActive ? "bg-accent text-accent-ink" : "text-ink-soft hover:bg-subtle hover:text-ink"}`
                      }
                    >
                      <Icon className="size-4.5 shrink-0" aria-hidden />
                      <span className="flex-1 truncate">{label}</span>
                      {badge ? (
                        <span className="grid min-w-5 place-items-center rounded-full bg-accent-wash px-1.5 text-xs font-bold text-accent-strong">
                          {badge}
                          <span className="sr-only"> sin leer</span>
                        </span>
                      ) : null}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </nav>
      <div className="m-3 rounded-2xl bg-subtle px-4 py-3 text-sm">
        <p className="font-bold">¿Necesitas ayuda? {CONTACT.phone}</p>
        <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 font-semibold">
          <Link to="/contacto" className="hover:underline">
            Contacto
          </Link>
          <Link to="/" className="hover:underline">
            Volver a la web
          </Link>
        </div>
      </div>
    </div>
  );
}

function NotificationsMenu() {
  const { open, setOpen, ref } = usePopover();
  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-label={`Notificaciones, ${unreadNotices.length} sin leer`}
        onClick={() => setOpen(!open)}
        className="relative grid size-10 cursor-pointer place-items-center rounded-xl text-ink-soft transition-colors hover:bg-subtle hover:text-ink"
      >
        <Bell className="size-5" aria-hidden />
        {unreadNotices.length > 0 && <span aria-hidden className="absolute top-1.5 right-1.5 grid size-4.5 place-items-center rounded-full bg-danger text-[10px] font-bold text-canvas">{unreadNotices.length}</span>}
      </button>
      {open && (
        <div className="card absolute right-0 z-40 mt-2 w-[min(23rem,calc(100vw-2rem))] p-2">
          <p className="px-3 pt-2 pb-1 font-extrabold">Notificaciones</p>
          <ul>
            {notices.slice(0, 4).map((notice) => (
              <li key={notice.id}>
                <Link to={notice.to} className="flex gap-3 rounded-xl px-3 py-2.5 hover:bg-subtle">
                  <span aria-hidden className={`mt-1.5 size-2 shrink-0 rounded-full ${notice.read ? "bg-transparent" : "bg-danger"}`} />
                  <span className="min-w-0">
                    <span className="block text-sm font-bold">{notice.title}</span>
                    <span className="block truncate text-sm text-ink-soft">{notice.text}</span>
                    <span className="block text-xs text-ink-soft">{formatRelative(notice.at)}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
          <Link to="/panel/notificaciones" className="block rounded-xl px-3 py-2.5 text-center text-sm font-bold hover:bg-subtle">
            Ver todas
          </Link>
        </div>
      )}
    </div>
  );
}

const ROLE_LABEL: Record<Role, string> = { PATIENT: "Paciente", DOCTOR: "Profesional", ADMIN: "Administración" };

function UserMenu() {
  const user = useSession((s) => s.user);
  const logout = useLogout();
  const { open, setOpen, ref } = usePopover();
  if (!user) return null;

  return (
    <div ref={ref} className="relative">
      <button type="button" aria-expanded={open} onClick={() => setOpen(!open)} className="flex h-11 cursor-pointer items-center gap-2 rounded-xl pr-2 pl-1 transition-colors hover:bg-subtle">
        <Avatar person={user} className="size-9 text-sm" />
        <span className="hidden text-left leading-tight md:block">
          <span className="block text-sm font-bold">{user.firstName}</span>
          <span className="block text-xs text-ink-soft">{ROLE_LABEL[user.role]}</span>
        </span>
        <ChevronDown className="size-4 text-ink-soft" aria-hidden />
        <span className="sr-only">Menú de tu cuenta</span>
      </button>
      {open && (
        <div className="card absolute right-0 z-40 mt-2 w-72 p-2">
          <p className="px-3 pt-2 pb-1 text-xs font-bold text-ink-soft">Ver el área de</p>
          <div className="flex items-center gap-3 rounded-xl bg-subtle px-3 py-2">
            <Avatar person={user} className="size-9 text-sm" />
            <span className="min-w-0 flex-1 truncate text-sm font-bold">{fullName(user)}</span>
            <span className="text-xs font-semibold text-ink-soft">Tú</span>
          </div>
          {family.map((member) => (
            <button key={member.id} type="button" onClick={() => demo(`Ver el área de ${member.firstName}`)} className="flex w-full cursor-pointer items-center gap-3 rounded-xl px-3 py-2 text-left hover:bg-subtle">
              <Avatar person={member} className="size-9 text-sm" />
              <span className="min-w-0 flex-1 truncate text-sm font-semibold">{fullName(member)}</span>
              <span className="text-xs text-ink-soft">{member.relation}</span>
            </button>
          ))}
          <div className="my-2 border-t border-line" />
          <Link to="/panel/perfil" className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold hover:bg-subtle">
            <UserRound className="size-4.5" aria-hidden />
            Mis datos
          </Link>
          <Link to="/panel/seguridad" className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold hover:bg-subtle">
            <ShieldCheck className="size-4.5" aria-hidden />
            Seguridad y privacidad
          </Link>
          <button type="button" onClick={logout} className="flex w-full cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold hover:bg-subtle">
            <LogOut className="size-4.5" aria-hidden />
            Cerrar sesión
          </button>
        </div>
      )}
    </div>
  );
}

function SearchBox() {
  const [query, setQuery] = useState("");
  function submit(event: FormEvent) {
    event.preventDefault();
    if (query.trim()) demo("Buscar en tu área");
  }
  return (
    <form role="search" onSubmit={submit} className="hidden h-11 max-w-md flex-1 items-center gap-2.5 rounded-xl bg-subtle px-3.5 md:flex">
      <Search className="size-4.5 shrink-0 text-ink-soft" aria-hidden />
      <input
        type="search"
        aria-label="Buscar en tu área"
        placeholder="Buscar citas, resultados, medicación…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        className="min-w-0 flex-1 bg-transparent text-sm text-ink outline-none placeholder:text-ink-soft"
      />
    </form>
  );
}

export function AppLayout() {
  const main = useRouteFocus();
  const { pathname } = useLocation();
  const [drawer, setDrawer] = useState(false);

  useEffect(() => setDrawer(false), [pathname]);

  return (
    <div className="min-h-dvh">
      <SkipLink />
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-68 border-r border-line bg-raised lg:block">
        <Sidebar />
      </aside>

      {drawer && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button type="button" aria-label="Cerrar menú" onClick={() => setDrawer(false)} className="absolute inset-0 cursor-default bg-black/40" />
          <aside className="absolute inset-y-0 left-0 w-72 max-w-[85vw] bg-raised">
            <Sidebar />
          </aside>
        </div>
      )}

      <div className="lg:pl-68">
        <header className="sticky top-0 z-20 flex h-16 items-center gap-2 border-b border-line bg-raised px-4 lg:px-8">
          <button type="button" aria-label="Abrir menú" onClick={() => setDrawer(true)} className="grid size-10 cursor-pointer place-items-center rounded-xl hover:bg-subtle lg:hidden">
            <Menu className="size-5" aria-hidden />
          </button>
          <Link to="/panel" aria-label="VitSync, tu área" className="lg:hidden">
            <Logo />
          </Link>
          <SearchBox />
          <div className="ml-auto flex items-center gap-1">
            <span className="hidden sm:block">
              <ButtonLink to="/reservar" size="sm">
                <Plus className="size-4" aria-hidden />
                Pedir cita
              </ButtonLink>
            </span>
            <ThemeToggle />
            <NotificationsMenu />
            <UserMenu />
          </div>
        </header>
        <main id="contenido" ref={main} tabIndex={-1} className="mx-auto max-w-7xl px-4 py-6 outline-none lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
      <ScrollRestoration />
    </div>
  );
}
