import { Search } from "lucide-react";
import { useState } from "react";
import { useSpecialties } from "@/shared/api/catalog";
import { useDebouncedValue } from "@/shared/lib/use-debounced-value";
import { ButtonLink } from "@/shared/ui/button";
import { EmptyState, ErrorState, Skeleton } from "@/shared/ui/feedback";
import { Pagination } from "@/shared/ui/pagination";
import { SpecialtyArt } from "@/shared/ui/specialty-art";

export function SpecialtiesPage() {
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const query = useDebouncedValue(q.trim());
  const { data, isPending, isError, refetch } = useSpecialties({ q: query, page, size: 12 });

  return (
    <>
      <section>
        <div className="mx-auto max-w-3xl px-5 pt-14 pb-10 text-center md:pt-20">
          <h1 className="text-5xl font-black tracking-tighter md:text-6xl">
            Nuestras <span className="text-accent">especialidades</span>
          </h1>
          <p className="mt-4 text-lg text-ink-soft">Explora las áreas médicas y encuentra al especialista adecuado para ti.</p>
          <div className="card mt-8 flex items-center gap-3 p-2 pl-6">
            <Search className="size-5 shrink-0 text-ink-soft" aria-hidden />
            <input
              type="search"
              aria-label="Buscar especialidad"
              placeholder="Buscar especialidad"
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
              className="h-13 min-w-0 flex-1 bg-transparent text-lg text-ink outline-none placeholder:text-ink-soft"
            />
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-[90rem] px-5 lg:px-8">
        {isError ? (
          <ErrorState onRetry={() => refetch()} />
        ) : isPending ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }, (_, i) => (
              <Skeleton key={i} className="h-96" />
            ))}
          </div>
        ) : data.content.length === 0 ? (
          <EmptyState title="Sin resultados">Prueba con otra palabra.</EmptyState>
        ) : (
          <>
            <ul className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {data.content.map((s) => (
                <li key={s.publicId}>
                  <article className="card flex h-full flex-col overflow-hidden">
                    <SpecialtyArt slug={s.slug} className="aspect-[16/10] w-full" />
                    <div className="flex flex-1 flex-col gap-2 p-6">
                      <h2 className="text-xl font-extrabold">{s.name}</h2>
                      <p className="text-ink-soft">{s.description}</p>
                      <ButtonLink to={`/profesionales?especialidad=${s.slug}`} className="mt-auto">
                        Pedir cita
                      </ButtonLink>
                    </div>
                  </article>
                </li>
              ))}
            </ul>
            <Pagination page={data.page.number} totalPages={data.page.totalPages} onChange={setPage} />
          </>
        )}
      </section>
    </>
  );
}
