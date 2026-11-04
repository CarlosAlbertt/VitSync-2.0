type Option<T extends string> = { value: T; label: string; count?: number };

export function Segmented<T extends string>({ label, value, options, onChange }: { label: string; value: T; options: Option<T>[]; onChange: (value: T) => void }) {
  return (
    <div role="group" aria-label={label} className="no-scrollbar inline-flex max-w-full gap-1 overflow-x-auto rounded-2xl bg-subtle p-1">
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            aria-pressed={selected}
            onClick={() => onChange(option.value)}
            className={`flex h-10 shrink-0 cursor-pointer items-center gap-2 rounded-xl px-4 text-sm font-semibold transition-colors ${selected ? "bg-raised text-ink ring-1 ring-line" : "text-ink-soft hover:text-ink"}`}
          >
            {option.label}
            {option.count !== undefined && <span className="rounded-full bg-accent-wash px-2 text-xs font-bold text-accent-strong">{option.count}</span>}
          </button>
        );
      })}
    </div>
  );
}
