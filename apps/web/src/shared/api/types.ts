import type { components } from "./schema";

// Tipos del contrato. Lo que ya existe en la API sale de docs/openapi.json, generado en schema.d.ts
// (pnpm --filter web api:types; la CI comprueba que está al día): si el backend cambia un campo, el
// frontend deja de compilar. Lo de más abajo, a mano, es la propuesta para endpoints que aún no existen;
// se moverá aquí arriba cuando existan.

type Schemas = components["schemas"];

/** Página de resultados: la misma forma para cualquier listado (PageResponse en el backend). */
export type Page<T> = { content: T[]; page: Schemas["PageMetadata"] };

export type Problem = {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  code?: string;
  traceId?: string;
  errors?: { field: string; message: string }[];
};

/** El usuario autenticado, tal y como lo devuelve /api/v1/me. Sin datos clínicos: tendrán su propio módulo. */
export type Me = Schemas["MeResponse"];
export type Role = Me["role"];
export type Session = Schemas["SessionResponse"];
export type Registration = Schemas["RegistrationResponse"];

export type Specialty = Schemas["SpecialtyResponse"];
export type SpecialtyKind = Specialty["kind"];
export type Facility = Schemas["FacilityResponse"];
export type Doctor = Schemas["DoctorResponse"];

// ─── Pendiente de endpoint ──────────────────────────────────────────────────

export type Slot = { startsAt: string; durationMin: number };

export type Modality = "IN_PERSON" | "TELEMEDICINE";
export type AppointmentStatus = "SCHEDULED" | "CONFIRMED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";

export type Appointment = {
  publicId: string;
  startsAt: string;
  durationMin: number;
  modality: Modality;
  status: AppointmentStatus;
  reason: string | null;
  doctor: { publicId: string; firstName: string; lastName: string; specialtyName: string };
  facility: { publicId: string; name: string; city: string } | null;
};

export type ReportKind = "LAB" | "DIAGNOSIS" | "IMAGING" | "PRESCRIPTION" | "OTHER";

export type Report = {
  publicId: string;
  title: string;
  kind: ReportKind;
  issuedOn: string;
  doctorName: string;
  fileCount: number;
};

