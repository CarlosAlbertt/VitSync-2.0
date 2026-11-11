import { ArrowRight, ChevronDown, Mail, MapPin, Phone, Search } from "lucide-react";
import { useMemo, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router";
import { useFacilities } from "@/shared/api/catalog";
import { CONTACT } from "@/shared/config/contact";

const ACTIONS = [
  { to: "/reservar", title: "Pedir cita", text: "Reserva tu próxima consulta con nuestros especialistas de forma rápida." },
  { to: "/panel/informes", title: "Resultados", text: "Consulta tus informes, analíticas y pruebas diagnósticas." },
];

const FAQS = [
  { q: "¿Cómo pido una cita?", a: "Entra en Cuadro médico, elige profesional y pulsa su primer hueco o Ver agenda. Después solo tienes que revisar los datos y confirmar." },
  { q: "¿Dónde veo mis resultados?", a: "En Mi área, dentro de Informes, encontrarás tus analíticas, pruebas y recetas, ordenadas por fecha." },
  { q: "¿Cómo cancelo una cita?", a: "En Mi área, dentro de Citas, tienes tus próximas consultas. Elige la que quieras y pulsa Cancelar." },
  { q: "¿Puedo tener la consulta por vídeo?", a: "Sí. Al confirmar la cita, elige Por vídeo como tipo de consulta en lugar de Presencial." },
  { q: "¿Cómo cambio mis datos?", a: "En Mi área, dentro de Mis datos, puedes actualizar tu teléfono, tus alergias y tu contacto de emergencia." },
  { q: "He olvidado mi contraseña", a: "En la pantalla de entrada, pulsa ¿Has olvidado tu contraseña? y te enviaremos un código por email para crear una nueva." },
];

function Faq({ q, a, open, onToggle }: { q: string; a: string; open: boolean; onToggle: () => void }) {
  return (
    <li className={`card overflow-hidden transition-colors ${open ? "border-accent" : ""}`}>
      <h3>
        <button type="button" aria-expanded={open} onClick={onToggle} className="flex w-full cursor-pointer items-center gap-4 px-6 py-5 text-left md:px-7">
          <span className="flex-1 text-lg font-bold">{q}</span>
          <span className={`grid size-10 shrink-0 place-items-center rounded-xl transition-colors ${open ? "bg-accent text-accent-ink" : "bg-subtle"}`}>
            <ChevronDown className={`size-5 transition-transform ${open ? "rotate-180" : ""}`} aria-hidden />
          </span>
        </button>
      </h3>
      {open && <p className="appear px-6 pb-6 leading-relaxed text-ink-soft md:px-7">{a}</p>}
    </li>
  );
}

function ContactRow({ href, label, value, icon: Icon }: { href: string; label: string; value: string; icon: typeof Phone }) {
  return (
    <a href={href} className="group flex items-center gap-4">
      <span className="grid size-13 shrink-0 place-items-center rounded-2xl bg-subtle text-accent transition-colors group-hover:bg-accent group-hover:text-accent-ink">
        <Icon className="size-5.5" aria-hidden />
      </span>
      <span className="min-w-0">
        <span className="block text-xs font-bold text-accent">{label}</span>
        <span className="block truncate text-lg font-extrabold">{value}</span>
      </span>
    </a>
  );
}

export function ContactPage() {
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const faqSection = useRef<HTMLDivElement>(null);
  const facilities = useFacilities();

  const faqs = useMemo(() => {
    const term = query.trim().toLowerCase();
    return term ? FAQS.filter((f) => `${f.q} ${f.a}`.toLowerCase().includes(term)) : FAQS;
  }, [query]);

  function search(event: FormEvent) {
    event.preventDefault();
    setActive(0);
    faqSection.current?.scrollIntoView({ block: "start" });
  }

  return (
    <>
      <section>
        <div className="mx-auto max-w-4xl px-5 pt-16 pb-32 text-center md:pt-24">
          <h1 className="text-5xl leading-[1.05] font-black tracking-tighter md:text-7xl">
            ¿Cómo podemos <span className="text-accent">ayudarte</span> hoy?
          </h1>
          <p className="mx-auto mt-7 max-w-2xl text-lg leading-relaxed text-ink-soft md:text-xl">Encuentra respuestas al momento o habla directamente con nuestro equipo de atención al paciente.</p>
          <form role="search" onSubmit={search} className="card mx-auto mt-10 flex max-w-2xl items-center gap-3 p-2 pl-6">
            <Search className="size-5 shrink-0 text-ink-soft" aria-hidden />
            <input
              type="search"
              aria-label="Buscar en las preguntas frecuentes"
              placeholder="Busca sobre citas, informes, dudas…"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setActive(0);
              }}
              className="h-13 min-w-0 flex-1 bg-transparent text-lg text-ink outline-none placeholder:text-ink-soft"
            />
            <button type="submit" className="h-13 shrink-0 cursor-pointer rounded-3xl bg-accent px-7 font-bold text-accent-ink transition-colors hover:bg-accent-strong">
              Buscar
            </button>
          </form>
        </div>
      </section>

      <div className="relative z-10 mx-auto -mt-20 max-w-[90rem] px-5 lg:px-8">
        <div className="mx-auto grid max-w-4xl gap-6 md:grid-cols-2">
          {ACTIONS.map((action) => (
            <Link key={action.to} to={action.to} className="card lift group flex flex-col items-center p-9 text-center">
              <span className="text-3xl font-black tracking-tight transition-colors group-hover:text-accent">{action.title}</span>
              <span className="mt-4 mb-7 max-w-72 leading-relaxed text-ink-soft">{action.text}</span>
              <span className="mt-auto inline-flex items-center gap-2 rounded-2xl bg-subtle px-6 py-3 text-sm font-bold text-accent transition-colors group-hover:bg-accent group-hover:text-accent-ink">
                Empezar
                <ArrowRight className="size-4" aria-hidden />
              </span>
            </Link>
          ))}
        </div>

        <div ref={faqSection} className="mt-20 grid scroll-mt-24 items-start gap-12 lg:grid-cols-12 lg:gap-14">
          <section aria-labelledby="faq" className="lg:col-span-8">
            <p className="text-sm font-bold text-accent">Resolución de dudas</p>
            <h2 id="faq" className="mt-1 text-4xl font-black tracking-tight">
              Preguntas frecuentes
            </h2>
            {faqs.length === 0 ? (
              <div className="mt-8 rounded-3xl border border-dashed border-line px-6 py-14 text-center">
                <p className="text-xl font-bold">No hemos encontrado nada para «{query}»</p>
                <p className="mt-1 text-ink-soft">Prueba con otras palabras o contacta con nosotros.</p>
              </div>
            ) : (
              <ul className="mt-8 grid gap-4">
                {faqs.map((faq, i) => (
                  <Faq key={faq.q} {...faq} open={active === i} onToggle={() => setActive(active === i ? -1 : i)} />
                ))}
              </ul>
            )}
          </section>

          <aside className="grid gap-6 lg:sticky lg:top-26 lg:col-span-4">
            <section aria-labelledby="directo" className="card p-8">
              <h2 id="directo" className="text-2xl font-black tracking-tight">
                Contacto directo
              </h2>
              <p className="mt-3 mb-7 text-sm leading-relaxed text-ink-soft">¿Tienes una consulta concreta? Nuestro equipo te atiende encantado.</p>
              <div className="grid gap-5">
                <ContactRow href={`tel:${CONTACT.phone.replaceAll(" ", "")}`} label="Centralita" value={CONTACT.phone} icon={Phone} />
                <ContactRow href={`mailto:${CONTACT.email}`} label="Email de soporte" value={CONTACT.email} icon={Mail} />
              </div>
            </section>

            <section aria-labelledby="sedes" className="card p-8">
              <h2 id="sedes" className="text-xl font-black tracking-tight">
                Nuestros centros
              </h2>
              <ul className="mt-5 grid gap-5">
                {facilities.data?.content.map((f) => (
                  <li key={f.publicId} className="flex items-start gap-3">
                    <MapPin className="mt-0.5 size-5 shrink-0 text-accent" aria-hidden />
                    <span>
                      <span className="block font-bold">{f.name}</span>
                      <span className="block text-sm text-ink-soft">
                        {f.address}, {f.city}
                      </span>
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          </aside>
        </div>
      </div>
    </>
  );
}
