import { delay, http, HttpResponse } from "msw";
import type { Appointment, Me, Page, Session, Slot } from "@/shared/api/types";
import { DEMO_CODE, DEMO_EMAIL, DEMO_PASSWORD, demoUser, doctors, facilities, reports, specialties } from "./data";

// API simulada con la forma del contrato: /api/v1, Page<T> y errores RFC 9457.
// Se usa en desarrollo (hasta que exista el backend) y en los tests.

const API = "*/api/v1";
const LATENCY = import.meta.env.MODE === "test" ? 0 : 280;

type Account = { user: Me; password: string; verified: boolean };

const accounts = new Map<string, Account>([[DEMO_EMAIL, { user: demoUser, password: DEMO_PASSWORD, verified: true }]]);

// sessionStorage hace aquí de "cookie httpOnly" y de base de datos: solo existe en el mock.
const REFRESH_KEY = "mock-refresh-cookie";
const APPOINTMENTS_KEY = "mock-appointments";

const problem = (status: number, code: string, detail: string) =>
  HttpResponse.json(
    { type: `https://api.vitsync.es/errors/${code.toLowerCase().replaceAll("_", "-")}`, title: detail, status, detail, code, traceId: crypto.randomUUID().slice(0, 16) },
    { status, headers: { "Content-Type": "application/problem+json" } },
  );

function paginate<T>(items: T[], url: URL, defaultSize = 20): Page<T> {
  const number = Number(url.searchParams.get("page") ?? 0);
  const size = Number(url.searchParams.get("size") ?? defaultSize);
  return {
    content: items.slice(number * size, (number + 1) * size),
    page: { number, size, totalElements: items.length, totalPages: Math.ceil(items.length / size) },
  };
}

const sessionFor = (user: Me): Session => ({ accessToken: `mock.${user.email}`, user });

function authedUser(request: Request) {
  const email = request.headers.get("Authorization")?.replace("Bearer mock.", "");
  return email ? accounts.get(email)?.user : undefined;
}

const unauthenticated = () => problem(401, "UNAUTHENTICATED", "A valid access token is required");

function at(daysFromNow: number, hour: number, minute = 0) {
  const date = new Date();
  date.setDate(date.getDate() + daysFromNow);
  // Los centros no abren en fin de semana.
  while (date.getDay() === 0 || date.getDay() === 6) date.setDate(date.getDate() + 1);
  date.setHours(hour, minute, 0, 0);
  return date.toISOString();
}

function initialAppointments(): Appointment[] {
  const brief = (i: number) => {
    const d = doctors[i]!;
    return { publicId: d.publicId, firstName: d.firstName, lastName: d.lastName, specialtyName: d.specialty.name };
  };
  const facility = { publicId: "fac-turia", name: "Centro Jardín del Turia", city: "València" };
  return [
    { publicId: "apt-1", startsAt: at(6, 10, 20), durationMin: 20, modality: "IN_PERSON", status: "SCHEDULED", reason: "Revisión anual", doctor: brief(0), facility },
    { publicId: "apt-2", startsAt: at(19, 12, 0), durationMin: 20, modality: "TELEMEDICINE", status: "CONFIRMED", reason: "Resultados de la analítica", doctor: brief(2), facility: null },
    { publicId: "apt-3", startsAt: at(-40, 9, 40), durationMin: 20, modality: "IN_PERSON", status: "COMPLETED", reason: "Dolor de rodilla", doctor: brief(8), facility: { publicId: "fac-albufera", name: "Clínica Albufera", city: "València" } },
    { publicId: "apt-4", startsAt: at(-95, 11, 0), durationMin: 20, modality: "IN_PERSON", status: "CANCELLED", reason: null, doctor: brief(4), facility: { publicId: "fac-albufera", name: "Clínica Albufera", city: "València" } },
  ];
}

function loadAppointments(): Appointment[] {
  const saved = sessionStorage.getItem(APPOINTMENTS_KEY);
  return saved ? (JSON.parse(saved) as Appointment[]) : initialAppointments();
}

const saveAppointments = (list: Appointment[]) => sessionStorage.setItem(APPOINTMENTS_KEY, JSON.stringify(list));

/** Número estable a partir de un texto: los huecos ocupados no cambian entre recargas. */
function hash(text: string) {
  let h = 0;
  for (const c of text) h = (h * 31 + c.charCodeAt(0)) | 0;
  return Math.abs(h);
}

// Este hueco parece libre pero otra persona lo reserva antes: enseña el 409 de la historia VIT-84.
const RACE_HOUR = 10;
const racedSlots = new Set<string>();

function slotsFor(doctorId: string, dateKey: string): Slot[] {
  const taken = new Set(loadAppointments().filter((a) => a.doctor.publicId === doctorId && a.status !== "CANCELLED").map((a) => a.startsAt));
  const slots: Slot[] = [];
  for (let minutes = 9 * 60; minutes < 14 * 60; minutes += 20) {
    const [y, m, d] = dateKey.split("-").map(Number);
    const startsAt = new Date(y!, m! - 1, d!, Math.floor(minutes / 60), minutes % 60).toISOString();
    const busy = hash(`${doctorId}${startsAt}`) % 3 === 0;
    if (!busy && !taken.has(startsAt) && !racedSlots.has(startsAt)) slots.push({ startsAt, durationMin: 20 });
  }
  return slots;
}

export const handlers = [
  http.all(`${API}/*`, async () => {
    await delay(LATENCY);
  }),

  // ─── Identidad ────────────────────────────────────────────────────────────
  http.post(`${API}/auth/login`, async ({ request }) => {
    const { email, password } = (await request.json()) as { email: string; password: string };
    const account = accounts.get(email.toLowerCase());
    if (!account || account.password !== password) return problem(401, "INVALID_CREDENTIALS", "Invalid credentials");
    if (!account.verified) return problem(403, "ACCOUNT_NOT_VERIFIED", "Account not verified");
    sessionStorage.setItem(REFRESH_KEY, account.user.email);
    return HttpResponse.json(sessionFor(account.user));
  }),

  http.post(`${API}/auth/refresh`, () => {
    const account = accounts.get(sessionStorage.getItem(REFRESH_KEY) ?? "");
    return account ? HttpResponse.json(sessionFor(account.user)) : unauthenticated();
  }),

  http.post(`${API}/auth/logout`, () => {
    sessionStorage.removeItem(REFRESH_KEY);
    return new HttpResponse(null, { status: 204 });
  }),

  http.post(`${API}/auth/register`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string; firstName: string; lastName: string; birthDate: string };
    const email = body.email.toLowerCase();
    if (accounts.has(email)) return problem(409, "ACCOUNT_ALREADY_EXISTS", "Account already exists");
    if (body.password.toLowerCase().includes("password")) return problem(400, "PASSWORD_BREACHED", "Password found in known breaches");
    const user: Me = { publicId: crypto.randomUUID(), email, firstName: body.firstName, lastName: body.lastName, role: "PATIENT", phone: null, birthDate: body.birthDate, bloodType: null, allergies: null, emergencyContact: null };
    accounts.set(email, { user, password: body.password, verified: false });
    return new HttpResponse(null, { status: 201 });
  }),

  http.post(`${API}/auth/verify`, async ({ request }) => {
    const { email, code } = (await request.json()) as { email: string; code: string };
    const account = accounts.get(email.toLowerCase());
    if (!account || code !== DEMO_CODE) return problem(400, "INVALID_CODE", "Invalid or expired code");
    account.verified = true;
    return new HttpResponse(null, { status: 204 });
  }),

  http.post(`${API}/auth/password-reset/request`, () => new HttpResponse(null, { status: 202 })),

  http.post(`${API}/auth/password-reset`, async ({ request }) => {
    const { email, code, password } = (await request.json()) as { email: string; code: string; password: string };
    const account = accounts.get(email.toLowerCase());
    if (!account || code !== DEMO_CODE) return problem(400, "INVALID_CODE", "Invalid or expired code");
    account.password = password;
    sessionStorage.removeItem(REFRESH_KEY);
    return new HttpResponse(null, { status: 204 });
  }),

  http.get(`${API}/me`, ({ request }) => {
    const user = authedUser(request);
    return user ? HttpResponse.json(user) : unauthenticated();
  }),

  http.patch(`${API}/me`, async ({ request }) => {
    const user = authedUser(request);
    if (!user) return unauthenticated();
    Object.assign(user, await request.json());
    return HttpResponse.json(user);
  }),

  // ─── Catálogo ─────────────────────────────────────────────────────────────
  http.get(`${API}/specialties`, ({ request }) => {
    const url = new URL(request.url);
    const q = url.searchParams.get("q")?.toLowerCase();
    const kind = url.searchParams.get("kind");
    const items = specialties.filter((s) => (!q || s.name.toLowerCase().includes(q)) && (!kind || s.kind === kind));
    return HttpResponse.json(paginate(items, url));
  }),

  http.get(`${API}/facilities`, ({ request }) => HttpResponse.json(paginate(facilities, new URL(request.url)))),

  http.get(`${API}/doctors`, ({ request }) => {
    const url = new URL(request.url);
    const specialty = url.searchParams.get("specialty");
    const facility = url.searchParams.get("facility");
    const items = doctors.filter((d) => (!specialty || d.specialty.slug === specialty) && (!facility || d.facilities.some((f) => f.publicId === facility)));
    return HttpResponse.json(paginate(items, url, 8));
  }),

  http.get(`${API}/doctors/:id/slots`, ({ params, request }) => {
    const date = new URL(request.url).searchParams.get("date") ?? "";
    return HttpResponse.json(slotsFor(String(params.id), date));
  }),

  // ─── Citas ────────────────────────────────────────────────────────────────
  http.get(`${API}/appointments/me`, ({ request }) => {
    if (!authedUser(request)) return unauthenticated();
    const url = new URL(request.url);
    const upcoming = url.searchParams.get("when") !== "past";
    const now = Date.now();
    const isUpcoming = (a: Appointment) => new Date(a.startsAt).getTime() >= now && a.status !== "CANCELLED";
    const items = loadAppointments()
      .filter((a) => isUpcoming(a) === upcoming)
      .sort((a, b) => (upcoming ? a.startsAt.localeCompare(b.startsAt) : b.startsAt.localeCompare(a.startsAt)));
    return HttpResponse.json(paginate(items, url));
  }),

  http.post(`${API}/appointments`, async ({ request }) => {
    if (!authedUser(request)) return unauthenticated();
    const body = (await request.json()) as { doctorId: string; startsAt: string; modality: Appointment["modality"]; facilityId?: string; reason?: string };
    const start = new Date(body.startsAt);
    if (start.getHours() === RACE_HOUR && start.getMinutes() === 0 && !racedSlots.has(body.startsAt)) {
      racedSlots.add(body.startsAt);
      return problem(409, "APPOINTMENT_SLOT_TAKEN", "The selected time is no longer available");
    }
    const doctor = doctors.find((d) => d.publicId === body.doctorId);
    if (!doctor) return problem(404, "RESOURCE_NOT_FOUND", "The requested resource does not exist");
    const facility = doctor.facilities.find((f) => f.publicId === body.facilityId) ?? null;
    const appointment: Appointment = {
      publicId: crypto.randomUUID(),
      startsAt: body.startsAt,
      durationMin: 20,
      modality: body.modality,
      status: "SCHEDULED",
      reason: body.reason || null,
      doctor: { publicId: doctor.publicId, firstName: doctor.firstName, lastName: doctor.lastName, specialtyName: doctor.specialty.name },
      facility: body.modality === "IN_PERSON" ? facility : null,
    };
    saveAppointments([...loadAppointments(), appointment]);
    return HttpResponse.json(appointment, { status: 201 });
  }),

  http.post(`${API}/appointments/:id/cancel`, ({ params, request }) => {
    if (!authedUser(request)) return unauthenticated();
    const list = loadAppointments();
    const target = list.find((a) => a.publicId === params.id);
    if (!target) return problem(404, "RESOURCE_NOT_FOUND", "The requested resource does not exist");
    if (target.status === "COMPLETED") return problem(409, "APPOINTMENT_NOT_CANCELLABLE", "Appointment cannot be cancelled");
    target.status = "CANCELLED";
    saveAppointments(list);
    return new HttpResponse(null, { status: 204 });
  }),

  // ─── Informes ─────────────────────────────────────────────────────────────
  http.get(`${API}/reports/me`, ({ request }) => {
    if (!authedUser(request)) return unauthenticated();
    const url = new URL(request.url);
    const kind = url.searchParams.get("kind");
    return HttpResponse.json(paginate(reports.filter((r) => !kind || r.kind === kind), url));
  }),
];

/** Para los tests: cada uno empieza con el mock limpio. */
export function resetMocks() {
  racedSlots.clear();
  sessionStorage.clear();
  accounts.clear();
  accounts.set(DEMO_EMAIL, { user: { ...demoUser }, password: DEMO_PASSWORD, verified: true });
}
