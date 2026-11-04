import { useId, type InputHTMLAttributes, type ReactNode, type Ref, type SelectHTMLAttributes, type TextareaHTMLAttributes } from "react";

const CONTROL =
  "w-full rounded-xl border border-line bg-raised px-3.5 text-ink placeholder:text-ink-soft transition-colors hover:border-ink/30 focus:border-accent aria-[invalid=true]:border-danger";

type FieldProps = { label: string; error?: string; hint?: ReactNode };

function Frame({ id, label, error, hint, children }: FieldProps & { id: string; children: ReactNode }) {
  return (
    <div className="grid gap-1.5">
      <label htmlFor={id} className="text-sm font-semibold">
        {label}
      </label>
      {children}
      {hint && !error && (
        <p id={`${id}-hint`} className="text-[0.82rem] text-ink-soft">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="text-[0.82rem] font-medium text-danger">
          {error}
        </p>
      )}
    </div>
  );
}

const describedBy = (id: string, { error, hint }: FieldProps) =>
  error ? `${id}-error` : hint ? `${id}-hint` : undefined;

export function TextField({ label, error, hint, ref, ...props }: FieldProps & InputHTMLAttributes<HTMLInputElement> & { ref?: Ref<HTMLInputElement> }) {
  const id = useId();
  return (
    <Frame id={id} label={label} error={error} hint={hint}>
      <input ref={ref} id={id} aria-invalid={Boolean(error)} aria-describedby={describedBy(id, { label, error, hint })} className={`${CONTROL} h-12`} {...props} />
    </Frame>
  );
}

export function TextAreaField({ label, error, hint, ref, ...props }: FieldProps & TextareaHTMLAttributes<HTMLTextAreaElement> & { ref?: Ref<HTMLTextAreaElement> }) {
  const id = useId();
  return (
    <Frame id={id} label={label} error={error} hint={hint}>
      <textarea ref={ref} id={id} rows={3} aria-invalid={Boolean(error)} aria-describedby={describedBy(id, { label, error, hint })} className={`${CONTROL} py-2.5`} {...props} />
    </Frame>
  );
}

export function SelectField({ label, error, hint, children, ...props }: FieldProps & SelectHTMLAttributes<HTMLSelectElement>) {
  const id = useId();
  return (
    <Frame id={id} label={label} error={error} hint={hint}>
      <select id={id} aria-invalid={Boolean(error)} className={`${CONTROL} h-12`} {...props}>
        {children}
      </select>
    </Frame>
  );
}
