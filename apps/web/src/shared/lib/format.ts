// Las fechas llegan en UTC y se muestran siempre en hora de Madrid.
const TZ = "Europe/Madrid";

export const formatDay = (iso: string) =>
  new Intl.DateTimeFormat("es-ES", { weekday: "long", day: "numeric", month: "long", timeZone: TZ }).format(new Date(iso));

export const formatShortDay = (iso: string) =>
  new Intl.DateTimeFormat("es-ES", { weekday: "short", day: "numeric", timeZone: TZ }).format(new Date(iso));

export const formatMonth = (iso: string) =>
  new Intl.DateTimeFormat("es-ES", { month: "short", timeZone: TZ }).format(new Date(iso));

export const formatTime = (iso: string) =>
  new Intl.DateTimeFormat("es-ES", { hour: "2-digit", minute: "2-digit", timeZone: TZ }).format(new Date(iso));

export const formatDate = (iso: string) =>
  new Intl.DateTimeFormat("es-ES", { day: "2-digit", month: "short", year: "numeric", timeZone: TZ }).format(new Date(iso));

/** Fecha local YYYY-MM-DD, sin pasar por UTC. */
export function toDateKey(date: Date) {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Los próximos n días laborables, empezando mañana. */
export function nextWorkdays(n: number, from = new Date()) {
  const days: Date[] = [];
  const cursor = new Date(from);
  while (days.length < n) {
    cursor.setDate(cursor.getDate() + 1);
    if (cursor.getDay() !== 0 && cursor.getDay() !== 6) days.push(new Date(cursor));
  }
  return days;
}

export const initials = (first: string, last: string) => `${first[0] ?? ""}${last[0] ?? ""}`.toUpperCase();

export const fullName = (p: { firstName: string; lastName: string }) => `${p.firstName} ${p.lastName}`;

// Acepta tanto una fecha ISO completa como una clave YYYY-MM-DD.
const asDate = (value: string) => new Date(value.length === 10 ? `${value}T12:00:00` : value);

export const formatDayMonth = (value: string) => new Intl.DateTimeFormat("es-ES", { day: "numeric", month: "short", timeZone: TZ }).format(asDate(value));

const relative = new Intl.RelativeTimeFormat("es-ES", { numeric: "auto" });

/** "hace 3 horas", "ayer", "hace 5 días"; a partir de una semana, la fecha. */
export function formatRelative(value: string) {
  const minutes = Math.round((asDate(value).getTime() - Date.now()) / 60_000);
  if (Math.abs(minutes) < 60) return relative.format(minutes, "minute");
  if (Math.abs(minutes) < 24 * 60) return relative.format(Math.round(minutes / 60), "hour");
  const days = Math.round(minutes / (24 * 60));
  return Math.abs(days) < 7 ? relative.format(days, "day") : formatDayMonth(value);
}

export const formatMoney = (amount: number) => new Intl.NumberFormat("es-ES", { style: "currency", currency: "EUR" }).format(amount);

export const formatNumber = (value: number) => new Intl.NumberFormat("es-ES", { maximumFractionDigits: 1 }).format(value);
