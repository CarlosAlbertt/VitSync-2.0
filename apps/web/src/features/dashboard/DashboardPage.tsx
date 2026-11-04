import { useQuery } from "@tanstack/react-query";
import { ArrowDown, ArrowUp, CalendarPlus, Check, ClipboardList, CreditCard, FilePen, FileUp, type LucideIcon, MapPin, MessageSquare, Pill, Plus, Syringe, Video } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router";
import { request } from "@/shared/api/client";
import { useSession } from "@/shared/api/session";
import type { Appointment, Page } from "@/shared/api/types";
import { analytes, careTeam, conversations, dosesToday, family, flagOf, LAB_REPORT_ID, medications, tasks, type TaskKind, vaccines, vitals } from "@/shared/demo/patient";
import { formatDay, formatMonth, formatNumber, formatRelative, formatShortDay, formatTime, fullName } from "@/shared/lib/format";
import { Avatar } from "@/shared/ui/avatar";
import { Badge } from "@/shared/ui/badge";
import { ButtonLink } from "@/shared/ui/button";
import { Sparkline } from "@/shared/ui/chart";
import { ErrorState, Skeleton } from "@/shared/ui/feedback";
import { Panel } from "@/shared/ui/panel";

const dayOfMonth = (iso: string) => new Intl.DateTimeFormat("es-ES", { day: "numeric", timeZone: "Europe/Madrid" }).format(new Date(iso));

function greeting() {
  const hour = new Date().getHours();
  return hour < 14 ? "Buenos días" : hour < 21 ? "Buenas tardes" : "Buenas noches";
}

function NextAppointment() {
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ["appointments", "me", "upcoming"],
    queryFn: () => request<Page<Appointment>>("/appointments/me", { query: { when: "upcoming" } }),
  });

  if (isError) return <ErrorState onRetry={() => refetch()} />;
  if (isPending) return <Skeleton className="h-52 rounded-[2rem]" />;

  const [next, ...rest] = data.content;
  if (!next) {
    return (
      <section className="rounded-[2rem] bg-brand p-7 text-white md:p-9">
        <h2 className="text-2xl font-black tracking-tight">No tienes citas próximas</h2>
        <p className="mt-1">Reserva tu próxima consulta en unos minutos.</p>
        <Link to="/reservar" className="mt-5 inline-flex h-12 items-center gap-2 rounded-xl bg-white px-6 font-bold text-brand">
          <Plus className="size-4.5" aria-hidden />
          Pedir cita
        </Link>
      </section>
    );
  }

  const video = next.modality === "TELEMEDICINE";
  return (
    <section aria-label="Próxima cita" className="rounded-[2rem] bg-brand p-7 text-white md:p-9">
      <div className="flex flex-wrap items-center gap-6 md:gap-8">
        <div className="grid size-24 shrink-0 place-content-center rounded-3xl bg-white text-center text-brand">
          <span className="text-4xl leading-none font-black">{dayOfMonth(next.startsAt)}</span>
          <span className="mt-1 text-sm font-bold uppercase">{formatMonth(next.startsAt)}</span>
        </div>
        <div className="min-w-56 flex-1">
          <p className="text-sm font-semibold">Tu próxima cita</p>
          <h2 className="mt-0.5 text-2xl font-black tracking-tight first-letter:uppercase md:text-3xl">
            {formatDay(next.startsAt)}, {formatTime(next.startsAt)} h
          </h2>
          <p className="mt-1.5 font-semibold">
            {fullName(next.doctor)} · {next.doctor.specialtyName}
          </p>
          <p className="mt-1 flex items-center gap-1.5 text-sm">
            {video ? <Video className="size-4" aria-hidden /> : <MapPin className="size-4" aria-hidden />}
            {video ? "Consulta por vídeo" : `${next.facility?.name}, ${next.facility?.city}`}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {video && (
            <Link to="/panel/videoconsulta" className="inline-flex h-12 items-center rounded-xl bg-white px-6 font-bold text-brand">
              Preparar videoconsulta
            </Link>
          )}
          <Link to="/panel/citas" className="inline-flex h-12 items-center rounded-xl bg-white/15 px-6 font-bold transition-colors hover:bg-white/25">
            Gestionar
          </Link>
        </div>
      </div>
      {rest.length > 0 && (
        <p className="mt-6 border-t border-white/20 pt-4 text-sm">
          Después: {rest.map((a) => `${formatShortDay(a.startsAt)} con ${fullName(a.doctor)}`).join(" · ")}
        </p>
      )}
    </section>
  );
}

const ACTIONS: { to: string; label: string; icon: LucideIcon }[] = [
  { to: "/reservar", label: "Pedir cita", icon: CalendarPlus },
  { to: "/panel/videoconsulta", label: "Videoconsulta", icon: Video },
  { to: "/panel/mensajes", label: "Escribir a mi médico", icon: MessageSquare },
  { to: "/panel/medicacion", label: "Renovar receta", icon: Pill },
  { to: "/panel/documentos", label: "Subir documento", icon: FileUp },
];

function QuickActions() {
  return (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {ACTIONS.map(({ to, label, icon: Icon }) => (
        <li key={to}>
          <Link to={to} className="card lift flex h-full flex-col gap-3 rounded-3xl p-4">
            <span className="grid size-11 place-items-center rounded-2xl bg-accent-wash text-accent-strong">
              <Icon className="size-5" aria-hidden />
            </span>
            <span className="font-bold">{label}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

const TASK_ICONS: Record<TaskKind, LucideIcon> = { form: ClipboardList, sign: FilePen, refill: Pill, payment: CreditCard };

function Tasks() {
  return (
    <Panel title={`Pendiente (${tasks.length})`}>
      <ul className="divide-y divide-line">
        {tasks.map((task) => {
          const Icon = TASK_ICONS[task.kind];
          return (
            <li key={task.id}>
              <Link to={task.to} className="flex items-center gap-3 py-3 hover:underline">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-warning-wash text-warning">
                  <Icon className="size-5" aria-hidden />
                </span>
                <span className="min-w-0">
                  <span className="block font-semibold">{task.title}</span>
                  <span className="block text-sm text-ink-soft">{task.detail}</span>
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}

function TodayDoses() {
  const [taken, setTaken] = useState(() => new Set(dosesToday.filter((d) => d.taken).map((d) => d.id)));
  return (
    <Panel title="Medicación de hoy" to="/panel/medicacion" linkLabel="Ver tratamientos">
      <p className="mb-3 text-sm text-ink-soft">
        {taken.size} de {dosesToday.length} tomas hechas
      </p>
      <ul className="grid grid-cols-1 gap-2">
        {dosesToday.map((dose) => {
          const medication = medications.find((m) => m.id === dose.medicationId)!;
          const done = taken.has(dose.id);
          return (
            <li key={dose.id} className="flex items-center gap-3 rounded-2xl bg-subtle p-3">
              <span className="w-12 shrink-0 text-center font-black tabular-nums">{dose.time}</span>
              <span className="min-w-0 flex-1">
                <span className="block truncate font-semibold">
                  {medication.name} {medication.dose}
                </span>
                <span className="block truncate text-sm text-ink-soft">{medication.instructions}</span>
              </span>
              <button
                type="button"
                aria-pressed={done}
                onClick={() =>
                  setTaken((current) => {
                    const next = new Set(current);
                    if (done) next.delete(dose.id);
                    else next.add(dose.id);
                    return next;
                  })
                }
                className={`inline-flex h-10 shrink-0 cursor-pointer items-center gap-1.5 rounded-xl px-3 text-sm font-bold transition-colors ${done ? "bg-success-wash text-success" : "bg-raised ring-1 ring-line hover:ring-accent"}`}
              >
                {done && <Check className="size-4" aria-hidden />}
                {done ? "Tomada" : "Marcar"}
              </button>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}

const last = <T,>(items: T[]) => items[items.length - 1]!;

function Vitals() {
  const quarter = (points: { value: number }[]) => last(points).value - points[Math.max(points.length - 13, 0)]!.value;
  const tiles = [
    { label: "Tensión arterial", value: `${last(vitals.systolic).value}/${last(vitals.diastolic).value}`, unit: "mmHg", change: quarter(vitals.systolic), values: vitals.systolic.map((p) => p.value) },
    { label: "Peso", value: formatNumber(last(vitals.weight).value), unit: "kg", change: last(vitals.weight).value - vitals.weight[Math.max(vitals.weight.length - 7, 0)]!.value, values: vitals.weight.map((p) => p.value) },
    { label: "Glucosa en ayunas", value: String(last(vitals.glucose).value), unit: "mg/dL", change: last(vitals.glucose).value - vitals.glucose[Math.max(vitals.glucose.length - 7, 0)]!.value, values: vitals.glucose.map((p) => p.value) },
  ];
  return (
    <Panel title="Tu seguimiento" to="/panel/seguimiento" linkLabel="Ver gráficas">
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {tiles.map((tile) => {
          const Arrow = tile.change <= 0 ? ArrowDown : ArrowUp;
          return (
            <li key={tile.label} className="rounded-2xl bg-subtle p-4">
              <p className="text-sm font-semibold text-ink-soft">{tile.label}</p>
              <p className="mt-1">
                <span className="text-2xl font-black tracking-tight">{tile.value}</span> <span className="text-sm text-ink-soft">{tile.unit}</span>
              </p>
              <Sparkline values={tile.values} className="mt-2 h-10 w-full" />
              <p className="mt-1 flex items-center gap-1 text-xs text-ink-soft">
                <Arrow className="size-3.5" aria-hidden />
                {formatNumber(Math.abs(tile.change))} {tile.unit} en 3 meses
              </p>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}

function RecentResults() {
  const flagged = analytes.filter((a) => flagOf(a) !== "normal");
  return (
    <Panel title="Último resultado" to={`/panel/informes?informe=${LAB_REPORT_ID}`} linkLabel="Ver analítica">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <p className="font-bold">Analítica general de sangre</p>
        <Badge tone="warning">{flagged.length} valores fuera de rango</Badge>
      </div>
      <ul className="divide-y divide-line">
        {flagged.map((a) => {
          const high = flagOf(a) === "high";
          return (
            <li key={a.id} className="flex items-center justify-between gap-3 py-2.5">
              <span className="font-semibold">{a.name}</span>
              <span className="flex items-center gap-3">
                <span className="tabular-nums">
                  <span className="font-bold">{formatNumber(a.value)}</span> <span className="text-sm text-ink-soft">{a.unit}</span>
                </span>
                <Badge tone="warning">
                  {high ? <ArrowUp className="size-3" aria-hidden /> : <ArrowDown className="size-3" aria-hidden />}
                  {high ? "Alto" : "Bajo"}
                </Badge>
              </span>
            </li>
          );
        })}
      </ul>
      <p className="mt-3 text-sm text-ink-soft">Tu médica ya ha revisado estos resultados y te ha escrito.</p>
    </Panel>
  );
}

function Messages() {
  return (
    <Panel title="Mensajes" to="/panel/mensajes">
      <ul className="grid grid-cols-1 gap-1">
        {conversations.map((c) => {
          const lastMessage = c.messages[c.messages.length - 1]!;
          return (
            <li key={c.id}>
              <Link to={`/panel/mensajes?c=${c.id}`} className="-mx-2 flex gap-3 rounded-2xl p-2 hover:bg-subtle">
                <Avatar person={c.with} className="size-10 text-sm" />
                <span className="min-w-0 flex-1">
                  <span className="flex items-center justify-between gap-2">
                    <span className="truncate font-bold">{fullName(c.with)}</span>
                    <span className="shrink-0 text-xs text-ink-soft">{formatRelative(lastMessage.at)}</span>
                  </span>
                  <span className={`block truncate text-sm ${c.unread ? "font-semibold text-ink" : "text-ink-soft"}`}>{lastMessage.text}</span>
                </span>
                {c.unread > 0 && <span aria-label="Sin leer" className="mt-2 size-2.5 shrink-0 rounded-full bg-danger" />}
              </Link>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}

function CareTeam() {
  return (
    <Panel title="Tu equipo médico">
      <ul className="grid grid-cols-1 gap-3">
        {careTeam.map((member) => (
          <li key={member.doctorId} className="flex items-center gap-3">
            <Avatar person={member} className="size-11 text-sm" />
            <span className="min-w-0 flex-1">
              <span className="block truncate font-bold">{fullName(member)}</span>
              <span className="block truncate text-sm text-ink-soft">
                {member.specialty} · {member.role}
              </span>
            </span>
            <Link to="/panel/mensajes" aria-label={`Escribir a ${fullName(member)}`} className="grid size-10 shrink-0 place-items-center rounded-xl text-ink-soft hover:bg-subtle hover:text-ink">
              <MessageSquare className="size-4.5" aria-hidden />
            </Link>
          </li>
        ))}
      </ul>
    </Panel>
  );
}

function Prevention() {
  const pending = vaccines.filter((v) => v.status === "pending");
  return (
    <Panel title="Prevención" to="/panel/historial" linkLabel="Ver vacunas">
      <ul className="grid grid-cols-1 gap-3">
        {pending.map((vaccine) => (
          <li key={vaccine.name} className="flex items-start gap-3 rounded-2xl bg-subtle p-3">
            <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-accent-wash text-accent-strong">
              <Syringe className="size-5" aria-hidden />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block font-bold">{vaccine.name}</span>
              <span className="block text-sm text-ink-soft">{vaccine.next}</span>
            </span>
          </li>
        ))}
      </ul>
      <ButtonLink to="/reservar" variant="soft" size="sm" className="mt-3 w-full">
        Pedir cita para vacunarte
      </ButtonLink>
    </Panel>
  );
}

function Family() {
  return (
    <Panel title="Tu familia" to="/panel/familia" linkLabel="Gestionar">
      <ul className="grid grid-cols-1 gap-3">
        {family.map((member) => (
          <li key={member.id} className="flex items-center gap-3">
            <Avatar person={member} className="size-11 text-sm" />
            <span className="min-w-0 flex-1">
              <span className="block font-bold">
                {member.firstName} <span className="font-normal text-ink-soft">· {member.relation}</span>
              </span>
              <span className="block truncate text-sm text-ink-soft">{member.next ? `Cita el ${formatShortDay(member.next)}` : "Sin citas próximas"}</span>
            </span>
            {member.alerts.length > 0 && <Badge tone="warning">{member.alerts.length} aviso</Badge>}
          </li>
        ))}
      </ul>
    </Panel>
  );
}

export function DashboardPage() {
  const user = useSession((s) => s.user);
  const today = formatDay(new Date().toISOString());

  return (
    <div className="grid grid-cols-1 gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="font-semibold text-ink-soft first-letter:uppercase">{today}</p>
          <h1 className="mt-1 text-4xl font-black tracking-tighter">
            {greeting()}, {user?.firstName}
          </h1>
        </div>
        <ButtonLink to="/panel/mensajes" variant="soft">
          <MessageSquare className="size-4.5" aria-hidden />
          Escribir a mi médico
        </ButtonLink>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="grid min-w-0 grid-cols-1 content-start gap-6">
          <NextAppointment />
          <QuickActions />
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Tasks />
            <TodayDoses />
          </div>
          <Vitals />
          <RecentResults />
        </div>
        <div className="grid min-w-0 grid-cols-1 content-start gap-6 md:grid-cols-2 xl:grid-cols-1">
          <Messages />
          <CareTeam />
          <Prevention />
          <Family />
        </div>
      </div>
    </div>
  );
}
