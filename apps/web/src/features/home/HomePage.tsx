import { ArrowRight, BriefcaseMedical, CalendarCheck, CalendarPlus, FlaskConical, MapPin, MessageSquare, Mic, Phone, Search, Stethoscope, Video, type LucideIcon } from "lucide-react";
import { useMemo, useState, type FormEvent, type ReactNode } from "react";
import { Link, useNavigate } from "react-router";
import { useDoctors, useFacilities, useSlots, useSpecialties } from "@/shared/api/catalog";
import { useSession } from "@/shared/api/session";
import type { Doctor } from "@/shared/api/types";
import { CONTACT } from "@/shared/config/contact";
import { formatShortDay, formatTime, fullName, nextWorkdays, toDateKey } from "@/shared/lib/format";
import { Avatar } from "@/shared/ui/avatar";
import { Badge } from "@/shared/ui/badge";
import { ButtonLink } from "@/shared/ui/button";
import { Sparkline } from "@/shared/ui/chart";
import { Skeleton } from "@/shared/ui/feedback";
import { SpecialtyArt } from "@/shared/ui/specialty-art";

// Ancho de la portada: más amplio que el resto para que en pantallas grandes no sobre margen.
const WRAP = "mx-auto w-full max-w-[90rem] px-5 lg:px-8";

function Section({ id, title, intro, action, children, tone = "plain" }: { id: string; title: string; intro?: string; action?: ReactNode; children: ReactNode; tone?: "plain" | "soft" }) {
  return (
    <section aria-labelledby={id} className={tone === "soft" ? "bg-subtle" : ""}>
      <div className={`${WRAP} py-16 md:py-20`}>
        <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
          <div className="max-w-2xl">
            <h2 id={id} className="text-2xl font-bold tracking-tight md:text-3xl">
              {title}
            </h2>
            {intro && <p className="mt-2 text-ink-soft md:text-lg">{intro}</p>}
          </div>
          {action}
        </div>
        {children}
      </div>
    </section>
  );
}

function SeeAll({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link to={to} className="inline-flex items-center gap-1.5 font-semibold hover:underline">
      {children}
      <ArrowRight className="size-4" aria-hidden />
    </Link>
  );
}

/** Primer hueco libre de un profesional el próximo día laborable. */
function useFirstSlot(doctor: Doctor | undefined) {
  const day = useMemo(() => toDateKey(nextWorkdays(1)[0]!), []);
  return useSlots(doctor?.publicId, day).data?.[0];
}

// ─── Cabecera ───────────────────────────────────────────────────────────────

function SearchForm() {
  const navigate = useNavigate();
  const specialties = useSpecialties({ size: 50 });
  const facilities = useFacilities();
  const [specialty, setSpecialty] = useState("");
  const [facility, setFacility] = useState("");

  function submit(event: FormEvent) {
    event.preventDefault();
    const params = new URLSearchParams();
    if (specialty) params.set("especialidad", specialty);
    if (facility) params.set("centro", facility);
    navigate(`/profesionales?${params}`);
  }

  const field = "grid gap-1 rounded-xl px-4 py-2.5 has-focus-visible:outline-2 has-focus-visible:outline-accent";
  const select = "w-full cursor-pointer bg-transparent font-semibold text-ink outline-none";

  return (
    <form onSubmit={submit} className="mt-8 grid gap-1 rounded-2xl bg-raised p-2 text-ink md:grid-cols-[1fr_1fr_auto] md:gap-0">
      <label className={field}>
        <span className="text-xs font-semibold text-ink-soft">Especialidad</span>
        <select value={specialty} onChange={(e) => setSpecialty(e.target.value)} className={select}>
          <option value="">Todas las especialidades</option>
          {specialties.data?.content.map((s) => (
            <option key={s.publicId} value={s.slug}>
              {s.name}
            </option>
          ))}
        </select>
      </label>
      <label className={`${field} md:rounded-none md:border-l md:border-line`}>
        <span className="text-xs font-semibold text-ink-soft">Centro</span>
        <select value={facility} onChange={(e) => setFacility(e.target.value)} className={select}>
          <option value="">Todos los centros</option>
          {facilities.data?.content.map((f) => (
            <option key={f.publicId} value={f.publicId}>
              {f.name}, {f.city}
            </option>
          ))}
        </select>
      </label>
      <button type="submit" className="inline-flex h-13 cursor-pointer items-center justify-center gap-2 rounded-xl bg-accent px-7 font-semibold text-accent-ink transition-colors hover:bg-accent-strong md:h-auto">
        <Search className="size-4.5" aria-hidden />
        Buscar
      </button>
    </form>
  );
}

const POPULAR = [
  ["medicina-familiar", "Medicina familiar"],
  ["dermatologia", "Dermatología"],
  ["pediatria", "Pediatría"],
  ["cardiologia", "Cardiología"],
  ["traumatologia", "Traumatología"],
  ["ginecologia", "Ginecología"],
];

/** Fichas reales de la app flotando: lo que el paciente verá al entrar, en lugar de una foto de archivo. */
function Collage() {
  const { data } = useDoctors({ size: 1 });
  const doctor = data?.content[0];
  const slot = useFirstSlot(doctor);
  const glucose = [109, 106, 107, 103, 104, 100, 101, 98];

  return (
    <div aria-hidden className="relative hidden h-[30rem] lg:block">
      <span className="absolute top-6 right-10 size-80 rounded-full bg-white/8" />
      <span className="absolute bottom-0 left-16 size-56 rounded-full bg-white/6" />

      <div className="absolute top-12 right-16 grid size-72 place-items-center overflow-hidden rounded-[2rem] bg-white">
        <SpecialtyArt slug="cardiologia" className="size-64" />
      </div>

      <div className="absolute top-4 left-0 w-72 rounded-2xl bg-raised p-4 text-ink">
        <p className="flex items-center gap-2 text-xs font-semibold text-ink-soft">
          <CalendarCheck className="size-4" />
          Próximo hueco disponible
        </p>
        <p className="mt-2 text-lg font-bold first-letter:uppercase">{slot ? `${formatShortDay(slot.startsAt)}, ${formatTime(slot.startsAt)} h` : "Mañana, 09:00 h"}</p>
        <p className="text-sm text-ink-soft">{doctor ? `${fullName(doctor)} · ${doctor.specialty.name}` : "Medicina familiar"}</p>
      </div>

      <div className="absolute bottom-16 left-6 w-64 rounded-2xl bg-raised p-4 text-ink">
        <div className="flex items-center gap-3">
          <span className="relative">
            <Avatar person={{ firstName: "Marta", lastName: "Bellver" }} className="size-11 text-sm" />
            <span className="absolute right-0 bottom-0 size-3 rounded-full bg-success ring-2 ring-raised" />
          </span>
          <span>
            <span className="block font-bold">Videoconsulta</span>
            <span className="block text-sm text-ink-soft">Con tu cardióloga</span>
          </span>
        </div>
        <div className="mt-3 flex gap-2">
          <span className="grid size-9 place-items-center rounded-full bg-subtle">
            <Mic className="size-4" />
          </span>
          <span className="grid size-9 place-items-center rounded-full bg-subtle">
            <Video className="size-4" />
          </span>
          <span className="ml-auto inline-flex h-9 items-center rounded-full bg-accent px-4 text-sm font-semibold text-accent-ink">Entrar</span>
        </div>
      </div>

      <div className="absolute right-0 bottom-4 w-60 rounded-2xl bg-raised p-4 text-ink">
        <div className="flex items-center justify-between gap-2">
          <span className="text-xs font-semibold text-ink-soft">Glucosa en ayunas</span>
          <Badge tone="success">Normal</Badge>
        </div>
        <p className="mt-1">
          <span className="text-2xl font-bold">98</span> <span className="text-sm text-ink-soft">mg/dL</span>
        </p>
        <Sparkline values={glucose} className="mt-1 h-10 w-full" />
      </div>
    </div>
  );
}

function Hero() {
  return (
    <section className="bg-brand text-white">
      <div className={`${WRAP} grid items-center gap-12 pt-14 pb-24 md:pt-20 md:pb-28 lg:grid-cols-[1.05fr_1fr]`}>
        <div className="min-w-0">
          <p className="text-sm font-semibold text-white/80">Citas, resultados y videoconsulta</p>
          <h1 className="mt-3 text-4xl leading-[1.1] font-bold tracking-tight md:text-[3.4rem]">Encuentra a tu especialista y pide cita en minutos</h1>
          <p className="mt-5 max-w-xl text-lg leading-relaxed text-white/85">Consulta la agenda real de cada profesional, reserva presencial o por vídeo y sigue tu salud desde tu área personal.</p>
          <SearchForm />
          <div className="mt-5 flex flex-wrap items-center gap-2">
            <span className="mr-1 text-sm text-white/80">Lo más buscado:</span>
            {POPULAR.map(([slug, name]) => (
              <Link key={slug} to={`/profesionales?especialidad=${slug}`} className="rounded-full bg-white/12 px-3.5 py-1.5 text-sm font-medium transition-colors hover:bg-white/22">
                {name}
              </Link>
            ))}
          </div>
        </div>
        <Collage />
      </div>
    </section>
  );
}

/** Cifras del catálogo, montadas sobre el borde de la cabecera. */
function Stats() {
  const doctors = useDoctors({ size: 1 });
  const specialties = useSpecialties({ size: 1 });
  const facilities = useFacilities();
  const items: [ReactNode, string][] = [
    [doctors.data?.page.totalElements ?? "–", "especialistas"],
    [specialties.data?.page.totalElements ?? "–", "especialidades"],
    [facilities.data?.content.length ?? "–", "centros propios"],
    [<Video key="v" className="size-7" aria-hidden />, "consulta presencial o por vídeo"],
  ];
  return (
    <div className={`${WRAP} relative z-10 -mt-12`}>
      <ul className="card grid grid-cols-2 gap-6 rounded-2xl p-6 md:grid-cols-4 md:p-8">
        {items.map(([value, label]) => (
          <li key={label} className="flex items-center gap-3">
            <span className="text-3xl font-bold tracking-tight tabular-nums">{value}</span>
            <span className="text-sm leading-tight text-ink-soft">{label}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

// ─── Accesos ────────────────────────────────────────────────────────────────

const SERVICES: { to: string; title: string; text: string; icon: LucideIcon }[] = [
  { to: "/reservar", title: "Pedir cita", text: "Elige especialista, día y hora en unos minutos, sin llamar.", icon: CalendarPlus },
  { to: "/panel/videoconsulta", title: "Videoconsulta", text: "Consulta a tu médico desde casa, con la misma calidad.", icon: Video },
  { to: "/panel/informes", title: "Resultados", text: "Analíticas, pruebas e informes en cuanto se publican.", icon: FlaskConical },
  { to: "/panel/mensajes", title: "Habla con tu médico", text: "Resuelve dudas por mensaje, sin pedir otra cita.", icon: MessageSquare },
];

function Services() {
  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {SERVICES.map(({ to, title, text, icon: Icon }) => (
        <li key={to}>
          <Link to={to} className="card lift group flex h-full flex-col rounded-2xl p-6">
            <span className="grid size-12 place-items-center rounded-xl bg-accent-wash text-accent-strong">
              <Icon className="size-6" aria-hidden />
            </span>
            <span className="mt-5 text-lg font-bold">{title}</span>
            <span className="mt-1 text-ink-soft">{text}</span>
            <span className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold">
              Acceder
              <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5 motion-reduce:transition-none" aria-hidden />
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

// ─── Especialidades ─────────────────────────────────────────────────────────

function Specialties() {
  const { data, isPending } = useSpecialties({ size: 8 });
  return (
    <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {isPending
        ? Array.from({ length: 8 }, (_, i) => <Skeleton key={i} className="h-24" />)
        : data?.content.map((s) => (
            <li key={s.publicId}>
              <Link to={`/profesionales?especialidad=${s.slug}`} className="card lift flex items-center gap-4 rounded-2xl p-3 pr-4">
                <span className="grid size-18 shrink-0 place-items-center overflow-hidden rounded-xl bg-white">
                  <SpecialtyArt slug={s.slug} className="size-18 scale-125" />
                </span>
                <span className="min-w-0">
                  <span className="block truncate font-bold">{s.name}</span>
                  <span className="block text-sm text-ink-soft">
                    {s.doctorCount} {s.doctorCount === 1 ? "profesional" : "profesionales"}
                  </span>
                </span>
              </Link>
            </li>
          ))}
    </ul>
  );
}

// ─── Área del paciente ──────────────────────────────────────────────────────

const PORTAL = ["Tu próxima cita y lo que tienes pendiente, de un vistazo", "Resultados con su rango de referencia y su evolución", "Recordatorios de medicación y renovación de recetas", "Mensajes con tu equipo médico y videoconsulta"];

function PortalShowcase() {
  const authenticated = useSession((s) => s.status === "authenticated");
  return (
    <section aria-labelledby="area" className="overflow-hidden">
      <div className={`${WRAP} grid items-center gap-12 py-16 md:py-20 lg:grid-cols-[1fr_1.4fr]`}>
        <div>
          <p className="text-sm font-semibold text-ink-soft">Área del paciente</p>
          <h2 id="area" className="mt-2 text-3xl font-bold tracking-tight md:text-4xl">
            Toda tu salud, ordenada en un solo lugar
          </h2>
          <ul className="mt-7 grid gap-3.5">
            {PORTAL.map((text) => (
              <li key={text} className="flex items-start gap-3">
                <span className="mt-0.5 grid size-6 shrink-0 place-items-center rounded-full bg-accent-wash text-accent-strong">
                  <Stethoscope className="size-3.5" aria-hidden />
                </span>
                {text}
              </li>
            ))}
          </ul>
          <div className="mt-8 flex flex-wrap gap-3">
            {authenticated ? (
              <ButtonLink to="/panel">Ir a mi área</ButtonLink>
            ) : (
              <>
                <ButtonLink to="/registro">Crear cuenta gratis</ButtonLink>
                <ButtonLink to="/entrar" variant="soft">
                  Ya tengo cuenta
                </ButtonLink>
              </>
            )}
          </div>
        </div>
        <div className="rounded-2xl bg-brand p-3 md:p-5">
          <img src="/portal-preview.webp" alt="Inicio del área del paciente: próxima cita, accesos rápidos, tareas pendientes y mensajes" loading="lazy" className="w-full rounded-xl" />
        </div>
      </div>
    </section>
  );
}

// ─── Especialistas ──────────────────────────────────────────────────────────

function DoctorRow({ doctor }: { doctor: Doctor }) {
  const slot = useFirstSlot(doctor);
  return (
    <li className="card flex flex-col gap-4 rounded-2xl p-5">
      <div className="flex items-center gap-3">
        <Avatar person={doctor} className="size-12 text-base" />
        <div className="min-w-0">
          <p className="truncate font-bold">{fullName(doctor)}</p>
          <p className="truncate text-sm text-ink-soft">{doctor.specialty.name}</p>
        </div>
      </div>
      <p className="flex items-start gap-1.5 text-sm text-ink-soft">
        <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden />
        {doctor.facilities[0]?.name}, {doctor.facilities[0]?.city}
      </p>
      <div className="mt-auto border-t border-line pt-4">
        {slot ? (
          <Link to={`/reservar?profesional=${doctor.publicId}&hora=${encodeURIComponent(slot.startsAt)}`} className="flex items-center justify-between gap-2 text-sm font-semibold hover:underline">
            <span className="first-letter:uppercase">
              {formatShortDay(slot.startsAt)}, {formatTime(slot.startsAt)} h
            </span>
            <span className="inline-flex items-center gap-1">
              Reservar
              <ArrowRight className="size-4" aria-hidden />
            </span>
          </Link>
        ) : (
          <Link to={`/reservar?profesional=${doctor.publicId}`} className="text-sm font-semibold hover:underline">
            Ver agenda
          </Link>
        )}
      </div>
    </li>
  );
}

function Doctors() {
  const { data, isPending } = useDoctors({ size: 4 });
  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {isPending ? Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-48" />) : data?.content.map((d) => <DoctorRow key={d.publicId} doctor={d} />)}
    </ul>
  );
}

// ─── Cómo funciona, profesionales y centros ─────────────────────────────────

const STEPS = [
  ["Busca", "Elige la especialidad, el centro o el profesional que prefieras."],
  ["Reserva", "Escoge el día y la hora que te vengan bien, presencial o por vídeo."],
  ["Acude", "Recibes la confirmación y un recordatorio antes de la cita."],
];

function Steps() {
  return (
    <ol className="grid gap-4 md:grid-cols-3">
      {STEPS.map(([title, text], i) => (
        <li key={title} className="card rounded-2xl p-6">
          <span className="grid size-10 place-items-center rounded-full bg-accent text-sm font-bold text-accent-ink tabular-nums">{i + 1}</span>
          <p className="mt-4 text-lg font-bold">{title}</p>
          <p className="mt-1 text-ink-soft">{text}</p>
        </li>
      ))}
    </ol>
  );
}

function Professionals() {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <div className="rounded-2xl bg-accent-wash p-8">
        <BriefcaseMedical className="size-8 text-accent-strong" aria-hidden />
        <h3 className="mt-4 text-xl font-bold">Tu agenda, sin papeles</h3>
        <p className="mt-2 text-ink-soft">Gestiona tus citas, publica informes y atiende por vídeo desde el mismo sitio que tus pacientes.</p>
        <ButtonLink to="/entrar" className="mt-6">
          Acceso profesionales
        </ButtonLink>
      </div>
      <div className="rounded-2xl bg-warning-wash p-8">
        <Stethoscope className="size-8 text-warning" aria-hidden />
        <h3 className="mt-4 text-xl font-bold">Únete a nuestro cuadro médico</h3>
        <p className="mt-2 text-ink-soft">Buscamos especialistas para nuestros centros de València, Madrid y Barcelona.</p>
        <ButtonLink to="/contacto" variant="soft" className="mt-6">
          Escríbenos
        </ButtonLink>
      </div>
    </div>
  );
}

function Facilities() {
  const { data, isPending } = useFacilities();
  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {isPending
        ? Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-44" />)
        : data?.content.map((f) => (
            <li key={f.publicId} className="card flex flex-col rounded-2xl p-5">
              <p className="font-bold">{f.name}</p>
              <p className="mt-2 flex items-start gap-1.5 text-sm text-ink-soft">
                <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden />
                {f.address}, {f.city}
              </p>
              {f.phone && (
                <a href={`tel:${f.phone.replaceAll(" ", "")}`} className="mt-1.5 flex items-center gap-1.5 text-sm text-ink-soft hover:text-ink">
                  <Phone className="size-4 shrink-0" aria-hidden />
                  {f.phone}
                </a>
              )}
              <Link to={`/profesionales?centro=${f.publicId}`} className="mt-auto inline-flex items-center gap-1.5 pt-4 text-sm font-semibold hover:underline">
                Ver profesionales
                <ArrowRight className="size-4" aria-hidden />
              </Link>
            </li>
          ))}
    </ul>
  );
}

export function HomePage() {
  return (
    <>
      <Hero />
      <Stats />

      <Section id="servicios" title="¿Qué necesitas hoy?">
        <Services />
      </Section>

      <Section id="especialidades" title="Especialidades" intro="Medicina general y especializada, con profesionales en cada centro." action={<SeeAll to="/especialidades">Ver todas</SeeAll>} tone="soft">
        <Specialties />
      </Section>

      <PortalShowcase />

      <Section id="especialistas" title="Nuestros especialistas" intro="Consulta su agenda y reserva el primer hueco que te venga bien." action={<SeeAll to="/profesionales">Ver cuadro médico</SeeAll>} tone="soft">
        <Doctors />
      </Section>

      <Section id="como" title="Cómo pedir cita">
        <Steps />
      </Section>

      <Section id="centros" title="Nuestros centros" intro={`Atención al paciente: ${CONTACT.phone}, de lunes a viernes.`} tone="soft">
        <Facilities />
      </Section>

      <Section id="profesionales" title="¿Eres profesional sanitario?">
        <Professionals />
      </Section>
    </>
  );
}
