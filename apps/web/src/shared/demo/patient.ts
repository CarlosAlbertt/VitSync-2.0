import { toDateKey } from "@/shared/lib/format";

// Datos de demostración del área del paciente: ninguna persona, valor ni documento es real.
// Estas pantallas aún no tienen API. Cuando la tengan, cada bloque se sustituye por su consulta.

const daysAgo = (days: number) => {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return toDateKey(date);
};

const at = (days: number, hour: number, minute = 0) => {
  const date = new Date();
  date.setDate(date.getDate() + days);
  date.setHours(hour, minute, 0, 0);
  return date.toISOString();
};

export type Person = { firstName: string; lastName: string };

// ─── Equipo médico ──────────────────────────────────────────────────────────

export type TeamMember = Person & { doctorId: string; specialty: string; role: string };

export const careTeam: TeamMember[] = [
  { doctorId: "doc-1", firstName: "Inés", lastName: "Soler Martí", specialty: "Medicina familiar", role: "Tu médica de cabecera" },
  { doctorId: "doc-3", firstName: "Marta", lastName: "Bellver Ros", specialty: "Cardiología", role: "Control de la tensión" },
  { doctorId: "doc-14", firstName: "Carmen", lastName: "Ibars Lluch", specialty: "Análisis clínicos", role: "Tus analíticas" },
];

// ─── Mensajes ───────────────────────────────────────────────────────────────

export type Message = { id: string; from: "me" | "them"; text: string; at: string };
export type Conversation = { id: string; with: Person; doctorId?: string; specialty: string; subject: string; unread: number; messages: Message[] };

export const conversations: Conversation[] = [
  {
    id: "c-1",
    with: careTeam[0]!,
    doctorId: "doc-1",
    specialty: "Medicina familiar",
    subject: "Resultados de la analítica",
    unread: 1,
    messages: [
      { id: "m-1", from: "them", at: at(-3, 10, 12), text: "Hola, Lucía. He revisado tu analítica: en general está bien. La glucosa y el colesterol salen algo altos y la vitamina D, baja. Lo vemos con calma en la próxima cita." },
      { id: "m-2", from: "me", at: at(-3, 18, 40), text: "Gracias, doctora. ¿Tengo que cambiar algo hasta entonces?" },
      { id: "m-3", from: "them", at: at(-2, 9, 5), text: "Intenta caminar 30 minutos al día y reducir los azúcares. Sigue con la vitamina D como te la pauté. Si te notas mareada, escríbeme." },
    ],
  },
  {
    id: "c-2",
    with: careTeam[1]!,
    doctorId: "doc-3",
    specialty: "Cardiología",
    subject: "Control de tensión",
    unread: 1,
    messages: [
      { id: "m-4", from: "me", at: at(-4, 20, 15), text: "Le adjunto las mediciones de esta semana, como me pidió." },
      { id: "m-5", from: "them", at: at(-1, 16, 20), text: "Buenas tardes. Veo tus últimas mediciones: la tensión va bajando, buen trabajo. Seguimos con el Enalapril igual y lo revisamos en la cita." },
    ],
  },
  {
    id: "c-3",
    with: { firstName: "Atención", lastName: "al paciente" },
    specialty: "VitSync",
    subject: "Factura de tu consulta",
    unread: 0,
    messages: [
      { id: "m-6", from: "them", at: at(-6, 11, 0), text: "Te hemos emitido la factura de tu consulta de dermatología. Puedes pagarla desde Pagos y seguro." },
      { id: "m-7", from: "me", at: at(-6, 12, 30), text: "Perfecto, muchas gracias." },
    ],
  },
];

// ─── Notificaciones y tareas ────────────────────────────────────────────────

export type NoticeKind = "appointment" | "result" | "message" | "medication" | "billing" | "security";
export type Notice = { id: string; kind: NoticeKind; title: string; text: string; at: string; read: boolean; to: string };

export const notices: Notice[] = [
  { id: "n-1", kind: "message", title: "Nuevo mensaje de Marta Bellver", text: "La tensión va bajando, buen trabajo…", at: at(-1, 16, 20), read: false, to: "/panel/mensajes?c=c-2" },
  { id: "n-2", kind: "medication", title: "Tu receta de Enalapril caduca pronto", text: "Te quedan 9 días de tratamiento. Pide la renovación.", at: at(0, 8, 0), read: false, to: "/panel/medicacion" },
  { id: "n-3", kind: "appointment", title: "Recordatorio de cita", text: "Tienes cita con Inés Soler la próxima semana.", at: at(-1, 9, 0), read: false, to: "/panel/citas" },
  { id: "n-4", kind: "result", title: "Nuevo resultado disponible", text: "Analítica general de sangre.", at: "2026-09-22T12:00:00.000Z", read: true, to: "/panel/informes?informe=rep-1" },
  { id: "n-5", kind: "billing", title: "Factura pendiente", text: "Consulta de dermatología · 35,00 €", at: at(-6, 11, 0), read: true, to: "/panel/pagos" },
  { id: "n-6", kind: "security", title: "Nuevo inicio de sesión", text: "App VitSync en iPhone · València", at: at(-2, 21, 4), read: true, to: "/panel/seguridad" },
];

export type TaskKind = "form" | "sign" | "refill" | "payment";
export const tasks: { id: string; kind: TaskKind; title: string; detail: string; to: string }[] = [
  { id: "t-1", kind: "form", title: "Completa el cuestionario previo a tu cita", detail: "Con Inés Soler · unos 5 minutos", to: "/panel/citas" },
  { id: "t-2", kind: "sign", title: "Firma el consentimiento de videoconsulta", detail: "Lo necesitas antes de tu cita por vídeo", to: "/panel/documentos" },
  { id: "t-3", kind: "refill", title: "Renueva la receta de Enalapril", detail: "Te quedan 9 días de tratamiento", to: "/panel/medicacion" },
  { id: "t-4", kind: "payment", title: "Paga tu factura pendiente", detail: "35,00 € · Consulta de dermatología", to: "/panel/pagos" },
];

// ─── Medicación ─────────────────────────────────────────────────────────────

export type Medication = {
  id: string;
  name: string;
  dose: string;
  form: string;
  instructions: string;
  purpose: string;
  prescriber: string;
  since: string;
  daysLeft: number | null;
  times: string[];
  asNeeded?: boolean;
  refillable: boolean;
};

export const medications: Medication[] = [
  { id: "med-1", name: "Enalapril", dose: "10 mg", form: "Comprimido", instructions: "1 comprimido por la mañana", purpose: "Tensión arterial", prescriber: "Marta Bellver Ros", since: "2024-02-10", daysLeft: 9, times: ["08:00"], refillable: true },
  { id: "med-2", name: "Atorvastatina", dose: "20 mg", form: "Comprimido", instructions: "1 comprimido por la noche", purpose: "Colesterol", prescriber: "Inés Soler Martí", since: "2025-06-01", daysLeft: 41, times: ["22:00"], refillable: true },
  { id: "med-3", name: "Colecalciferol (vitamina D)", dose: "25.000 UI", form: "Ampolla bebible", instructions: "1 ampolla al mes, con comida", purpose: "Déficit de vitamina D", prescriber: "Inés Soler Martí", since: daysAgo(40), daysLeft: 50, times: [], refillable: false },
  { id: "med-4", name: "Ibuprofeno", dose: "600 mg", form: "Comprimido", instructions: "1 comprimido si hay dolor, máximo 3 al día", purpose: "Migraña", prescriber: "Diego Marín Tena", since: "2026-03-09", daysLeft: null, times: [], asNeeded: true, refillable: false },
];

export const pastMedications = [
  { name: "Omeprazol 20 mg", period: "Mar 2026 – May 2026", prescriber: "Inés Soler Martí" },
  { name: "Paracetamol 1 g", period: "May 2026", prescriber: "Álvaro Pastor Llorens" },
];

export const dosesToday = [
  { id: "dose-1", medicationId: "med-1", time: "08:00", taken: true },
  { id: "dose-2", medicationId: "med-2", time: "22:00", taken: false },
];

/** Tomas de los últimos 7 días, de la más antigua a hoy. */
export const adherence: Record<string, boolean[]> = {
  "med-1": [true, true, true, false, true, true, true],
  "med-2": [true, true, true, true, true, false, false],
};

// ─── Resultados ─────────────────────────────────────────────────────────────

export type Analyte = {
  id: string;
  name: string;
  group: string;
  value: number;
  unit: string;
  low: number | null;
  high: number | null;
  history: { date: string; value: number }[];
};

const series = (values: number[]) => ["2025-06-10", "2025-11-27", "2026-04-02", "2026-09-22"].map((date, i) => ({ date, value: values[i]! }));

export const LAB_REPORT_ID = "rep-1";

export const analytes: Analyte[] = [
  { id: "glu", name: "Glucosa", group: "Bioquímica", value: 104, unit: "mg/dL", low: 70, high: 100, history: series([96, 99, 101, 104]) },
  { id: "cre", name: "Creatinina", group: "Bioquímica", value: 0.82, unit: "mg/dL", low: 0.5, high: 1.1, history: series([0.79, 0.8, 0.84, 0.82]) },
  { id: "col", name: "Colesterol total", group: "Lípidos", value: 212, unit: "mg/dL", low: null, high: 200, history: series([228, 219, 215, 212]) },
  { id: "ldl", name: "Colesterol LDL", group: "Lípidos", value: 131, unit: "mg/dL", low: null, high: 130, history: series([149, 140, 135, 131]) },
  { id: "hdl", name: "Colesterol HDL", group: "Lípidos", value: 58, unit: "mg/dL", low: 40, high: null, history: series([54, 55, 57, 58]) },
  { id: "tg", name: "Triglicéridos", group: "Lípidos", value: 112, unit: "mg/dL", low: null, high: 150, history: series([131, 124, 118, 112]) },
  { id: "hb", name: "Hemoglobina", group: "Hemograma", value: 13.8, unit: "g/dL", low: 12, high: 16, history: series([13.5, 13.6, 13.9, 13.8]) },
  { id: "leu", name: "Leucocitos", group: "Hemograma", value: 6.4, unit: "mil/µL", low: 4, high: 10, history: series([7.1, 6.8, 6.2, 6.4]) },
  { id: "pla", name: "Plaquetas", group: "Hemograma", value: 245, unit: "mil/µL", low: 150, high: 400, history: series([238, 251, 240, 245]) },
  { id: "tsh", name: "TSH", group: "Hormonas y vitaminas", value: 2.1, unit: "mUI/L", low: 0.4, high: 4, history: series([2.4, 2.2, 2.0, 2.1]) },
  { id: "vitd", name: "Vitamina D", group: "Hormonas y vitaminas", value: 22, unit: "ng/mL", low: 30, high: 100, history: series([27, 24, 25, 22]) },
];

export type Flag = "low" | "normal" | "high";
export const flagOf = (a: Pick<Analyte, "value" | "low" | "high">): Flag => (a.low !== null && a.value < a.low ? "low" : a.high !== null && a.value > a.high ? "high" : "normal");

export const reportNotes: Record<string, { summary: string; recommendations: string[]; files: { name: string; size: string }[] }> = {
  "rep-2": {
    summary: "Revisión de 14 lunares con dermatoscopia digital. No hay cambios sospechosos respecto a la revisión anterior.",
    recommendations: ["Protección solar alta a diario", "Autoexploración mensual", "Próxima revisión en 12 meses"],
    files: [{ name: "Informe dermatología.pdf", size: "420 KB" }, { name: "Mapa de lunares.jpg", size: "2,1 MB" }],
  },
  "rep-3": {
    summary: "Ecografía de rodilla izquierda: pequeño derrame articular sin rotura de ligamentos ni meniscos.",
    recommendations: ["Reposo relativo dos semanas", "Hielo 15 minutos tres veces al día", "Fisioterapia si persiste el dolor"],
    files: [{ name: "Informe ecografía.pdf", size: "380 KB" }, { name: "Imagen 1.jpg", size: "1,4 MB" }, { name: "Imagen 2.jpg", size: "1,3 MB" }],
  },
  "rep-4": {
    summary: "Tratamiento para las crisis de migraña. Ibuprofeno 600 mg si hay dolor, sin superar tres comprimidos al día.",
    recommendations: ["Anota en un diario cuándo aparecen las crisis", "Evita el ayuno prolongado", "Consulta si tienes más de 4 crisis al mes"],
    files: [{ name: "Receta.pdf", size: "120 KB" }],
  },
  "rep-5": {
    summary: "Electrocardiograma en ritmo sinusal a 72 latidos por minuto, sin alteraciones.",
    recommendations: ["Continuar con el tratamiento actual", "Control de tensión en casa dos veces por semana"],
    files: [{ name: "ECG.pdf", size: "640 KB" }],
  },
};

// ─── Seguimiento ────────────────────────────────────────────────────────────

const wobble = (i: number, amplitude: number) => Math.sin(i * 1.7) * amplitude;
const readings = (count: number, every: number, value: (i: number) => number, decimals = 0) =>
  Array.from({ length: count }, (_, i) => ({ date: daysAgo((count - 1 - i) * every), value: Number(value(i).toFixed(decimals)) }));

export const vitals = {
  systolic: readings(26, 7, (i) => 141 - i * 0.6 + wobble(i, 3)),
  diastolic: readings(26, 7, (i) => 90 - i * 0.4 + wobble(i + 2, 2)),
  weight: readings(13, 14, (i) => 72.4 - i * 0.2 + wobble(i, 0.3), 1),
  glucose: readings(13, 14, (i) => 109 - i * 0.8 + wobble(i, 4)),
  heartRate: readings(26, 7, (i) => 74 - i * 0.1 + wobble(i, 3)),
};

export const HEIGHT_M = 1.65;
export const HEIGHT_LABEL = "1,65 m";

// ─── Historial clínico ──────────────────────────────────────────────────────

export const conditions = [
  { name: "Hipertensión arterial", since: "2023", status: "En tratamiento", managedBy: "Cardiología" },
  { name: "Colesterol alto", since: "2024", status: "En tratamiento", managedBy: "Medicina familiar" },
  { name: "Migraña sin aura", since: "2019", status: "Controlada", managedBy: "Neurología" },
  { name: "Esguince de rodilla izquierda", since: "2026", status: "Resuelto", managedBy: "Traumatología" },
];

export const allergies = [{ name: "Penicilina", reaction: "Urticaria y picor generalizado", severity: "Moderada", since: "2010" }];

export const surgeries = [{ name: "Apendicectomía", year: "2008", place: "Hospital ficticio de València" }];

export const familyHistory = ["Padre: diabetes tipo 2", "Madre: hipertensión arterial", "Abuela materna: cáncer de mama"];

export const habits = ["No fumadora", "Alcohol ocasional", "Actividad física moderada, 3 días por semana"];

export type VaccineStatus = "done" | "pending" | "scheduled";
export const vaccines: { name: string; date: string | null; status: VaccineStatus; next: string | null }[] = [
  { name: "Gripe 2026-2027", date: null, status: "pending", next: "Disponible desde el 15 de octubre" },
  { name: "Gripe 2025-2026", date: "2025-10-14", status: "done", next: null },
  { name: "COVID-19 (refuerzo)", date: "2025-10-14", status: "done", next: null },
  { name: "Tétanos y difteria", date: "2019-05-20", status: "done", next: "Próxima dosis en 2029" },
  { name: "Hepatitis B (3 dosis)", date: "2004-03-02", status: "done", next: null },
];

export const encounters = [
  { date: "2026-09-22", title: "Analítica general de sangre", who: "Carmen Ibars Lluch · Análisis clínicos", reportId: "rep-1" },
  { date: "2026-07-03", title: "Revisión de lunares", who: "Clara Montoro Gil · Dermatología", reportId: "rep-2" },
  { date: "2026-05-18", title: "Ecografía de rodilla", who: "Rubén Sancho Beltrán · Radiología", reportId: "rep-3" },
  { date: "2026-03-09", title: "Consulta por migraña", who: "Diego Marín Tena · Neurología", reportId: "rep-4" },
  { date: "2025-11-27", title: "Electrocardiograma de control", who: "Marta Bellver Ros · Cardiología", reportId: "rep-5" },
];

// ─── Documentos ─────────────────────────────────────────────────────────────

export type DocumentCategory = "Informes" | "Consentimientos" | "Identificación" | "Otros";
export type PatientDocument = { id: string; name: string; category: DocumentCategory; size: string; date: string; source: "Tú" | "VitSync"; pendingSignature?: boolean };

export const documents: PatientDocument[] = [
  { id: "d-1", name: "Consentimiento de videoconsulta.pdf", category: "Consentimientos", size: "180 KB", date: at(-2, 10), source: "VitSync", pendingSignature: true },
  { id: "d-2", name: "Informe de alta (otro centro).pdf", category: "Informes", size: "1,2 MB", date: at(-30, 18), source: "Tú" },
  { id: "d-3", name: "Tarjeta sanitaria.jpg", category: "Identificación", size: "640 KB", date: "2026-02-11T10:00:00.000Z", source: "Tú" },
  { id: "d-4", name: "Consentimiento de tratamiento de datos.pdf", category: "Consentimientos", size: "150 KB", date: "2024-02-10T10:00:00.000Z", source: "VitSync" },
  { id: "d-5", name: "Lista de medicación.pdf", category: "Otros", size: "90 KB", date: at(-12, 9), source: "Tú" },
];

// ─── Familia ────────────────────────────────────────────────────────────────

export type FamilyMember = Person & { id: string; relation: string; age: number; access: string; next: string | null; alerts: string[] };

export const family: FamilyMember[] = [
  { id: "f-1", firstName: "Mateo", lastName: "Ferrer Navarro", relation: "Hijo", age: 8, access: "Gestionas su cuenta", next: at(9, 17, 30), alerts: ["Vacuna de los 8 años pendiente"] },
  { id: "f-2", firstName: "Rosa", lastName: "Navarro Pérez", relation: "Madre", age: 67, access: "Puedes gestionar sus citas", next: at(17, 11, 0), alerts: [] },
];

// ─── Pagos y seguro ─────────────────────────────────────────────────────────

export const insurance = {
  insurer: "Mutua de demostración",
  plan: "Plan Completo",
  policy: "VS-2026-000184",
  validUntil: "2027-12-31",
  coverage: ["Consultas de especialista", "Pruebas diagnósticas", "Videoconsulta ilimitada", "Urgencias 24 horas"],
};

export type InvoiceStatus = "pending" | "paid" | "covered";
export const invoices: { id: string; concept: string; date: string; amount: number; status: InvoiceStatus }[] = [
  { id: "FAC-2026-0187", concept: "Consulta de dermatología", date: at(-6, 11), amount: 35, status: "pending" },
  { id: "FAC-2026-0142", concept: "Analítica general de sangre", date: "2026-09-22T10:00:00.000Z", amount: 0, status: "covered" },
  { id: "FAC-2026-0098", concept: "Ecografía de rodilla", date: "2026-05-18T10:00:00.000Z", amount: 18, status: "paid" },
  { id: "FAC-2026-0051", concept: "Consulta de neurología", date: "2026-03-09T10:00:00.000Z", amount: 0, status: "covered" },
  { id: "FAC-2025-0410", concept: "Electrocardiograma", date: "2025-11-27T10:00:00.000Z", amount: 25, status: "paid" },
];

export const paymentMethod = { brand: "Visa", last4: "0000", expires: "12/28" };

// ─── Seguridad ──────────────────────────────────────────────────────────────

export const sessions = [
  { id: "s-1", device: "Chrome en Windows", place: "València, España", at: new Date().toISOString(), current: true, kind: "laptop" as const },
  { id: "s-2", device: "App VitSync en iPhone", place: "València, España", at: at(-2, 21, 4), current: false, kind: "phone" as const },
  { id: "s-3", device: "Safari en Mac", place: "Madrid, España", at: at(-9, 13, 40), current: false, kind: "laptop" as const },
];

export const accessLog = [
  { who: "Marta Bellver Ros", role: "Cardiología", what: "Revisó tus mediciones de tensión", at: at(-1, 16, 10) },
  { who: "Inés Soler Martí", role: "Medicina familiar", what: "Consultó tu analítica", at: at(-3, 10, 2) },
  { who: "Carmen Ibars Lluch", role: "Análisis clínicos", what: "Publicó un resultado", at: "2026-09-22T11:30:00.000Z" },
];
