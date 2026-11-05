import { Download, FileText, Syringe, TriangleAlert } from "lucide-react";
import { Link } from "react-router";
import { allergies, clinicalProfile, conditions, encounters, familyHistory, habits, HEIGHT_LABEL, HEIGHT_M, medications, surgeries, vaccines, type VaccineStatus, vitals } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatDate, formatNumber } from "@/shared/lib/format";
import { Badge, type Tone } from "@/shared/ui/badge";
import { Button, ButtonLink } from "@/shared/ui/button";
import { PageHeader, Panel } from "@/shared/ui/panel";

const CONDITION_TONE: Record<string, Tone> = { "En tratamiento": "info", Controlada: "success", Resuelto: "neutral" };
const VACCINE: Record<VaccineStatus, [string, Tone]> = { done: ["Puesta", "success"], pending: ["Pendiente", "warning"], scheduled: ["Con cita", "info"] };

export function HistoryPage() {
  const weight = vitals.weight[vitals.weight.length - 1]!.value;
  const bmi = weight / HEIGHT_M ** 2;
  const chronic = medications.filter((m) => m.times.length > 0);

  return (
    <>
      <PageHeader
        title="Historial clínico"
        description="El resumen de tu salud que ven los profesionales que te atienden."
        actions={
          <Button variant="soft" onClick={() => demo("Descargar el historial")}>
            <Download className="size-4.5" aria-hidden />
            Descargar resumen
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="grid min-w-0 content-start gap-6">
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Panel title="Problemas de salud">
              <ul className="divide-y divide-line">
                {conditions.map((c) => (
                  <li key={c.name} className="flex items-start justify-between gap-3 py-3">
                    <span>
                      <span className="block font-bold">{c.name}</span>
                      <span className="block text-sm text-ink-soft">
                        Desde {c.since} · {c.managedBy}
                      </span>
                    </span>
                    <Badge tone={CONDITION_TONE[c.status] ?? "neutral"}>{c.status}</Badge>
                  </li>
                ))}
              </ul>
            </Panel>

            <Panel title="Alergias">
              <ul className="grid grid-cols-1 gap-3">
                {allergies.map((a) => (
                  <li key={a.name} className="rounded-2xl bg-danger-wash p-4 text-danger">
                    <p className="flex items-center gap-2 text-lg font-extrabold">
                      <TriangleAlert className="size-5" aria-hidden />
                      {a.name}
                    </p>
                    <p className="mt-1 text-sm font-semibold">{a.reaction}</p>
                    <p className="mt-1 text-sm">
                      Gravedad {a.severity.toLowerCase()} · desde {a.since}
                    </p>
                  </li>
                ))}
              </ul>
              <p className="mt-3 text-sm text-ink-soft">Si descubres una alergia nueva, díselo a tu médica para que la añada.</p>
            </Panel>
          </div>

          <Panel title="Vacunas">
            <ul className="divide-y divide-line">
              {vaccines.map((v) => {
                const [label, tone] = VACCINE[v.status];
                return (
                  <li key={v.name} className="flex flex-wrap items-center gap-3 py-3">
                    <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-accent-wash text-accent-strong">
                      <Syringe className="size-5" aria-hidden />
                    </span>
                    <span className="min-w-48 flex-1">
                      <span className="block font-bold">{v.name}</span>
                      <span className="block text-sm text-ink-soft">{[v.date && formatDate(v.date), v.next].filter(Boolean).join(" · ")}</span>
                    </span>
                    <Badge tone={tone}>{label}</Badge>
                    {v.status === "pending" && (
                      <ButtonLink to="/reservar" size="sm">
                        Pedir cita
                      </ButtonLink>
                    )}
                  </li>
                );
              })}
            </ul>
          </Panel>

          <Panel title="Cronología">
            <ol className="relative grid gap-6 border-l-2 border-line pl-6">
              {encounters.map((e) => (
                <li key={e.date} className="relative">
                  <span aria-hidden className="absolute top-1.5 -left-[1.95rem] size-3.5 rounded-full bg-accent ring-4 ring-raised" />
                  <p className="text-sm font-semibold text-ink-soft">{formatDate(e.date)}</p>
                  <p className="font-bold">{e.title}</p>
                  <p className="text-sm text-ink-soft">{e.who}</p>
                  <Link to={`/panel/informes?informe=${e.reportId}`} className="mt-1 inline-flex items-center gap-1.5 text-sm font-bold hover:underline">
                    <FileText className="size-4" aria-hidden />
                    Ver informe
                  </Link>
                </li>
              ))}
            </ol>
          </Panel>
        </div>

        <div className="grid content-start gap-6">
          <section aria-labelledby="emergencia" className="rounded-[2rem] bg-brand p-6 text-white">
            <h2 id="emergencia" className="text-lg font-extrabold">
              Resumen de emergencia
            </h2>
            <p className="mt-1 text-sm">Lo que necesita saber quien te atienda en una urgencia.</p>
            <dl className="mt-4 grid gap-3">
              {[
                ["Grupo sanguíneo", clinicalProfile.bloodType],
                ["Alergias", allergies.map((a) => a.name).join(", ")],
                ["Medicación habitual", chronic.map((m) => `${m.name} ${m.dose}`).join(", ")],
                ["Contacto de emergencia", clinicalProfile.emergencyContact],
              ].map(([term, value]) => (
                <div key={term} className="rounded-2xl bg-white/10 p-3">
                  <dt className="text-xs font-semibold">{term}</dt>
                  <dd className="font-bold">{value}</dd>
                </div>
              ))}
            </dl>
            <button type="button" onClick={() => demo("Tarjeta de emergencia")} className="mt-4 h-11 w-full cursor-pointer rounded-xl bg-white font-bold text-brand">
              Ver tarjeta de emergencia
            </button>
          </section>

          <Panel title="Medidas" to="/panel/seguimiento" linkLabel="Seguimiento">
            <dl className="grid grid-cols-3 gap-2 text-center">
              {[
                ["Altura", HEIGHT_LABEL],
                ["Peso", `${formatNumber(weight)} kg`],
                ["IMC", formatNumber(bmi)],
              ].map(([term, value]) => (
                <div key={term} className="rounded-2xl bg-subtle p-3">
                  <dt className="text-xs font-semibold text-ink-soft">{term}</dt>
                  <dd className="text-lg font-black">{value}</dd>
                </div>
              ))}
            </dl>
          </Panel>

          <Panel title="Antecedentes">
            {[
              ["Quirúrgicos", surgeries.map((s) => `${s.name} (${s.year})`)],
              ["Familiares", familyHistory],
              ["Hábitos", habits],
            ].map(([title, items]) => (
              <div key={title as string} className="mb-4 last:mb-0">
                <h3 className="mb-1 text-sm font-bold text-ink-soft">{title as string}</h3>
                <ul className="grid grid-cols-1 gap-1">
                  {(items as string[]).map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </div>
            ))}
          </Panel>
        </div>
      </div>
    </>
  );
}
