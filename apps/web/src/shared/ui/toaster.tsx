import { CircleAlert, CircleCheck, Info, X } from "lucide-react";
import { useToasts } from "@/shared/lib/toast";

export function Toaster() {
  const { toasts, dismiss } = useToasts();
  return (
    <div aria-live="polite" className="fixed right-4 bottom-4 z-50 grid w-[min(24rem,calc(100vw-2rem))] gap-2">
      {toasts.map((t) => (
        <div key={t.id} className="card appear flex items-start gap-3 rounded-xl p-4 text-sm">
          {t.tone === "success" ? (
            <CircleCheck className="size-5 shrink-0 text-success" aria-hidden />
          ) : t.tone === "info" ? (
            <Info className="size-5 shrink-0 text-ink-soft" aria-hidden />
          ) : (
            <CircleAlert className="size-5 shrink-0 text-danger" aria-hidden />
          )}
          <span className="flex-1">{t.text}</span>
          <button type="button" onClick={() => dismiss(t.id)} aria-label="Cerrar aviso" className="cursor-pointer text-ink-soft hover:text-ink">
            <X className="size-4" aria-hidden />
          </button>
        </div>
      ))}
    </div>
  );
}
