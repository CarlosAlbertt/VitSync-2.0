import { useQuery } from "@tanstack/react-query";
import { ArrowDown, ArrowUp, Download, FileImage, FileText, FlaskConical, MessageSquare, Pill, ScanLine, Share2, Stethoscope, type LucideIcon } from "lucide-react";
import { useState } from "react";
import { Link, useSearchParams } from "react-router";
import { request } from "@/shared/api/client";
import type { Page, Report, ReportKind } from "@/shared/api/types";
import { type Analyte, analytes, type Flag, flagOf, LAB_REPORT_ID, reportNotes } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatDate, formatNumber } from "@/shared/lib/format";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { LineChart } from "@/shared/ui/chart";
import { EmptyState, ErrorState, Loading } from "@/shared/ui/feedback";
import { PageHeader } from "@/shared/ui/panel";
import { Segmented } from "@/shared/ui/segmented";

const KINDS: Record<ReportKind, { label: string; icon: LucideIcon }> = {
  LAB: { label: "Analíticas", icon: FlaskConical },
  DIAGNOSIS: { label: "Diagnósticos", icon: Stethoscope },
  IMAGING: { label: "Imagen", icon: ScanLine },
  PRESCRIPTION: { label: "Recetas", icon: Pill },
  OTHER: { label: "Otros", icon: FileText },
};

function FlagBadge({ flag }: { flag: Flag }) {
  if (flag === "normal") return <Badge tone="success">Normal</Badge>;
  return (
    <Badge tone="warning">
      {flag === "high" ? <ArrowUp className="size-3" aria-hidden /> : <ArrowDown className="size-3" aria-hidden />}
      {flag === "high" ? "Alto" : "Bajo"}
    </Badge>
  );
}

const rangeText = (a: Analyte) => (a.low !== null && a.high !== null ? `${formatNumber(a.low)}–${formatNumber(a.high)}` : a.high !== null ? `< ${formatNumber(a.high)}` : `> ${formatNumber(a.low ?? 0)}`);

/** Dónde cae el valor respecto al rango de referencia. */
function RangeBar({ analyte }: { analyte: Analyte }) {
  const low = analyte.low ?? 0;
  const high = analyte.high ?? low * 2.5;
  const min = Math.min(low * 0.6, analyte.value * 0.9);
  const max = Math.max(high * 1.3, analyte.value * 1.1);
  const position = (v: number) => ((v - min) / (max - min)) * 100;
  const normal = flagOf(analyte) === "normal";
  return (
    <div aria-hidden className="relative h-2 w-full rounded-full bg-subtle">
      <div className="absolute inset-y-0 rounded-full bg-success-wash" style={{ left: `${position(low)}%`, width: `${position(high) - position(low)}%` }} />
      <span className={`absolute top-1/2 size-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full ring-2 ring-raised ${normal ? "bg-success" : "bg-warning"}`} style={{ left: `${position(analyte.value)}%` }} />
    </div>
  );
}

function LabResults() {
  const flagged = analytes.filter((a) => flagOf(a) !== "normal");
  const [selectedId, setSelectedId] = useState(flagged[0]?.id ?? analytes[0]!.id);
  const selected = analytes.find((a) => a.id === selectedId)!;
  const groups = [...new Set(analytes.map((a) => a.group))];

  return (
    <>
      <div className="mt-5 grid gap-3 sm:grid-cols-3">
        <div className="rounded-2xl bg-subtle p-4">
          <p className="text-sm font-semibold text-ink-soft">Valores analizados</p>
          <p className="text-2xl font-black">{analytes.length}</p>
        </div>
        <div className="rounded-2xl bg-warning-wash p-4 text-warning">
          <p className="text-sm font-semibold">Fuera de rango</p>
          <p className="text-2xl font-black">{flagged.length}</p>
        </div>
        <Link to="/panel/mensajes?c=c-1" className="flex items-center gap-3 rounded-2xl bg-subtle p-4 hover:bg-line">
          <MessageSquare className="size-5 shrink-0" aria-hidden />
          <span className="text-sm font-semibold">Revisado por tu médica. Ver su comentario</span>
        </Link>
      </div>

      {groups.map((group) => (
        <section key={group} className="mt-6">
          <h3 className="mb-2 text-sm font-bold text-ink-soft">{group}</h3>
          <ul className="grid grid-cols-1 gap-1">
            {analytes
              .filter((a) => a.group === group)
              .map((a) => (
                <li key={a.id}>
                  <button
                    type="button"
                    aria-pressed={a.id === selectedId}
                    onClick={() => setSelectedId(a.id)}
                    className={`grid w-full cursor-pointer grid-cols-[minmax(0,1fr)_auto] items-center gap-x-4 gap-y-2 rounded-2xl p-3 text-left transition-colors md:grid-cols-[minmax(0,1.3fr)_7rem_minmax(0,1fr)_5.5rem] ${a.id === selectedId ? "bg-subtle ring-1 ring-line" : "hover:bg-subtle"}`}
                  >
                    <span className="font-semibold">{a.name}</span>
                    <span className="text-right tabular-nums md:text-left">
                      <span className="font-black">{formatNumber(a.value)}</span> <span className="text-xs text-ink-soft">{a.unit}</span>
                    </span>
                    <span className="grid gap-1">
                      <RangeBar analyte={a} />
                      <span className="text-xs text-ink-soft">Referencia: {rangeText(a)}</span>
                    </span>
                    <span className="justify-self-end">
                      <FlagBadge flag={flagOf(a)} />
                    </span>
                  </button>
                </li>
              ))}
          </ul>
        </section>
      ))}

      <section className="mt-8 rounded-3xl border border-line p-5">
        <h3 className="text-lg font-extrabold">Evolución: {selected.name}</h3>
        <p className="mb-4 text-sm text-ink-soft">Tus últimas cuatro analíticas.</p>
        <LineChart
          key={selected.id}
          title={`Evolución de ${selected.name}`}
          unit={selected.unit}
          series={[{ name: selected.name, points: selected.history }]}
          band={selected.low !== null && selected.high !== null ? [selected.low, selected.high] : undefined}
        />
      </section>
    </>
  );
}

function Summary({ report }: { report: Report }) {
  const notes = reportNotes[report.publicId];
  if (!notes) return <p className="mt-5 text-ink-soft">Este informe no tiene resumen.</p>;
  return (
    <div className="mt-5 grid gap-6">
      <section>
        <h3 className="mb-1.5 text-sm font-bold text-ink-soft">Conclusión</h3>
        <p className="text-lg leading-relaxed">{notes.summary}</p>
      </section>
      <section>
        <h3 className="mb-1.5 text-sm font-bold text-ink-soft">Recomendaciones</h3>
        <ul className="grid list-disc gap-1 pl-5">
          {notes.recommendations.map((r) => (
            <li key={r}>{r}</li>
          ))}
        </ul>
      </section>
      <section>
        <h3 className="mb-2 text-sm font-bold text-ink-soft">Archivos adjuntos</h3>
        <ul className="grid grid-cols-1 gap-2">
          {notes.files.map((file) => {
            const Icon = file.name.endsWith(".jpg") ? FileImage : FileText;
            return (
              <li key={file.name} className="flex items-center gap-3 rounded-2xl bg-subtle p-3">
                <Icon className="size-5 shrink-0" aria-hidden />
                <span className="min-w-0 flex-1 truncate font-semibold">{file.name}</span>
                <span className="text-sm text-ink-soft">{file.size}</span>
                <Button variant="ghost" size="sm" onClick={() => demo("Abrir el archivo")}>
                  Ver
                </Button>
              </li>
            );
          })}
        </ul>
      </section>
    </div>
  );
}

function Detail({ report }: { report: Report }) {
  const { label, icon: Icon } = KINDS[report.kind];
  return (
    <section className="card min-w-0 p-6 md:p-8" aria-labelledby="detalle">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <Badge tone="info">
            <Icon className="size-3" aria-hidden />
            {label}
          </Badge>
          <h2 id="detalle" className="mt-2 text-2xl font-black tracking-tight">
            {report.title}
          </h2>
          <p className="mt-1 text-ink-soft">
            {report.doctorName} · {formatDate(report.issuedOn)}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="soft" size="sm" onClick={() => demo("Descargar el PDF")}>
            <Download className="size-4" aria-hidden />
            PDF
          </Button>
          <Button variant="ghost" size="sm" onClick={() => demo("Compartir con otro profesional")}>
            <Share2 className="size-4" aria-hidden />
            Compartir
          </Button>
        </div>
      </div>
      {report.publicId === LAB_REPORT_ID ? <LabResults /> : <Summary report={report} />}
    </section>
  );
}

export function ReportsPage() {
  const [kind, setKind] = useState<"" | ReportKind>("");
  const [params, setParams] = useSearchParams();
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ["reports", "me", kind],
    queryFn: () => request<Page<Report>>("/reports/me", { query: { kind } }),
  });

  const reports = data?.content ?? [];
  const selected = reports.find((r) => r.publicId === params.get("informe")) ?? reports[0];

  return (
    <>
      <PageHeader title="Resultados e informes" description="Tus analíticas, pruebas, diagnósticos y recetas, en cuanto el profesional los publica." />
      <Segmented
        label="Filtrar por tipo"
        value={kind}
        onChange={setKind}
        options={[{ value: "" as const, label: "Todos" }, ...Object.entries(KINDS).map(([value, k]) => ({ value: value as ReportKind, label: k.label }))]}
      />

      <div className="mt-5">
        {isError ? (
          <ErrorState onRetry={() => refetch()} />
        ) : isPending ? (
          <Loading label="Cargando informes" />
        ) : reports.length === 0 ? (
          <EmptyState title="Sin informes de este tipo" />
        ) : (
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-[20rem_minmax(0,1fr)]">
            <ul className="grid grid-cols-1 content-start gap-2">
              {reports.map((r) => {
                const { icon: Icon } = KINDS[r.kind];
                const active = r.publicId === selected?.publicId;
                return (
                  <li key={r.publicId}>
                    <button
                      type="button"
                      aria-current={active ? "true" : undefined}
                      onClick={() => setParams({ informe: r.publicId })}
                      className={`card flex w-full cursor-pointer items-start gap-3 rounded-3xl p-4 text-left transition-colors ${active ? "border-accent" : "hover:border-accent"}`}
                    >
                      <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-accent-wash text-accent-strong">
                        <Icon className="size-5" aria-hidden />
                      </span>
                      <span className="min-w-0">
                        <span className="block font-bold">{r.title}</span>
                        <span className="block text-sm text-ink-soft">
                          {r.doctorName} · {formatDate(r.issuedOn)}
                        </span>
                        {r.publicId === LAB_REPORT_ID && (
                          <span className="mt-1.5 block">
                            <Badge tone="warning">{analytes.filter((a) => flagOf(a) !== "normal").length} fuera de rango</Badge>
                          </span>
                        )}
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
            {selected && <Detail report={selected} />}
          </div>
        )}
      </div>
    </>
  );
}
