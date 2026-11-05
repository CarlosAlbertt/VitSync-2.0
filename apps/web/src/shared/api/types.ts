// Tipos del contrato, escritos a mano. Identidad (/auth, /me) sigue docs/openapi.json; el resto es
// la propuesta para endpoints que aún no existen. Cuando existan, se generarán del schema (guía, Fase 3.3).

export type Page<T> = {
  content: T[];
  page: { number: number; size: number; totalElements: number; totalPages: number };
};

export type Problem = {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  code?: string;
  traceId?: string;
  errors?: { field: string; message: string }[];
};

export type Role = "PATIENT" | "DOCTOR" | "ADMIN";

/** El usuario autenticado, tal y como lo devuelve /api/v1/me. Sin datos clínicos: tendrán su propio módulo. */
export type Me = {
  id: string;
  email: string;
  role: Role;
  firstName: string;
  lastName: string;
  phone: string | null;
  locale: string;
  emailVerified: boolean;
};

export type Registration = { email: string; status: "PENDING_VERIFICATION" };

export type SpecialtyKind = "MEDICAL" | "SURGICAL" | "DIAGNOSTIC" | "GENERAL" | "UNIT";

export type Specialty = {
  publicId: string;
  slug: string;
  name: string;
  description: string;
  kind: SpecialtyKind;
  doctorCount: number;
};

export type Facility = {
  publicId: string;
  name: string;
  address: string;
  city: string;
  phone: string;
};

export type Doctor = {
  publicId: string;
  firstName: string;
  lastName: string;
  licenseNumber: string;
  bio: string;
  specialty: { publicId: string; slug: string; name: string };
  facilities: { publicId: string; name: string; city: string }[];
};

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

export type Session = { accessToken: string; user: Me };
