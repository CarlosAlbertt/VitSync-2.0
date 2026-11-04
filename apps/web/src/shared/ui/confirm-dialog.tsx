import { useEffect, useRef, type ReactNode } from "react";
import { Button } from "./button";

type Props = {
  open: boolean;
  title: string;
  children: ReactNode;
  confirmLabel: string;
  busy?: boolean;
  onConfirm: () => void;
  onClose: () => void;
};

// <dialog> nativo: trae foco atrapado, Escape y fondo inerte sin librerías.
export function ConfirmDialog({ open, title, children, confirmLabel, busy, onConfirm, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      aria-labelledby="confirm-title"
      className="card m-auto w-[min(28rem,calc(100vw-2rem))] rounded-xl p-7 text-ink"
    >
      <h2 id="confirm-title" className="text-xl font-semibold">
        {title}
      </h2>
      <div className="mt-2 text-ink-soft">{children}</div>
      <div className="mt-6 flex justify-end gap-2">
        <Button variant="ghost" onClick={onClose}>
          Volver
        </Button>
        <Button variant="danger" disabled={busy} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </dialog>
  );
}
