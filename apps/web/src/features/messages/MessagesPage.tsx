import { ArrowLeft, CalendarPlus, Paperclip, PenSquare, Search, Send, TriangleAlert } from "lucide-react";
import { useEffect, useMemo, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { Link, useSearchParams } from "react-router";
import { conversations as seed, type Conversation } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatRelative, formatTime, fullName } from "@/shared/lib/format";
import { toast } from "@/shared/lib/toast";
import { Avatar } from "@/shared/ui/avatar";
import { Button } from "@/shared/ui/button";
import { PageHeader } from "@/shared/ui/panel";

function Thread({ conversation, onBack, onSend }: { conversation: Conversation; onBack: () => void; onSend: (text: string) => void }) {
  const [draft, setDraft] = useState("");
  const scroller = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scroller.current?.scrollTo({ top: scroller.current.scrollHeight });
  }, [conversation.messages.length, conversation.id]);

  function submit(event?: FormEvent) {
    event?.preventDefault();
    if (!draft.trim()) return;
    onSend(draft.trim());
    setDraft("");
  }

  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex items-center gap-3 border-b border-line p-4">
        <button type="button" aria-label="Volver a la lista" onClick={onBack} className="grid size-10 cursor-pointer place-items-center rounded-xl hover:bg-subtle lg:hidden">
          <ArrowLeft className="size-5" aria-hidden />
        </button>
        <Avatar person={conversation.with} className="size-11 text-sm" />
        <div className="min-w-0 flex-1">
          <p className="truncate font-bold">{fullName(conversation.with)}</p>
          <p className="truncate text-sm text-ink-soft">
            {conversation.specialty} · {conversation.subject}
          </p>
        </div>
        {conversation.doctorId && (
          <Link to={`/reservar?profesional=${conversation.doctorId}`} className="hidden items-center gap-1.5 rounded-xl px-3 py-2 text-sm font-bold hover:bg-subtle sm:flex">
            <CalendarPlus className="size-4" aria-hidden />
            Pedir cita
          </Link>
        )}
      </div>

      <div ref={scroller} className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto p-5" aria-live="polite">
        {conversation.messages.map((message) => {
          const mine = message.from === "me";
          return (
            <div key={message.id} className={`max-w-[85%] md:max-w-[70%] ${mine ? "self-end" : "self-start"}`}>
              <p className={`rounded-3xl px-4 py-3 leading-relaxed ${mine ? "rounded-br-lg bg-accent text-accent-ink" : "rounded-bl-lg bg-subtle"}`}>{message.text}</p>
              <p className={`mt-1 text-xs text-ink-soft ${mine ? "text-right" : ""}`}>
                {formatRelative(message.at)}, {formatTime(message.at)}
              </p>
            </div>
          );
        })}
      </div>

      <form onSubmit={submit} className="flex items-end gap-2 border-t border-line p-3">
        <button type="button" aria-label="Adjuntar archivo" onClick={() => demo("Adjuntar archivos")} className="grid size-12 shrink-0 cursor-pointer place-items-center rounded-xl text-ink-soft hover:bg-subtle hover:text-ink">
          <Paperclip className="size-5" aria-hidden />
        </button>
        <textarea
          aria-label="Escribe tu mensaje"
          placeholder="Escribe tu mensaje…"
          rows={1}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={onKeyDown}
          className="max-h-32 min-h-12 flex-1 resize-none rounded-xl border border-line bg-raised px-4 py-3 text-ink outline-none placeholder:text-ink-soft focus:border-accent"
        />
        <Button type="submit" aria-label="Enviar" disabled={!draft.trim()} className="size-12 shrink-0 px-0">
          <Send className="size-5" aria-hidden />
        </Button>
      </form>
    </div>
  );
}

export function MessagesPage() {
  const [params, setParams] = useSearchParams();
  const [threads, setThreads] = useState(seed);
  const [query, setQuery] = useState("");
  const activeId = params.get("c");
  const active = threads.find((t) => t.id === activeId) ?? threads[0];

  // Abrir una conversación la marca como leída.
  useEffect(() => {
    if (!active?.unread) return;
    setThreads((current) => current.map((t) => (t.id === active.id ? { ...t, unread: 0 } : t)));
  }, [active?.id, active?.unread]);

  const visible = useMemo(() => {
    const term = query.trim().toLowerCase();
    return term ? threads.filter((t) => `${fullName(t.with)} ${t.subject} ${t.specialty}`.toLowerCase().includes(term)) : threads;
  }, [threads, query]);

  function send(text: string) {
    if (!active) return;
    const message = { id: crypto.randomUUID(), from: "me" as const, text, at: new Date().toISOString() };
    setThreads((current) => current.map((t) => (t.id === active.id ? { ...t, messages: [...t.messages, message] } : t)));
    toast.success("Mensaje enviado. Te responderán en un máximo de 48 horas laborables.");
  }

  return (
    <>
      <PageHeader
        title="Mensajes"
        description="Habla con tu equipo médico sin tener que pedir cita."
        actions={
          <Button onClick={() => demo("Nuevo mensaje")}>
            <PenSquare className="size-4.5" aria-hidden />
            Nuevo mensaje
          </Button>
        }
      />

      <p role="note" className="mb-4 flex items-start gap-3 rounded-2xl bg-warning-wash p-4 text-sm font-semibold text-warning">
        <TriangleAlert className="mt-0.5 size-5 shrink-0" aria-hidden />
        Este canal no es para urgencias. Si es una urgencia, llama al 112.
      </p>

      <div className="card flex h-[calc(100dvh-19rem)] min-h-[32rem] overflow-hidden">
        <div className={`w-full flex-col border-line lg:flex lg:w-80 lg:border-r ${activeId ? "hidden" : "flex"}`}>
          <div className="border-b border-line p-3">
            <label className="flex h-11 items-center gap-2.5 rounded-xl bg-subtle px-3.5">
              <Search className="size-4.5 shrink-0 text-ink-soft" aria-hidden />
              <input type="search" aria-label="Buscar conversaciones" placeholder="Buscar" value={query} onChange={(e) => setQuery(e.target.value)} className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-ink-soft" />
            </label>
          </div>
          <ul className="flex-1 overflow-y-auto p-2">
            {visible.map((t) => {
              const lastMessage = t.messages[t.messages.length - 1]!;
              const selected = t.id === active?.id;
              return (
                <li key={t.id}>
                  <button
                    type="button"
                    aria-current={selected ? "true" : undefined}
                    onClick={() => setParams({ c: t.id })}
                    className={`flex w-full cursor-pointer gap-3 rounded-2xl p-3 text-left transition-colors ${selected ? "bg-subtle" : "hover:bg-subtle"}`}
                  >
                    <Avatar person={t.with} className="size-11 text-sm" />
                    <span className="min-w-0 flex-1">
                      <span className="flex items-center justify-between gap-2">
                        <span className="truncate font-bold">{fullName(t.with)}</span>
                        <span className="shrink-0 text-xs text-ink-soft">{formatRelative(lastMessage.at)}</span>
                      </span>
                      <span className="block truncate text-sm font-semibold">{t.subject}</span>
                      <span className={`block truncate text-sm ${t.unread ? "text-ink" : "text-ink-soft"}`}>{lastMessage.text}</span>
                    </span>
                    {t.unread > 0 && <span aria-label="Sin leer" className="mt-2 size-2.5 shrink-0 rounded-full bg-danger" />}
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
        <div className={`min-w-0 flex-1 flex-col lg:flex ${activeId ? "flex" : "hidden"}`}>
          {active && <Thread conversation={active} onBack={() => setParams({})} onSend={send} />}
        </div>
      </div>
    </>
  );
}
