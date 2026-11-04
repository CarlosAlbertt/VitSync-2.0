import type { ReactNode } from "react";

export function Switch({ label, description, checked, onChange }: { label: string; description?: ReactNode; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="flex cursor-pointer items-start justify-between gap-4 py-3">
      <span className="min-w-0">
        <span className="block font-semibold">{label}</span>
        {description && <span className="block text-sm text-ink-soft">{description}</span>}
      </span>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={`relative mt-0.5 h-7 w-12 shrink-0 cursor-pointer rounded-full transition-colors ${checked ? "bg-accent" : "bg-line"}`}
      >
        <span aria-hidden className={`absolute top-1 left-1 size-5 rounded-full bg-raised transition-transform ${checked ? "translate-x-5" : ""}`} />
      </button>
    </label>
  );
}
