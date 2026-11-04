import { CalendarDays, ShieldCheck, TriangleAlert, UserPlus } from "lucide-react";
import { family } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatDay, formatTime, fullName } from "@/shared/lib/format";
import { Avatar } from "@/shared/ui/avatar";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { PageHeader, Panel } from "@/shared/ui/panel";

export function FamilyPage() {
  return (
    <>
      <PageHeader
        title="Familia"
        description="Gestiona la salud de las personas que dependen de ti, con su permiso."
        actions={
          <Button onClick={() => demo("Añadir un familiar")}>
            <UserPlus className="size-4.5" aria-hidden />
            Añadir familiar
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_21rem]">
        <ul className="grid grid-cols-1 content-start gap-6 md:grid-cols-2">
          {family.map((member) => (
            <li key={member.id} className="card flex flex-col gap-5 p-6">
              <div className="flex items-center gap-4">
                <Avatar person={member} className="size-16 text-xl" />
                <div className="min-w-0">
                  <h2 className="text-xl font-extrabold">{fullName(member)}</h2>
                  <p className="text-ink-soft">
                    {member.relation} · {member.age} años
                  </p>
                </div>
              </div>
              <Badge tone="info">
                <ShieldCheck className="size-3" aria-hidden />
                {member.access}
              </Badge>
              <div className="rounded-2xl bg-subtle p-4">
                <p className="flex items-center gap-2 text-sm font-semibold text-ink-soft">
                  <CalendarDays className="size-4" aria-hidden />
                  Próxima cita
                </p>
                <p className="mt-1 font-bold first-letter:uppercase">{member.next ? `${formatDay(member.next)}, ${formatTime(member.next)} h` : "Sin citas próximas"}</p>
              </div>
              {member.alerts.map((alert) => (
                <p key={alert} className="flex items-center gap-2 rounded-2xl bg-warning-wash p-3 text-sm font-bold text-warning">
                  <TriangleAlert className="size-4.5 shrink-0" aria-hidden />
                  {alert}
                </p>
              ))}
              <div className="mt-auto flex flex-wrap gap-2">
                <Button size="sm" onClick={() => demo(`Ver el área de ${member.firstName}`)}>
                  Ver su área
                </Button>
                <Button size="sm" variant="soft" onClick={() => demo(`Pedir cita para ${member.firstName}`)}>
                  Pedir cita
                </Button>
                <Button size="sm" variant="ghost" onClick={() => demo("Cambiar los permisos")}>
                  Permisos
                </Button>
              </div>
            </li>
          ))}
          <li>
            <button type="button" onClick={() => demo("Añadir un familiar")} className="grid size-full min-h-64 cursor-pointer place-content-center justify-items-center gap-3 rounded-[2rem] border-2 border-dashed border-line p-6 text-center hover:border-accent">
              <span className="grid size-14 place-items-center rounded-2xl bg-subtle">
                <UserPlus className="size-6" aria-hidden />
              </span>
              <span className="font-bold">Añadir un familiar</span>
              <span className="max-w-64 text-sm text-ink-soft">Un hijo, tu pareja o una persona mayor a tu cargo.</span>
            </button>
          </li>
        </ul>

        <Panel title="Cómo funcionan los permisos">
          <ul className="grid grid-cols-1 gap-4 text-sm">
            <li>
              <p className="font-bold">Menores de edad</p>
              <p className="text-ink-soft">Sus padres o tutores legales gestionan su cuenta: citas, vacunas e informes.</p>
            </li>
            <li>
              <p className="font-bold">Adultos</p>
              <p className="text-ink-soft">Tienen que aceptar tu invitación y elegir qué te dejan ver y hacer.</p>
            </li>
            <li>
              <p className="font-bold">Siempre revocable</p>
              <p className="text-ink-soft">Cualquiera puede retirar el permiso desde su propia área cuando quiera.</p>
            </li>
          </ul>
        </Panel>
      </div>
    </>
  );
}
