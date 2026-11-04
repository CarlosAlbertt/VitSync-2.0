import { Download, Eye, KeyRound, Laptop, Smartphone, Trash2 } from "lucide-react";
import { useState, type FormEvent } from "react";
import { accessLog, sessions as seed } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatRelative } from "@/shared/lib/format";
import { toast } from "@/shared/lib/toast";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { ConfirmDialog } from "@/shared/ui/confirm-dialog";
import { TextField } from "@/shared/ui/field";
import { PageHeader, Panel } from "@/shared/ui/panel";
import { Switch } from "@/shared/ui/switch";

export function SecurityPage() {
  const [sessions, setSessions] = useState(seed);
  const [twoFactor, setTwoFactor] = useState(false);
  const [consents, setConsents] = useState({ research: false, news: true, sharing: true });
  const [deleting, setDeleting] = useState(false);

  function changePassword(event: FormEvent) {
    event.preventDefault();
    demo("Cambiar la contraseña");
  }

  function closeSession(id: string) {
    setSessions((current) => current.filter((s) => s.id !== id));
    toast.success("Sesión cerrada en ese dispositivo.");
  }

  return (
    <>
      <PageHeader title="Seguridad y privacidad" description="Quién puede entrar en tu cuenta, quién ha visto tu historial y qué haces con tus datos." />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <Panel title="Contraseña">
          <form onSubmit={changePassword} className="grid gap-4">
            <TextField label="Contraseña actual" type="password" autoComplete="current-password" />
            <TextField label="Contraseña nueva" type="password" autoComplete="new-password" hint="Mínimo 12 caracteres, con mayúscula, número y símbolo." />
            <Button type="submit" className="justify-self-start">
              <KeyRound className="size-4.5" aria-hidden />
              Cambiar contraseña
            </Button>
          </form>
        </Panel>

        <Panel title="Verificación en dos pasos">
          <Switch
            label="Pedir un código al entrar"
            description="Además de la contraseña, te enviaremos un código por email cada vez que entres desde un dispositivo nuevo."
            checked={twoFactor}
            onChange={(checked) => {
              setTwoFactor(checked);
              demo("La verificación en dos pasos");
            }}
          />
          <p className="mt-2 text-sm text-ink-soft">Recomendado: tu cuenta guarda datos de salud.</p>
        </Panel>

        <Panel title="Sesiones abiertas">
          <ul className="divide-y divide-line">
            {sessions.map((s) => {
              const Icon = s.kind === "phone" ? Smartphone : Laptop;
              return (
                <li key={s.id} className="flex items-center gap-3 py-3">
                  <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-subtle">
                    <Icon className="size-5" aria-hidden />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block font-bold">{s.device}</span>
                    <span className="block text-sm text-ink-soft">
                      {s.place} · {s.current ? "ahora" : formatRelative(s.at)}
                    </span>
                  </span>
                  {s.current ? (
                    <Badge tone="success">Este dispositivo</Badge>
                  ) : (
                    <Button size="sm" variant="ghost" onClick={() => closeSession(s.id)}>
                      Cerrar
                    </Button>
                  )}
                </li>
              );
            })}
          </ul>
        </Panel>

        <Panel title="Quién ha visto tu historial">
          <ul className="divide-y divide-line">
            {accessLog.map((entry) => (
              <li key={entry.at} className="flex items-start gap-3 py-3">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-subtle">
                  <Eye className="size-5" aria-hidden />
                </span>
                <span className="min-w-0">
                  <span className="block font-bold">
                    {entry.who} <span className="font-normal text-ink-soft">· {entry.role}</span>
                  </span>
                  <span className="block text-sm text-ink-soft">
                    {entry.what} · {formatRelative(entry.at)}
                  </span>
                </span>
              </li>
            ))}
          </ul>
          <p className="mt-2 text-sm text-ink-soft">Si ves un acceso que no reconoces, escríbenos desde Contacto.</p>
        </Panel>

        <Panel title="Consentimientos">
          <div className="divide-y divide-line">
            <Switch label="Compartir mi historial con mis profesionales" description="Necesario para que te atiendan con toda la información." checked={consents.sharing} onChange={(sharing) => setConsents({ ...consents, sharing })} />
            <Switch label="Investigación con datos anónimos" description="Tus datos, sin nada que te identifique, para estudios médicos." checked={consents.research} onChange={(research) => setConsents({ ...consents, research })} />
            <Switch label="Novedades y consejos de salud" description="Un email al mes como máximo." checked={consents.news} onChange={(news) => setConsents({ ...consents, news })} />
          </div>
        </Panel>

        <Panel title="Tus datos">
          <p className="text-ink-soft">Puedes descargar una copia de todos tus datos o pedir que borremos tu cuenta.</p>
          <div className="mt-4 flex flex-wrap gap-2">
            <Button variant="soft" onClick={() => demo("Descargar una copia de tus datos")}>
              <Download className="size-4.5" aria-hidden />
              Descargar mis datos
            </Button>
            <Button variant="danger" onClick={() => setDeleting(true)}>
              <Trash2 className="size-4.5" aria-hidden />
              Eliminar mi cuenta
            </Button>
          </div>
        </Panel>
      </div>

      <ConfirmDialog
        open={deleting}
        title="¿Eliminar tu cuenta?"
        confirmLabel="Sí, eliminarla"
        onClose={() => setDeleting(false)}
        onConfirm={() => {
          setDeleting(false);
          demo("Eliminar la cuenta");
        }}
      >
        Perderás el acceso a tus citas, informes y mensajes. Los datos clínicos que la ley obliga a conservar se guardarán bloqueados el tiempo exigido.
      </ConfirmDialog>
    </>
  );
}
