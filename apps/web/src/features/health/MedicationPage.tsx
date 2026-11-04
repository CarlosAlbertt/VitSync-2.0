import { Check, Clock, MapPin, Pill, Plus, X } from "lucide-react";
import { useState } from "react";
import { adherence, dosesToday, type Medication, medications, pastMedications } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatDate } from "@/shared/lib/format";
import { toast } from "@/shared/lib/toast";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { PageHeader, Panel } from "@/shared/ui/panel";
import { Switch } from "@/shared/ui/switch";

const WEEK = ["L", "M", "X", "J", "V", "S", "D"];

function weekLabels() {
  const labels: string[] = [];
  for (let i = 6; i >= 0; i--) {
    const day = new Date();
    day.setDate(day.getDate() - i);
    labels.push(WEEK[(day.getDay() + 6) % 7]!);
  }
  return labels;
}

function MedicationCard({ medication, requested, onRefill }: { medication: Medication; requested: boolean; onRefill: () => void }) {
  const low = medication.daysLeft !== null && medication.daysLeft < 14;
  return (
    <li className="card flex flex-col gap-4 p-5">
      <div className="flex items-start gap-3">
        <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-accent-wash text-accent-strong">
          <Pill className="size-6" aria-hidden />
        </span>
        <div className="min-w-0 flex-1">
          <h3 className="text-lg font-extrabold">
            {medication.name} <span className="font-semibold text-ink-soft">{medication.dose}</span>
          </h3>
          <p className="text-sm text-ink-soft">{medication.form}</p>
        </div>
        <Badge tone="info">{medication.purpose}</Badge>
      </div>

      <p className="font-semibold">{medication.instructions}</p>

      {medication.times.length > 0 && (
        <ul className="flex flex-wrap gap-2" aria-label="Horario">
          {medication.times.map((time) => (
            <li key={time} className="inline-flex items-center gap-1.5 rounded-xl bg-subtle px-3 py-1.5 text-sm font-bold tabular-nums">
              <Clock className="size-4" aria-hidden />
              {time}
            </li>
          ))}
        </ul>
      )}
      {medication.asNeeded && (
        <span className="self-start">
          <Badge>Solo si lo necesitas</Badge>
        </span>
      )}

      {medication.daysLeft !== null && (
        <div>
          <div className="mb-1.5 flex justify-between text-sm">
            <span className="font-semibold">Quedan {medication.daysLeft} días</span>
            {low && <span className="font-bold text-warning">Renueva pronto</span>}
          </div>
          <div className="h-2 rounded-full bg-subtle" aria-hidden>
            <div className={`h-full rounded-full ${low ? "bg-warning" : "bg-success"}`} style={{ width: `${Math.min(medication.daysLeft / 60, 1) * 100}%` }} />
          </div>
        </div>
      )}

      <p className="text-sm text-ink-soft">
        Pautado por {medication.prescriber} · desde {formatDate(medication.since)}
      </p>

      <div className="mt-auto flex flex-wrap gap-2">
        {medication.refillable &&
          (requested ? (
            <Badge tone="success">
              <Check className="size-3" aria-hidden />
              Renovación solicitada
            </Badge>
          ) : (
            <Button size="sm" onClick={onRefill}>
              Renovar receta
            </Button>
          ))}
        <Button size="sm" variant="ghost" onClick={() => demo("Ver el prospecto")}>
          Ver prospecto
        </Button>
      </div>
    </li>
  );
}

export function MedicationPage() {
  const [taken, setTaken] = useState(() => new Set(dosesToday.filter((d) => d.taken).map((d) => d.id)));
  const [requested, setRequested] = useState<Set<string>>(new Set());
  const [reminders, setReminders] = useState<Record<string, boolean>>({ "med-1": true, "med-2": true, "med-3": false });
  const scheduled = medications.filter((m) => adherence[m.id]);
  const labels = weekLabels();
  const totals = scheduled.reduce((sum, m) => [sum[0]! + adherence[m.id]!.filter(Boolean).length, sum[1]! + 7], [0, 0]);

  function refill(medication: Medication) {
    setRequested((current) => new Set(current).add(medication.id));
    toast.success(`Hemos enviado la solicitud a ${medication.prescriber}. Te avisaremos cuando esté lista.`);
  }

  return (
    <>
      <PageHeader
        title="Medicación"
        description="Tus tratamientos activos, cuándo tomarlos y cuándo renovar la receta."
        actions={
          <Button variant="soft" onClick={() => demo("Añadir un medicamento")}>
            <Plus className="size-4.5" aria-hidden />
            Añadir medicamento
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_21rem]">
        <div className="grid min-w-0 content-start gap-6">
          <Panel title="Tomas de hoy">
            <ul className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              {dosesToday.map((dose) => {
                const medication = medications.find((m) => m.id === dose.medicationId)!;
                const done = taken.has(dose.id);
                return (
                  <li key={dose.id} className={`flex items-center gap-3 rounded-2xl p-4 ${done ? "bg-success-wash" : "bg-subtle"}`}>
                    <span className="text-xl font-black tabular-nums">{dose.time}</span>
                    <span className="min-w-0 flex-1">
                      <span className="block font-bold">
                        {medication.name} {medication.dose}
                      </span>
                      <span className="block text-sm text-ink-soft">{done ? "Tomada" : "Pendiente"}</span>
                    </span>
                    <Button
                      size="sm"
                      variant={done ? "ghost" : "primary"}
                      aria-pressed={done}
                      onClick={() =>
                        setTaken((current) => {
                          const next = new Set(current);
                          if (done) next.delete(dose.id);
                          else next.add(dose.id);
                          return next;
                        })
                      }
                    >
                      {done ? "Deshacer" : "Marcar tomada"}
                    </Button>
                  </li>
                );
              })}
            </ul>
          </Panel>

          <section aria-labelledby="activos">
            <h2 id="activos" className="mb-3 text-xl font-extrabold">
              Tratamientos activos
            </h2>
            <ul className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {medications.map((m) => (
                <MedicationCard key={m.id} medication={m} requested={requested.has(m.id)} onRefill={() => refill(m)} />
              ))}
            </ul>
          </section>

          <Panel title="Tratamientos anteriores">
            <ul className="divide-y divide-line">
              {pastMedications.map((m) => (
                <li key={m.name} className="flex flex-wrap items-center justify-between gap-2 py-3">
                  <span className="font-semibold">{m.name}</span>
                  <span className="text-sm text-ink-soft">
                    {m.period} · {m.prescriber}
                  </span>
                </li>
              ))}
            </ul>
          </Panel>
        </div>

        <div className="grid content-start gap-6">
          <Panel title="Esta semana">
            <p className="mb-4 text-sm text-ink-soft">
              Has hecho {totals[0]} de {totals[1]} tomas programadas.
            </p>
            <ul className="grid grid-cols-1 gap-4">
              {scheduled.map((m) => (
                <li key={m.id}>
                  <p className="mb-2 font-semibold">{m.name}</p>
                  <ol className="grid grid-cols-7 gap-1.5">
                    {adherence[m.id]!.map((ok, i) => (
                      <li key={i} className="grid justify-items-center gap-1">
                        <span className={`grid size-8 place-items-center rounded-full ${ok ? "bg-success-wash text-success" : "bg-danger-wash text-danger"}`}>
                          {ok ? <Check className="size-4" aria-hidden /> : <X className="size-4" aria-hidden />}
                          <span className="sr-only">{ok ? "Tomada" : "Olvidada"}</span>
                        </span>
                        <span className="text-xs text-ink-soft">{labels[i]}</span>
                      </li>
                    ))}
                  </ol>
                </li>
              ))}
            </ul>
          </Panel>

          <Panel title="Recordatorios">
            <div className="divide-y divide-line">
              {medications
                .filter((m) => !m.asNeeded)
                .map((m) => (
                  <Switch
                    key={m.id}
                    label={m.name}
                    description={m.times.length ? `Aviso a las ${m.times.join(" y ")}` : "Aviso el día 1 de cada mes"}
                    checked={reminders[m.id] ?? false}
                    onChange={(checked) => setReminders({ ...reminders, [m.id]: checked })}
                  />
                ))}
            </div>
          </Panel>

          <Panel title="Farmacia habitual">
            <p className="font-bold">Farmacia Plaza del Turia</p>
            <p className="mt-1 flex items-start gap-1.5 text-sm text-ink-soft">
              <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden />
              Calle ficticia 4, València
            </p>
            <Button variant="soft" size="sm" className="mt-4 w-full" onClick={() => demo("Cambiar de farmacia")}>
              Cambiar farmacia
            </Button>
          </Panel>
        </div>
      </div>
    </>
  );
}
