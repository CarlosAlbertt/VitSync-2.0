import { useEffect, useMemo, useRef, useState, type PointerEvent } from "react";
import { formatDate, formatDayMonth, formatNumber } from "@/shared/lib/format";

export type ChartPoint = { date: string; value: number };
export type ChartSeries = { name: string; points: ChartPoint[] };

// Las series comparten fechas (p. ej. sistólica y diastólica): el índice de un punto vale para todas.
const COLORS = ["var(--series-1)", "var(--series-2)"];
const HEIGHT = 220;
const PAD = { top: 12, right: 52, bottom: 28, left: 44 };

const time = (date: string) => new Date(`${date}T12:00:00`).getTime();

function niceStep(range: number, count: number) {
  const raw = range / count;
  const power = 10 ** Math.floor(Math.log10(raw));
  const error = raw / power;
  return (error < 1.5 ? 1 : error < 3 ? 2 : error < 7 ? 5 : 10) * power;
}

function useWidth() {
  const ref = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(640);
  useEffect(() => {
    const element = ref.current;
    if (!element || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(([entry]) => entry && setWidth(entry.contentRect.width));
    observer.observe(element);
    return () => observer.disconnect();
  }, []);
  return [ref, width] as const;
}

type Props = { title: string; series: ChartSeries[]; unit: string; band?: [number, number]; bandLabel?: string };

/** Gráfica de líneas con rango de referencia, tooltip al pasar el ratón y vista en tabla. */
export function LineChart({ title, series, unit, band, bandLabel = "Rango de referencia" }: Props) {
  const [ref, width] = useWidth();
  const [hover, setHover] = useState<number | null>(null);
  const [asTable, setAsTable] = useState(false);
  const base = useMemo(() => series[0]?.points ?? [], [series]);

  const geometry = useMemo(() => {
    const values = series.flatMap((s) => s.points.map((p) => p.value)).concat(band ?? []);
    const min = Math.min(...values);
    const max = Math.max(...values);
    const step = niceStep(Math.max(max - min, 1), 4);
    const low = Math.floor(min / step) * step;
    const high = Math.ceil(max / step) * step;
    const ticks: number[] = [];
    for (let v = low; v <= high + step / 2; v += step) ticks.push(Number(v.toFixed(6)));
    const first = time(base[0]?.date ?? "");
    const last = time(base[base.length - 1]?.date ?? "");
    const innerWidth = Math.max(width - PAD.left - PAD.right, 10);
    const innerHeight = HEIGHT - PAD.top - PAD.bottom;
    const x = (date: string) => PAD.left + (last === first ? innerWidth / 2 : ((time(date) - first) / (last - first)) * innerWidth);
    const y = (value: number) => PAD.top + (1 - (value - low) / (high - low || 1)) * innerHeight;
    return { x, y, ticks, innerWidth };
  }, [series, band, width, base]);

  if (base.length === 0) return <p className="py-8 text-center text-ink-soft">Aún no hay mediciones en este periodo.</p>;

  const { x, y, ticks, innerWidth } = geometry;
  const labelAt = [...new Set([0, Math.round((base.length - 1) / 3), Math.round(((base.length - 1) * 2) / 3), base.length - 1])];
  const latest = series.map((s) => `${s.name} ${formatNumber(s.points[s.points.length - 1]?.value ?? 0)}`).join(", ");

  function move(event: PointerEvent<SVGSVGElement>) {
    const left = event.clientX - event.currentTarget.getBoundingClientRect().left;
    let best = 0;
    base.forEach((point, i) => {
      if (Math.abs(x(point.date) - left) < Math.abs(x(base[best]!.date) - left)) best = i;
    });
    setHover(best);
  }

  const hovered = hover === null ? undefined : base[hover];

  return (
    <div>
      {(series.length > 1 || band) && (
        <ul className="mb-3 flex flex-wrap gap-x-5 gap-y-1 text-sm text-ink-soft">
          {series.length > 1 &&
            series.map((s, i) => (
              <li key={s.name} className="flex items-center gap-2">
                <span aria-hidden className="h-0.5 w-4 rounded-full" style={{ background: COLORS[i] }} />
                {s.name}
              </li>
            ))}
          {band && (
            <li className="flex items-center gap-2">
              <span aria-hidden className="size-3 rounded-sm bg-accent-wash" />
              {bandLabel}: {formatNumber(band[0])}–{formatNumber(band[1])} {unit}
            </li>
          )}
        </ul>
      )}

      {asTable ? (
        <div className="max-h-64 overflow-auto">
          <table className="w-full text-sm">
            <caption className="sr-only">{title}</caption>
            <thead>
              <tr className="text-left text-ink-soft">
                <th className="py-2 font-semibold">Fecha</th>
                {series.map((s) => (
                  <th key={s.name} className="py-2 text-right font-semibold">
                    {s.name} ({unit})
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {base
                .map((point, i) => ({ point, i }))
                .reverse()
                .map(({ point, i }) => (
                  <tr key={point.date + i} className="border-t border-line">
                    <td className="py-2">{formatDate(point.date)}</td>
                    {series.map((s) => (
                      <td key={s.name} className="py-2 text-right font-semibold tabular-nums">
                        {formatNumber(s.points[i]?.value ?? 0)}
                      </td>
                    ))}
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div ref={ref} className="relative">
          <svg width={width} height={HEIGHT} role="img" aria-label={`${title}. Último valor: ${latest} ${unit}.`} onPointerMove={move} onPointerLeave={() => setHover(null)} className="block">
            {band && <rect x={PAD.left} width={innerWidth} y={y(band[1])} height={Math.max(y(band[0]) - y(band[1]), 0)} fill="var(--accent-wash)" />}
            {ticks.map((tick) => (
              <g key={tick}>
                <line x1={PAD.left} x2={PAD.left + innerWidth} y1={y(tick)} y2={y(tick)} stroke="var(--line)" />
                <text x={PAD.left - 8} y={y(tick)} dy="0.32em" textAnchor="end" className="fill-ink-soft text-[11px]">
                  {formatNumber(tick)}
                </text>
              </g>
            ))}
            {labelAt.map((i) => (
              <text key={i} x={x(base[i]!.date)} y={HEIGHT - 8} textAnchor="middle" className="fill-ink-soft text-[11px]">
                {formatDayMonth(base[i]!.date)}
              </text>
            ))}
            {hovered && <line x1={x(hovered.date)} x2={x(hovered.date)} y1={PAD.top} y2={HEIGHT - PAD.bottom} stroke="var(--ink-soft)" strokeDasharray="3 3" />}
            {series.map((s, i) => {
              const last = s.points[s.points.length - 1];
              const path = s.points.map((p, n) => `${n ? "L" : "M"}${x(p.date).toFixed(1)},${y(p.value).toFixed(1)}`).join(" ");
              const point = hover === null ? undefined : s.points[hover];
              return (
                <g key={s.name}>
                  <path d={path} fill="none" stroke={COLORS[i]} strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />
                  {last && (
                    <>
                      <circle cx={x(last.date)} cy={y(last.value)} r={4} fill={COLORS[i]} stroke="var(--raised)" strokeWidth={2} />
                      <text x={x(last.date) + 9} y={y(last.value)} dy="0.32em" className="fill-ink text-xs font-bold">
                        {formatNumber(last.value)}
                      </text>
                    </>
                  )}
                  {point && <circle cx={x(point.date)} cy={y(point.value)} r={5} fill={COLORS[i]} stroke="var(--raised)" strokeWidth={2} />}
                </g>
              );
            })}
          </svg>
          {hovered && (
            <div className="pointer-events-none absolute top-0 z-10 min-w-36 -translate-x-1/2 rounded-xl border border-line bg-raised px-3 py-2 text-sm" style={{ left: Math.min(Math.max(x(hovered.date), 80), width - 80) }}>
              <p className="font-bold">{formatDate(hovered.date)}</p>
              {series.map((s, i) => (
                <p key={s.name} className="mt-0.5 flex items-center gap-2 text-ink-soft">
                  <span aria-hidden className="size-2.5 rounded-full" style={{ background: COLORS[i] }} />
                  {series.length > 1 && `${s.name}:`}
                  <span className="font-bold text-ink">
                    {formatNumber(s.points[hover!]?.value ?? 0)} {unit}
                  </span>
                </p>
              ))}
            </div>
          )}
        </div>
      )}

      <button type="button" onClick={() => setAsTable(!asTable)} className="mt-2 cursor-pointer text-sm font-semibold text-ink-soft underline-offset-2 hover:text-ink hover:underline">
        {asTable ? "Ver como gráfica" : "Ver como tabla"}
      </button>
    </div>
  );
}

/** Tendencia mínima para una tarjeta resumen; el valor exacto va siempre al lado en texto. */
export function Sparkline({ values, className = "h-10 w-28" }: { values: number[]; className?: string }) {
  const min = Math.min(...values);
  const max = Math.max(...values);
  const points = values.map((v, i) => `${(i / Math.max(values.length - 1, 1)) * 100},${max === min ? 50 : 90 - ((v - min) / (max - min)) * 80}`).join(" ");
  return (
    <svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden className={className}>
      <polyline points={points} fill="none" stroke="var(--series-1)" strokeWidth={2} vectorEffect="non-scaling-stroke" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}
