import { ArrowRight, CalendarPlus, FileText, Search } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";
import { useDoctors, useFacilities, useSpecialties } from "@/shared/api/catalog";
import { ButtonLink } from "@/shared/ui/button";
import { DoctorCard } from "@/shared/ui/doctor-card";
import { Skeleton } from "@/shared/ui/feedback";
import { SpecialtyArt } from "@/shared/ui/specialty-art";

const SEGMENT = "grid gap-0.5 rounded-2xl px-5 py-2 text-left has-focus-visible:outline-2 has-focus-visible:outline-accent";
const SEGMENT_SELECT = "w-full cursor-pointer bg-transparent font-semibold text-ink outline-none";

function SearchBar() {
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

  return (
    <form onSubmit={submit} className="card mt-9 grid gap-1 p-2 md:grid-cols-[1fr_1fr_auto] md:gap-0">
      <label className={SEGMENT}>
        <span className="text-xs font-semibold text-ink-soft">Especialidad</span>
        <select value={specialty} onChange={(e) => setSpecialty(e.target.value)} className={SEGMENT_SELECT}>
          <option value="">Cualquiera</option>
          {specialties.data?.content.map((s) => (
            <option key={s.publicId} value={s.slug}>
              {s.name}
            </option>
          ))}
        </select>
      </label>
      <label className={`${SEGMENT} border-line md:border-l`}>
        <span className="text-xs font-semibold text-ink-soft">Centro</span>
        <select value={facility} onChange={(e) => setFacility(e.target.value)} className={SEGMENT_SELECT}>
          <option value="">Cualquiera</option>
          {facilities.data?.content.map((f) => (
            <option key={f.publicId} value={f.publicId}>
              {f.name}, {f.city}
            </option>
          ))}
        </select>
      </label>
      <button type="submit" className="inline-flex h-14 cursor-pointer items-center justify-center gap-2 rounded-3xl bg-accent px-8 font-bold text-accent-ink transition-colors hover:bg-accent-strong">
        <Search className="size-5" aria-hidden />
        Buscar
      </button>
    </form>
  );
}

function Hero() {
  return (
    <section>
      <div className="mx-auto grid max-w-7xl items-center gap-12 px-5 pt-14 pb-16 lg:grid-cols-[1.25fr_1fr] lg:pt-20 lg:pb-24">
        <div className="min-w-0">
          <h1 className="text-5xl leading-[1.05] font-black tracking-tighter md:text-7xl">
            Tu salud, gestionada en un <span className="text-accent">solo lugar</span>
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-relaxed text-ink-soft md:text-xl">Encuentra a tu especialista, reserva tu cita y consulta tus informes, cuando y donde quieras.</p>
          <SearchBar />
        </div>
        <img src="/hero.webp" alt="Sala de espera luminosa con sillones y plantas" className="hidden aspect-[4/5] w-full rounded-[2.5rem] object-cover lg:block" />
      </div>
    </section>
  );
}

function Specialties() {
  const { data, isPending } = useSpecialties({ size: 8 });
  return (
    <ul className="mt-8 grid grid-cols-2 gap-4 lg:grid-cols-4">
      {isPending
        ? Array.from({ length: 8 }, (_, i) => <Skeleton key={i} className="h-60" />)
        : data?.content.map((s) => (
            <li key={s.publicId}>
              <Link to={`/profesionales?especialidad=${s.slug}`} className="card lift block h-full overflow-hidden">
                <SpecialtyArt slug={s.slug} className="aspect-[4/3] w-full" />
                <span className="block p-4 text-center font-bold">{s.name}</span>
              </Link>
            </li>
          ))}
    </ul>
  );
}

function Doctors() {
  const { data, isPending } = useDoctors({ size: 3 });
  return (
    <ul className="mt-8 grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      {isPending
        ? Array.from({ length: 3 }, (_, i) => <Skeleton key={i} className="h-96" />)
        : data?.content.map((d) => (
            <li key={d.publicId}>
              <DoctorCard doctor={d} />
            </li>
          ))}
    </ul>
  );
}

function SectionHead({ id, title, to, label }: { id: string; title: string; to: string; label: string }) {
  return (
    <div className="flex items-end justify-between gap-4">
      <h2 id={id} className="text-3xl font-black tracking-tight md:text-4xl">
        {title}
      </h2>
      <Link to={to} className="inline-flex shrink-0 items-center gap-1 font-bold text-accent hover:underline">
        {label}
        <ArrowRight className="size-4" aria-hidden />
      </Link>
    </div>
  );
}

const ACTIONS = [
  { to: "/reservar", title: "Pedir cita", text: "Elige especialista, día y hora en unos minutos.", icon: CalendarPlus },
  { to: "/panel/informes", title: "Mis informes", text: "Consulta tus analíticas, pruebas y recetas.", icon: FileText },
];

export function HomePage() {
  return (
    <>
      <Hero />

      <section aria-labelledby="especialidades" className="mx-auto max-w-7xl px-5">
        <SectionHead id="especialidades" title="Especialidades" to="/especialidades" label="Ver todas" />
        <Specialties />
      </section>

      <section aria-labelledby="especialistas" className="mx-auto mt-20 max-w-7xl px-5">
        <SectionHead id="especialistas" title="Nuestros especialistas" to="/profesionales" label="Ver cuadro médico" />
        <Doctors />
      </section>

      <section aria-labelledby="hoy" className="mx-auto mt-20 max-w-7xl px-5">
        <div className="rounded-[2.5rem] bg-brand p-8 text-white md:p-12">
          <h2 id="hoy" className="text-3xl font-black tracking-tight md:text-4xl">
            ¿Qué necesitas hoy?
          </h2>
          <div className="mt-7 grid gap-4 md:grid-cols-2">
            {ACTIONS.map(({ to, title, text, icon: Icon }) => (
              <Link key={to} to={to} className="flex items-center gap-4 rounded-3xl bg-raised p-5 text-ink">
                <span className="grid size-13 shrink-0 place-items-center rounded-2xl bg-accent-wash text-accent-strong">
                  <Icon className="size-6" aria-hidden />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block text-lg font-extrabold">{title}</span>
                  <span className="block text-sm text-ink-soft">{text}</span>
                </span>
                <ArrowRight className="size-5 shrink-0 text-accent-strong" aria-hidden />
              </Link>
            ))}
          </div>
          <ButtonLink to="/contacto" variant="ghost" className="mt-5 -ml-2 text-white hover:bg-white/15 hover:text-white">
            ¿Tienes dudas? Te ayudamos
          </ButtonLink>
        </div>
      </section>
    </>
  );
}
