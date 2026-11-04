import { CalendarPlus, ChevronLeft, ChevronRight, FileText, MapPin, Navigation, Plus, RefreshCw, Video } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router";
import type { Appointment, AppointmentStatus } from "@/shared/api/types";
import { demo } from "@/shared/lib/demo";
import { errorMessage } from "@/shared/lib/errors";
import { formatDay, formatMonth, formatTime, fullName, toDateKey } from "@/shared/lib/format";
import { toast } from "@/shared/lib/toast";
import { Badge, type Tone } from "@/shared/ui/badge";
import { Button, ButtonLink } from "@/shared/ui/button";
import { ConfirmDialog } from "@/shared/ui/confirm-dialog";
import { EmptyState, ErrorState, Loading } from "@/shared/ui/feedback";
import { PageHeader, Panel } from "@/shared/ui/panel";
import { Segmented } from "@/shared/ui/segmented";
import { useCancelAppointment, useMyAppointments } from "./api";

const STATUS: Record<AppointmentStatus, [string, Tone]> = {
  SCHEDULED: ["Reservada", "info"],
  CONFIRMED: ["Confirmada", "success"],
  COMPLETED: ["Atendida", "neutral"],
  CANCELLED: ["Cancelada", "danger"],
  NO_SHOW: ["No asistió", "warning"],
};

const weekday = (iso: string) => new Intl.DateTimeFormat("es-ES", { weekday: "short", timeZone: "Europe/Madrid" }).format(new Date(iso)).replace(".", "");
const dayNumber = (iso: string) => new Intl.DateTimeFormat("es-ES", { day: "numeric", timeZone: "Europe/Madrid" }).format(new Date(iso));

function AppointmentCard({ appointment, past, onCancel }: { appointment: Appointment; past: boolean; onCancel: () => void }) {
  const { startsAt, durationMin, doctor, facility, modality, status, reason } = appointment;
  const video = modality === "TELEMEDICINE";
  const [label, tone] = STATUS[status];

  return (
    <li className="card flex flex-col gap-5 p-5 md:flex-row md:items-center md:p-6">
      <div className={`grid size-20 shrink-0 place-content-center rounded-3xl text-center ${past ? "bg-subtle" : "bg-accent-wash text-accent-strong"}`}>
        <span className="text-xs font-bold uppercase">{weekday(startsAt)}</span>
        <span className="text-3xl leading-none font-black">{dayNumber(startsAt)}</span>
        <span className="text-xs font-bold uppercase">{formatMonth(startsAt)}</span>
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-lg font-extrabold">
            {formatTime(startsAt)} h <span className="font-semibold text-ink-soft">· {durationMin} min</span>
          </p>
          <Badge tone={tone}>{label}</Badge>
        </div>
        <p className="mt-1 font-semibold">
          {fullName(doctor)} · {doctor.specialtyName}
        </p>
        <p className="mt-1 flex items-center gap-1.5 text-sm text-ink-soft">
          {video ? <Video className="size-4 shrink-0" aria-hidden /> : <MapPin className="size-4 shrink-0" aria-hidden />}
          {video ? "Videoconsulta" : `${facility?.name}, ${facility?.city}`}
        </p>
        {reason && <p className="mt-1 text-sm text-ink-soft">Motivo: {reason}</p>}
      </div>
      <div className="flex flex-wrap gap-2 md:max-w-80 md:justify-end">
        {past ? (
          <>
            {status === "COMPLETED" && (
              <ButtonLink to="/panel/informes" variant="soft" size="sm">
                <FileText className="size-4" aria-hidden />
                Ver informe
              </ButtonLink>
            )}
            <ButtonLink to={`/reservar?profesional=${doctor.publicId}`} size="sm">
              <RefreshCw className="size-4" aria-hidden />
              Volver a pedir
            </ButtonLink>
          </>
        ) : (
          <>
            {video ? (
              <ButtonLink to="/panel/videoconsulta" size="sm">
                <Video className="size-4" aria-hidden />
                Ir a la sala
              </ButtonLink>
            ) : (
              <Button size="sm" variant="soft" onClick={() => demo("Cómo llegar")}>
                <Navigation className="size-4" aria-hidden />
                Cómo llegar
              </Button>
            )}
            <Button size="sm" variant="soft" onClick={() => demo("Añadir al calendario")}>
              <CalendarPlus className="size-4" aria-hidden />
              Al calendario
            </Button>
            <Button size="sm" variant="ghost" onClick={() => demo("Cambiar la fecha")}>
              Cambiar fecha
            </Button>
            <Button size="sm" variant="ghost" onClick={onCancel}>
              Cancelar
            </Button>
          </>
        )}
      </div>
    </li>
  );
}

function List({ when, onCancel }: { when: "upcoming" | "past"; onCancel: (a: Appointment) => void }) {
  const { data, isPending, isError, refetch } = useMyAppointments(when);
  if (isError) return <ErrorState onRetry={() => refetch()} />;
  if (isPending) return <Loading label="Cargando citas" />;
  if (data.content.length === 0) {
    return when === "upcoming" ? (
      <EmptyState title="No tienes citas próximas">
        <ButtonLink to="/reservar" size="sm" className="mt-2">
          Pedir cita
        </ButtonLink>
      </EmptyState>
    ) : (
      <EmptyState title="Sin citas anteriores" />
    );
  }
  return (
    <ul className="grid grid-cols-1 gap-4">
      {data.content.map((a) => (
        <AppointmentCard key={a.publicId} appointment={a} past={when === "past"} onCancel={() => onCancel(a)} />
      ))}
    </ul>
  );
}

const WEEKDAYS = ["L", "M", "X", "J", "V", "S", "D"];

function MonthCalendar() {
  const upcoming = useMyAppointments("upcoming");
  const past = useMyAppointments("past");
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });

  const byDay = useMemo(() => {
    const map = new Map<string, Appointment[]>();
    for (const a of [...(upcoming.data?.content ?? []), ...(past.data?.content ?? [])]) {
      const key = toDateKey(new Date(a.startsAt));
      map.set(key, [...(map.get(key) ?? []), a]);
    }
    return map;
  }, [upcoming.data, past.data]);

  const offset = (month.getDay() + 6) % 7;
  const days = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const cells = [...Array.from({ length: offset }, () => null), ...Array.from({ length: days }, (_, i) => i + 1)];
  const todayKey = toDateKey(new Date());
  const title = new Intl.DateTimeFormat("es-ES", { month: "long", year: "numeric" }).format(month);
  const shift = (delta: number) => setMonth(new Date(month.getFullYear(), month.getMonth() + delta, 1));

  return (
    <section className="card p-5 md:p-6" aria-label="Calendario de citas">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-extrabold first-letter:uppercase">{title}</h2>
        <div className="flex gap-1">
          <Button variant="ghost" size="sm" aria-label="Mes anterior" onClick={() => shift(-1)}>
            <ChevronLeft className="size-4.5" aria-hidden />
          </Button>
          <Button variant="ghost" size="sm" aria-label="Mes siguiente" onClick={() => shift(1)}>
            <ChevronRight className="size-4.5" aria-hidden />
          </Button>
        </div>
      </div>
      <div className="grid grid-cols-7 gap-1.5 text-center text-xs font-bold text-ink-soft">
        {WEEKDAYS.map((d) => (
          <span key={d}>{d}</span>
        ))}
      </div>
      <div className="mt-1.5 grid grid-cols-7 gap-1.5">
        {cells.map((day, i) => {
          if (day === null) return <span key={`empty-${i}`} />;
          const key = toDateKey(new Date(month.getFullYear(), month.getMonth(), day));
          const items = byDay.get(key) ?? [];
          return (
            <div key={key} className={`min-h-16 rounded-xl border p-1.5 md:min-h-24 ${items.length ? "border-accent" : "border-line"}`}>
              <span className={`grid size-7 place-items-center rounded-full text-sm font-bold ${key === todayKey ? "bg-accent text-accent-ink" : ""}`}>{day}</span>
              <ul className="mt-1 grid gap-1">
                {items.map((a) => (
                  <li key={a.publicId} className="truncate rounded-lg bg-accent-wash px-1.5 py-0.5 text-left text-xs font-semibold text-accent-strong">
                    <span className="md:hidden" aria-hidden>
                      •
                    </span>
                    <span className="sr-only md:not-sr-only">
                      {formatTime(a.startsAt)} {a.doctor.lastName.split(" ")[0]}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          );
        })}
      </div>
    </section>
  );
}

const CHECKLIST = ["Rellena el cuestionario previo", "Ten a mano tu tarjeta sanitaria", "Anota tus síntomas y preguntas", "Revisa tu lista de medicación"];

function Preparation() {
  const [done, setDone] = useState<Set<number>>(new Set([1]));
  return (
    <Panel title="Prepara tu próxima cita">
      <ul className="grid grid-cols-1 gap-1">
        {CHECKLIST.map((item, i) => (
          <li key={item}>
            <label className="flex cursor-pointer items-center gap-3 rounded-xl p-2 hover:bg-subtle">
              <input
                type="checkbox"
                checked={done.has(i)}
                onChange={() =>
                  setDone((current) => {
                    const next = new Set(current);
                    if (next.has(i)) next.delete(i);
                    else next.add(i);
                    return next;
                  })
                }
                className="size-5 accent-[var(--accent)]"
              />
              <span className={done.has(i) ? "text-ink-soft line-through" : "font-semibold"}>{item}</span>
            </label>
          </li>
        ))}
      </ul>
      <Button variant="soft" size="sm" className="mt-3 w-full" onClick={() => demo("Cuestionario previo")}>
        Empezar el cuestionario
      </Button>
      <Link to="/panel/medicacion" className="mt-3 block text-center text-sm font-semibold hover:underline">
        Ver mi medicación
      </Link>
    </Panel>
  );
}

export function AppointmentsPage() {
  const [view, setView] = useState<"list" | "calendar">("list");
  const [when, setWhen] = useState<"upcoming" | "past">("upcoming");
  const [target, setTarget] = useState<Appointment | null>(null);
  const cancel = useCancelAppointment();
  const upcoming = useMyAppointments("upcoming");
  const past = useMyAppointments("past");

  function confirmCancel() {
    if (!target) return;
    cancel.mutate(target.publicId, {
      onSuccess: () => {
        toast.success("Cita cancelada. El hueco queda libre para otro paciente.");
        setTarget(null);
      },
      onError: (error) => {
        toast.error(errorMessage(error));
        setTarget(null);
      },
    });
  }

  return (
    <>
      <PageHeader
        title="Citas"
        description="Tus próximas consultas, las anteriores y todo lo que necesitas para prepararlas."
        actions={
          <>
            <Segmented
              label="Vista"
              value={view}
              onChange={setView}
              options={[
                { value: "list", label: "Lista" },
                { value: "calendar", label: "Calendario" },
              ]}
            />
            <ButtonLink to="/reservar">
              <Plus className="size-4.5" aria-hidden />
              Pedir cita
            </ButtonLink>
          </>
        }
      />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_21rem]">
        <div className="min-w-0">
          {view === "calendar" ? (
            <MonthCalendar />
          ) : (
            <>
              <div className="mb-4">
                <Segmented
                  label="Citas"
                  value={when}
                  onChange={setWhen}
                  options={[
                    { value: "upcoming", label: "Próximas", count: upcoming.data?.page.totalElements },
                    { value: "past", label: "Anteriores", count: past.data?.page.totalElements },
                  ]}
                />
              </div>
              <List when={when} onCancel={setTarget} />
            </>
          )}
        </div>
        <div className="grid content-start gap-6">
          <Preparation />
          <Panel title="¿No puedes ir?">
            <p className="text-sm text-ink-soft">Cancela o cambia la cita con antelación: así el hueco queda libre para otra persona.</p>
          </Panel>
        </div>
      </div>

      <ConfirmDialog open={target !== null} title="¿Cancelar esta cita?" confirmLabel="Cancelar cita" busy={cancel.isPending} onConfirm={confirmCancel} onClose={() => setTarget(null)}>
        {target && (
          <>
            {fullName(target.doctor)}, {formatDay(target.startsAt)} a las {formatTime(target.startsAt)} h.
          </>
        )}
      </ConfirmDialog>
    </>
  );
}
