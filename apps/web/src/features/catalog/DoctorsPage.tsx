import { useSearchParams } from "react-router";
import { useDoctors, useFacilities, useSpecialties } from "@/shared/api/catalog";
import { DoctorCard } from "@/shared/ui/doctor-card";
import { EmptyState, ErrorState, Skeleton } from "@/shared/ui/feedback";
import { SelectField } from "@/shared/ui/field";
import { Pagination } from "@/shared/ui/pagination";

export function DoctorsPage() {
  const [params, setParams] = useSearchParams();
  const specialty = params.get("especialidad") ?? "";
  const facility = params.get("centro") ?? "";
  const page = Number(params.get("pagina") ?? 0);

  const specialties = useSpecialties({ size: 50 });
  const facilities = useFacilities();
  const { data, isPending, isError, refetch } = useDoctors({ specialty, facility, page, size: 9 });

  function update(key: string, value: string) {
    setParams((current) => {
      const merged = new URLSearchParams(current);
      if (key !== "pagina") merged.delete("pagina");
      if (value) merged.set(key, value);
      else merged.delete(key);
      return merged;
    });
  }

  return (
    <>
      <section>
        <div className="mx-auto max-w-[90rem] px-5 lg:px-8 pt-12 pb-8 md:pt-16">
          <h1 className="text-5xl font-black tracking-tighter md:text-6xl">
            Cuadro <span className="text-accent">médico</span>
          </h1>
          <p className="mt-4 max-w-2xl text-lg text-ink-soft">Conoce a nuestros profesionales y reserva con quien mejor te encaje.</p>
          <div className="card mt-7 grid gap-4 p-5 sm:grid-cols-2 md:max-w-3xl">
            <SelectField label="Especialidad" value={specialty} onChange={(e) => update("especialidad", e.target.value)}>
              <option value="">Todas</option>
              {specialties.data?.content.map((s) => (
                <option key={s.publicId} value={s.slug}>
                  {s.name}
                </option>
              ))}
            </SelectField>
            <SelectField label="Centro" value={facility} onChange={(e) => update("centro", e.target.value)}>
              <option value="">Todos</option>
              {facilities.data?.content.map((f) => (
                <option key={f.publicId} value={f.publicId}>
                  {f.name}, {f.city}
                </option>
              ))}
            </SelectField>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-[90rem] px-5 lg:px-8 pt-4">
        {isError ? (
          <ErrorState onRetry={() => refetch()} />
        ) : isPending ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }, (_, i) => (
              <Skeleton key={i} className="h-96" />
            ))}
          </div>
        ) : data.content.length === 0 ? (
          <EmptyState title="Sin resultados">Prueba con otro centro o especialidad.</EmptyState>
        ) : (
          <>
            <p aria-live="polite" className="mb-4 font-semibold text-ink-soft">
              {data.page.totalElements} {data.page.totalElements === 1 ? "profesional" : "profesionales"}
            </p>
            <ul className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
              {data.content.map((d) => (
                <li key={d.publicId}>
                  <DoctorCard doctor={d} />
                </li>
              ))}
            </ul>
            <Pagination page={data.page.number} totalPages={data.page.totalPages} onChange={(p) => update("pagina", String(p))} />
          </>
        )}
      </section>
    </>
  );
}
