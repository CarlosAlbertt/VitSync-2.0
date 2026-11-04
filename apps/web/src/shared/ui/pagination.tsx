import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./button";

type Props = { page: number; totalPages: number; onChange: (page: number) => void };

export function Pagination({ page, totalPages, onChange }: Props) {
  if (totalPages <= 1) return null;
  return (
    <nav aria-label="Paginación" className="mt-6 flex items-center justify-center gap-3">
      <Button variant="soft" size="sm" aria-label="Página anterior" disabled={page === 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft className="size-4" aria-hidden />
      </Button>
      <span className="text-sm text-ink-soft tabular-nums">
        Página {page + 1} de {totalPages}
      </span>
      <Button variant="soft" size="sm" aria-label="Página siguiente" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        <ChevronRight className="size-4" aria-hidden />
      </Button>
    </nav>
  );
}
