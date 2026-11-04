import { ArrowLeft, Check } from "lucide-react";
import type { ReactNode } from "react";
import { Link } from "react-router";
import { Logo } from "@/shared/ui/logo";

type Props = { title: string; intro?: ReactNode; wide?: boolean; children: ReactNode };

const POINTS = ["Reserva tu cita en minutos", "Tus informes siempre a mano", "Toda tu salud en un solo lugar"];

function BackLink() {
  return (
    <Link to="/" className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-ink-soft hover:text-ink">
      <ArrowLeft className="size-4" aria-hidden />
      Volver al inicio
    </Link>
  );
}

function Aside() {
  return (
    <aside className="relative hidden w-[45%] overflow-hidden bg-[url('/hero.webp')] bg-cover bg-center text-white lg:block">
      <div aria-hidden className="absolute inset-0 bg-brand/90" />
      <div className="relative flex h-full max-w-lg flex-col justify-center p-14">
        <img src="/logo.png" alt="" className="mb-6 size-16 rounded-2xl bg-white object-contain p-2" />
        <p className="font-semibold text-white/85">VitSync</p>
        <h2 className="mt-2 mb-8 text-4xl leading-tight font-black">Tu salud, en un solo lugar.</h2>
        <ul className="grid grid-cols-1 gap-3.5">
          {POINTS.map((point) => (
            <li key={point} className="flex items-center gap-3">
              <Check className="size-5 shrink-0" aria-hidden />
              {point}
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
}

export function AuthCard({ title, intro, wide, children }: Props) {
  const head = (
    <>
      <h1 className="text-3xl font-black tracking-tight">{title}</h1>
      {intro && <div className="mt-1.5 text-ink-soft">{intro}</div>}
    </>
  );

  if (wide) {
    return (
      <section className="min-h-dvh px-5 py-10">
        <div className="mx-auto max-w-2xl">
          <BackLink />
          <div className="mb-6">
            <div className="mb-5">
              <Logo />
            </div>
            {head}
          </div>
          <div className="card p-6 md:p-9">{children}</div>
        </div>
      </section>
    );
  }

  return (
    <div className="flex min-h-dvh">
      <Aside />
      <section className="flex flex-1 items-center justify-center px-5 py-10">
        <div className="w-full max-w-md">
          <BackLink />
          <div className="card p-7 md:p-10">
            <div className="mb-7">{head}</div>
            {children}
          </div>
        </div>
      </section>
    </div>
  );
}
