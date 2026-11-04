import { ButtonLink } from "@/shared/ui/button";

export function NotFoundPage() {
  return (
    <section className="mx-auto max-w-lg px-5 py-24">
      <div className="card p-10 text-center">
        <h1 className="text-3xl font-semibold">Página no encontrada</h1>
        <p className="mt-2 text-ink-soft">La dirección no existe o se ha movido.</p>
        <ButtonLink to="/" className="mt-6">
          Volver al inicio
        </ButtonLink>
      </div>
    </section>
  );
}
