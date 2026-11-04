import { CalendarDays, CheckCheck, CreditCard, FlaskConical, type LucideIcon, MessageSquare, Pill, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router";
import { type NoticeKind, notices as seed } from "@/shared/demo/patient";
import { formatRelative } from "@/shared/lib/format";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/feedback";
import { PageHeader, Panel } from "@/shared/ui/panel";
import { Segmented } from "@/shared/ui/segmented";

const KINDS: Record<NoticeKind, { label: string; icon: LucideIcon }> = {
  appointment: { label: "Citas", icon: CalendarDays },
  result: { label: "Resultados", icon: FlaskConical },
  message: { label: "Mensajes", icon: MessageSquare },
  medication: { label: "Medicación", icon: Pill },
  billing: { label: "Pagos", icon: CreditCard },
  security: { label: "Seguridad", icon: ShieldCheck },
};

const CHANNELS = ["Email", "SMS", "App"] as const;

export function NotificationsPage() {
  const [items, setItems] = useState(seed);
  const [filter, setFilter] = useState<"all" | "unread">("all");
  const [prefs, setPrefs] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(Object.keys(KINDS).flatMap((kind) => CHANNELS.map((channel) => [`${kind}.${channel}`, channel !== "SMS" || kind === "appointment"]))),
  );

  const unread = items.filter((n) => !n.read).length;
  const visible = filter === "unread" ? items.filter((n) => !n.read) : items;

  return (
    <>
      <PageHeader
        title="Notificaciones"
        description="Avisos de tus citas, resultados, mensajes y medicación."
        actions={
          <Button variant="soft" disabled={unread === 0} onClick={() => setItems(items.map((n) => ({ ...n, read: true })))}>
            <CheckCheck className="size-4.5" aria-hidden />
            Marcar todo como leído
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_24rem]">
        <div className="min-w-0">
          <div className="mb-4">
            <Segmented
              label="Filtro"
              value={filter}
              onChange={setFilter}
              options={[
                { value: "all", label: "Todas" },
                { value: "unread", label: "Sin leer", count: unread },
              ]}
            />
          </div>
          {visible.length === 0 ? (
            <EmptyState title="Estás al día">No tienes notificaciones sin leer.</EmptyState>
          ) : (
            <ul className="grid grid-cols-1 gap-3">
              {visible.map((notice) => {
                const { icon: Icon } = KINDS[notice.kind];
                return (
                  <li key={notice.id}>
                    <Link
                      to={notice.to}
                      onClick={() => setItems(items.map((n) => (n.id === notice.id ? { ...n, read: true } : n)))}
                      className={`card flex items-start gap-4 rounded-3xl p-4 transition-colors hover:border-accent ${notice.read ? "" : "border-accent"}`}
                    >
                      <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-accent-wash text-accent-strong">
                        <Icon className="size-5" aria-hidden />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="flex flex-wrap items-center justify-between gap-2">
                          <span className="font-bold">{notice.title}</span>
                          <span className="text-xs text-ink-soft">{formatRelative(notice.at)}</span>
                        </span>
                        <span className="block text-sm text-ink-soft">{notice.text}</span>
                      </span>
                      {!notice.read && <span aria-label="Sin leer" className="mt-2 size-2.5 shrink-0 rounded-full bg-danger" />}
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        <Panel title="Cómo quieres que te avisemos">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-ink-soft">
                <th className="py-2 text-left font-semibold">Aviso</th>
                {CHANNELS.map((c) => (
                  <th key={c} className="py-2 font-semibold">
                    {c}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {Object.entries(KINDS).map(([kind, { label }]) => (
                <tr key={kind} className="border-t border-line">
                  <th scope="row" className="py-3 text-left font-semibold">
                    {label}
                  </th>
                  {CHANNELS.map((channel) => {
                    const key = `${kind}.${channel}`;
                    return (
                      <td key={channel} className="py-3 text-center">
                        <input type="checkbox" aria-label={`${label} por ${channel}`} checked={prefs[key] ?? false} onChange={() => setPrefs({ ...prefs, [key]: !prefs[key] })} className="size-5 cursor-pointer accent-[var(--accent)]" />
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      </div>
    </>
  );
}
