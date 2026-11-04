import { ArrowRight } from "lucide-react";
import type { ReactNode } from "react";
import { Link } from "react-router";

export function PageHeader({ title, description, actions }: { title: string; description?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        <h1 className="text-3xl font-black tracking-tight md:text-4xl">{title}</h1>
        {description && <p className="mt-1.5 max-w-2xl text-ink-soft">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

type Props = { title: string; to?: string; linkLabel?: string; actions?: ReactNode; children: ReactNode; className?: string };

/** Bloque con título de un panel. Si lleva `to`, enlaza a la pantalla completa. */
export function Panel({ title, to, linkLabel = "Ver todo", actions, children, className = "" }: Props) {
  return (
    <section className={`card min-w-0 p-6 ${className}`}>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-extrabold">{title}</h2>
        {actions}
        {to && (
          <Link to={to} className="inline-flex shrink-0 items-center gap-1 text-sm font-bold text-accent hover:underline">
            {linkLabel}
            <ArrowRight className="size-3.5" aria-hidden />
          </Link>
        )}
      </div>
      {children}
    </section>
  );
}
