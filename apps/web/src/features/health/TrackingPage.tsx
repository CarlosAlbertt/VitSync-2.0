import { ArrowDown, ArrowUp, Plus, Watch } from "lucide-react";
import { useState, type FormEvent } from "react";
import { HEIGHT_LABEL, HEIGHT_M, vitals as seed } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatNumber, toDateKey } from "@/shared/lib/format";
import { toast } from "@/shared/lib/toast";
import { Button } from "@/shared/ui/button";
import { type ChartPoint, LineChart } from "@/shared/ui/chart";
import { SelectField, TextField } from "@/shared/ui/field";
import { PageHeader, Panel } from "@/shared/ui/panel";
import { Segmented } from "@/shared/ui/segmented";

type Range = "1m" | "3m" | "6m";
type Kind = "pressure" | "weight" | "glucose" | "heartRate";

const DAYS: Record<Range, number> = { "1m": 31, "3m": 92, "6m": 184 };
const PERIOD: Record<Range, string> = { "1m": "el último mes", "3m": "3 meses", "6m": "6 meses" };

function Headline({ value, unit, change, range }: { value: string; unit: string; change: number; range: Range }) {
  const Arrow = change <= 0 ? ArrowDown : ArrowUp;
  return (
    <div className="mb-4 flex flex-wrap items-end gap-x-4 gap-y-1">
      <p>
        <span className="text-4xl font-black tracking-tight">{value}</span> <span className="text-ink-soft">{unit}</span>
      </p>
      <p className="flex items-center gap-1 pb-1 text-sm font-semibold text-ink-soft">
        <Arrow className="size-4" aria-hidden />
        {formatNumber(Math.abs(change))} {unit} en {PERIOD[range]}
      </p>
    </div>
  );
}

function AddReading({ onAdd, onClose }: { onAdd: (kind: Kind, values: number[]) => void; onClose: () => void }) {
  const [kind, setKind] = useState<Kind>("pressure");
  const [first, setFirst] = useState("");
  const [second, setSecond] = useState("");

  function submit(event: FormEvent) {
    event.preventDefault();
    const values = (kind === "pressure" ? [first, second] : [first]).map((v) => Number(v.replace(",", ".")));
    if (values.some((v) => !Number.isFinite(v) || v <= 0)) {
      toast.error("Revisa los valores: tienen que ser números mayores que cero.");
      return;
    }
    onAdd(kind, values);
  }

  return (
    <form onSubmit={submit} className="card mb-6 grid gap-4 p-6 md:grid-cols-[1fr_1fr_1fr_auto] md:items-end">
      <SelectField label="Qué has medido" value={kind} onChange={(e) => setKind(e.target.value as Kind)}>
        <option value="pressure">Tensión arterial</option>
        <option value="weight">Peso</option>
        <option value="glucose">Glucosa en ayunas</option>
        <option value="heartRate">Frecuencia cardíaca</option>
      </SelectField>
      <TextField label={kind === "pressure" ? "Sistólica (mmHg)" : kind === "weight" ? "Peso (kg)" : kind === "glucose" ? "Glucosa (mg/dL)" : "Pulsaciones por minuto"} inputMode="decimal" required value={first} onChange={(e) => setFirst(e.target.value)} />
      {kind === "pressure" ? <TextField label="Diastólica (mmHg)" inputMode="decimal" required value={second} onChange={(e) => setSecond(e.target.value)} /> : <span className="hidden md:block" />}
      <div className="flex gap-2">
        <Button type="submit">Guardar</Button>
        <Button variant="ghost" onClick={onClose}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}

export function TrackingPage() {
  const [range, setRange] = useState<Range>("3m");
  const [data, setData] = useState(seed);
  const [adding, setAdding] = useState(false);

  const since = new Date();
  since.setDate(since.getDate() - DAYS[range]);
  const cut = (points: ChartPoint[]) => points.filter((p) => p.date >= toDateKey(since));
  const change = (points: ChartPoint[]) => (points.length ? points[points.length - 1]!.value - points[0]!.value : 0);

  const systolic = cut(data.systolic);
  const diastolic = cut(data.diastolic);
  const weight = cut(data.weight);
  const glucose = cut(data.glucose);
  const heartRate = cut(data.heartRate);
  const lastOf = (points: ChartPoint[]) => points[points.length - 1]?.value ?? 0;

  function add(kind: Kind, values: number[]) {
    const date = toDateKey(new Date());
    const point = (value: number) => ({ date, value });
    setData((current) =>
      kind === "pressure"
        ? { ...current, systolic: [...current.systolic, point(values[0]!)], diastolic: [...current.diastolic, point(values[1]!)] }
        : { ...current, [kind]: [...current[kind], point(values[0]!)] },
    );
    setAdding(false);
    toast.success("Medición guardada. Tu equipo médico la verá en tu seguimiento.");
  }

  return (
    <>
      <PageHeader
        title="Seguimiento"
        description="Tus mediciones en casa, para que tú y tu equipo médico veáis cómo evolucionan."
        actions={
          <>
            <Button variant="soft" onClick={() => demo("Conectar un dispositivo")}>
              <Watch className="size-4.5" aria-hidden />
              Conectar dispositivo
            </Button>
            <Button onClick={() => setAdding(true)}>
              <Plus className="size-4.5" aria-hidden />
              Añadir medición
            </Button>
          </>
        }
      />

      {adding && <AddReading onAdd={add} onClose={() => setAdding(false)} />}

      <div className="mb-5">
        <Segmented
          label="Periodo"
          value={range}
          onChange={setRange}
          options={[
            { value: "1m", label: "1 mes" },
            { value: "3m", label: "3 meses" },
            { value: "6m", label: "6 meses" },
          ]}
        />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <Panel title="Tensión arterial">
          <Headline value={`${lastOf(systolic)}/${lastOf(diastolic)}`} unit="mmHg" change={change(systolic)} range={range} />
          <LineChart
            title="Tensión arterial"
            unit="mmHg"
            series={[
              { name: "Sistólica", points: systolic },
              { name: "Diastólica", points: diastolic },
            ]}
          />
          <p className="mt-3 text-sm text-ink-soft">Objetivo pautado por tu cardióloga: por debajo de 130/80 mmHg.</p>
        </Panel>

        <Panel title="Peso">
          <Headline value={formatNumber(lastOf(weight))} unit="kg" change={change(weight)} range={range} />
          <LineChart title="Peso" unit="kg" series={[{ name: "Peso", points: weight }]} />
          <p className="mt-3 text-sm text-ink-soft">
            Índice de masa corporal: {formatNumber(lastOf(weight) / HEIGHT_M ** 2)} (con {HEIGHT_LABEL} de altura).
          </p>
        </Panel>

        <Panel title="Glucosa en ayunas">
          <Headline value={String(lastOf(glucose))} unit="mg/dL" change={change(glucose)} range={range} />
          <LineChart title="Glucosa en ayunas" unit="mg/dL" series={[{ name: "Glucosa", points: glucose }]} band={[70, 100]} />
        </Panel>

        <Panel title="Frecuencia cardíaca en reposo">
          <Headline value={String(lastOf(heartRate))} unit="lpm" change={change(heartRate)} range={range} />
          <LineChart title="Frecuencia cardíaca en reposo" unit="lpm" series={[{ name: "Frecuencia", points: heartRate }]} band={[60, 100]} />
        </Panel>
      </div>
    </>
  );
}
