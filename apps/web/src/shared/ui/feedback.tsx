import { CircleAlert } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "./button";

export function Skeleton({ className = "" }: { className?: string }) {
  return <div aria-hidden className={`skeleton ${className}`} />;
}

export function Loading({ label = "Cargando" }: { label?: string }) {
  return (
    <div role="status" className="grid gap-3 py-4">
      <span className="sr-only">{label}…</span>
      <Skeleton className="h-16" />
      <Skeleton className="h-16" />
      <Skeleton className="h-16" />
    </div>
  );
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-line px-6 py-10 text-center">
      <h3 className="text-lg font-medium">{title}</h3>
      {children && <div className="mx-auto mt-1.5 max-w-md text-ink-soft">{children}</div>}
    </div>
  );
}

export function ErrorState({ onRetry }: { onRetry?: () => void }) {
  return (
    <div role="alert" className="flex items-start gap-3 rounded-xl bg-subtle p-5">
      <CircleAlert className="mt-0.5 size-5 shrink-0 text-danger" aria-hidden />
      <div>
        <p className="font-medium">No hemos podido cargar esta información.</p>
        <p className="mt-0.5 text-sm text-ink-soft">Inténtalo de nuevo en unos minutos.</p>
        {onRetry && (
          <Button variant="soft" size="sm" className="mt-3" onClick={onRetry}>
            Reintentar
          </Button>
        )}
      </div>
    </div>
  );
}

export function FormError({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="flex items-start gap-2.5 rounded-xl bg-subtle p-3.5 text-sm font-medium">
      <CircleAlert className="mt-px size-4.5 shrink-0 text-danger" aria-hidden />
      <span>{children}</span>
    </p>
  );
}
