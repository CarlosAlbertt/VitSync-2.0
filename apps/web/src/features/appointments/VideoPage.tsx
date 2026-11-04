import { CircleCheck, Clock, Mic, MicOff, Settings, Video, VideoOff, Volume2, Wifi } from "lucide-react";
import { useState } from "react";
import { useSession } from "@/shared/api/session";
import { demo } from "@/shared/lib/demo";
import { formatDay, formatTime, fullName } from "@/shared/lib/format";
import { Avatar } from "@/shared/ui/avatar";
import { Badge } from "@/shared/ui/badge";
import { Button, ButtonLink } from "@/shared/ui/button";
import { EmptyState, Skeleton } from "@/shared/ui/feedback";
import { PageHeader, Panel } from "@/shared/ui/panel";
import { useMyAppointments } from "./api";

// La sala se abre unos minutos antes; la conexión real llegará con la integración de vídeo.
const OPENS_BEFORE_MIN = 10;

function countdown(minutes: number) {
  if (minutes > 60 * 24) return `Faltan ${Math.round(minutes / (60 * 24))} días`;
  if (minutes > 60) return `Faltan ${Math.round(minutes / 60)} horas`;
  return minutes > 0 ? `Faltan ${minutes} minutos` : "La consulta ya ha empezado";
}

function ControlButton({ on, label, onClick, iconOn: On, iconOff: Off }: { on: boolean; label: string; onClick: () => void; iconOn: typeof Mic; iconOff: typeof Mic }) {
  const Icon = on ? On : Off;
  return (
    <button
      type="button"
      aria-pressed={on}
      aria-label={label}
      onClick={onClick}
      className={`grid size-13 cursor-pointer place-items-center rounded-full transition-colors ${on ? "bg-subtle text-ink hover:bg-line" : "bg-danger text-canvas"}`}
    >
      <Icon className="size-5.5" aria-hidden />
    </button>
  );
}

export function VideoPage() {
  const user = useSession((s) => s.user);
  const { data, isPending } = useMyAppointments("upcoming");
  const [camera, setCamera] = useState(false);
  const [mic, setMic] = useState(true);

  const next = data?.content.find((a) => a.modality === "TELEMEDICINE");
  const minutes = next ? Math.round((new Date(next.startsAt).getTime() - Date.now()) / 60_000) : 0;
  const canJoin = next !== undefined && minutes <= OPENS_BEFORE_MIN;

  const checks = [
    { label: "Navegador compatible", ok: true, icon: CircleCheck },
    { label: "Conexión estable", ok: true, icon: Wifi },
    { label: camera ? "Cámara funcionando" : "Cámara sin probar", ok: camera, icon: Video },
    { label: mic ? "Micrófono activado" : "Micrófono silenciado", ok: mic, icon: Mic },
  ];

  return (
    <>
      <PageHeader title="Videoconsulta" description={`Prepara tu equipo antes de entrar. La sala se abre ${OPENS_BEFORE_MIN} minutos antes de la hora.`} />
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_23rem]">
        <section className="card overflow-hidden" aria-label="Vista previa">
          <div className="relative grid aspect-video place-items-center bg-brand text-white">
            {camera ? (
              <p className="px-6 text-center font-semibold">Aquí verás tu imagen. En esta demostración no se usa la cámara.</p>
            ) : (
              <div className="text-center">
                {user && <Avatar person={user} className="mx-auto size-24 text-3xl" />}
                <p className="mt-4 font-semibold">Tu cámara está apagada</p>
              </div>
            )}
            <span className="absolute bottom-4 left-4 rounded-xl bg-black/45 px-3 py-1.5 text-sm font-semibold">{user?.firstName} (tú)</span>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-3 p-5">
            <ControlButton on={mic} label={mic ? "Silenciar micrófono" : "Activar micrófono"} onClick={() => setMic(!mic)} iconOn={Mic} iconOff={MicOff} />
            <ControlButton on={camera} label={camera ? "Apagar cámara" : "Encender cámara"} onClick={() => setCamera(!camera)} iconOn={Video} iconOff={VideoOff} />
            <Button variant="soft" onClick={() => demo("Probar el sonido")}>
              <Volume2 className="size-4.5" aria-hidden />
              Probar sonido
            </Button>
            <Button variant="ghost" onClick={() => demo("Elegir dispositivos")}>
              <Settings className="size-4.5" aria-hidden />
              Dispositivos
            </Button>
          </div>
        </section>

        <div className="grid content-start gap-6">
          <Panel title="Tu consulta">
            {isPending ? (
              <Skeleton className="h-32" />
            ) : next ? (
              <>
                <div className="flex items-center gap-3">
                  <Avatar person={next.doctor} className="size-12 text-base" />
                  <div>
                    <p className="font-bold">{fullName(next.doctor)}</p>
                    <p className="text-sm text-ink-soft">{next.doctor.specialtyName}</p>
                  </div>
                </div>
                <p className="mt-4 font-semibold first-letter:uppercase">
                  {formatDay(next.startsAt)}, {formatTime(next.startsAt)} h
                </p>
                <p className="mt-1 flex items-center gap-1.5 text-sm text-ink-soft">
                  <Clock className="size-4" aria-hidden />
                  {countdown(minutes)}
                </p>
                <Button size="lg" className="mt-5 w-full" disabled={!canJoin} onClick={() => demo("Entrar en la sala")}>
                  Entrar en la sala
                </Button>
                {!canJoin && <p className="mt-2 text-center text-sm text-ink-soft">El botón se activará {OPENS_BEFORE_MIN} minutos antes.</p>}
              </>
            ) : (
              <EmptyState title="No tienes videoconsultas próximas">
                <ButtonLink to="/reservar" size="sm" className="mt-2">
                  Pedir una
                </ButtonLink>
              </EmptyState>
            )}
          </Panel>

          <Panel title="Comprobación del equipo">
            <ul className="grid grid-cols-1 gap-2">
              {checks.map(({ label, ok, icon: Icon }) => (
                <li key={label} className="flex items-center gap-3">
                  <span className={`grid size-9 place-items-center rounded-xl ${ok ? "bg-success-wash text-success" : "bg-warning-wash text-warning"}`}>
                    <Icon className="size-4.5" aria-hidden />
                  </span>
                  <span className="flex-1 font-semibold">{label}</span>
                  <Badge tone={ok ? "success" : "warning"}>{ok ? "Listo" : "Revisar"}</Badge>
                </li>
              ))}
            </ul>
          </Panel>

          <Panel title="Consejos">
            <ul className="grid list-disc gap-1.5 pl-5 text-sm text-ink-soft">
              <li>Busca un sitio tranquilo y con buena luz.</li>
              <li>Ten a mano tu medicación y tus últimos informes.</li>
              <li>Si se corta, vuelve a entrar desde esta pantalla.</li>
            </ul>
          </Panel>
        </div>
      </div>
    </>
  );
}
