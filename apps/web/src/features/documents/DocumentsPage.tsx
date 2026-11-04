import { FileImage, FilePen, FileText, Upload } from "lucide-react";
import { useRef, useState, type DragEvent } from "react";
import { type DocumentCategory, documents as seed, type PatientDocument } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { formatDate } from "@/shared/lib/format";
import { toast } from "@/shared/lib/toast";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/feedback";
import { PageHeader, Panel } from "@/shared/ui/panel";
import { Segmented } from "@/shared/ui/segmented";

const CATEGORIES: ("Todos" | DocumentCategory)[] = ["Todos", "Informes", "Consentimientos", "Identificación", "Otros"];

const size = (bytes: number) => (bytes > 1_000_000 ? `${(bytes / 1_000_000).toFixed(1).replace(".", ",")} MB` : `${Math.max(Math.round(bytes / 1000), 1)} KB`);

export function DocumentsPage() {
  const [docs, setDocs] = useState(seed);
  const [category, setCategory] = useState<(typeof CATEGORIES)[number]>("Todos");
  const [dragging, setDragging] = useState(false);
  const input = useRef<HTMLInputElement>(null);

  const pending = docs.filter((d) => d.pendingSignature);
  const visible = category === "Todos" ? docs : docs.filter((d) => d.category === category);

  // Prototipo: el archivo se lista, pero no sale del navegador.
  function add(files: FileList | null) {
    if (!files?.length) return;
    const added: PatientDocument[] = [...files].map((file) => ({ id: crypto.randomUUID(), name: file.name, category: "Otros", size: size(file.size), date: new Date().toISOString(), source: "Tú" }));
    setDocs((current) => [...added, ...current]);
    toast.success(`${added.length === 1 ? "Documento añadido" : `${added.length} documentos añadidos`}. En la demostración no se suben a ningún servidor.`);
  }

  function drop(event: DragEvent) {
    event.preventDefault();
    setDragging(false);
    add(event.dataTransfer.files);
  }

  function sign(id: string) {
    setDocs((current) => current.map((d) => (d.id === id ? { ...d, pendingSignature: false } : d)));
    toast.success("Documento firmado. Te hemos enviado una copia por email.");
  }

  return (
    <>
      <PageHeader title="Documentos" description="Informes de otros centros, consentimientos y documentación que quieras compartir con tu equipo médico." />

      {pending.length > 0 && (
        <div className="mb-6 flex flex-wrap items-center gap-4 rounded-3xl bg-warning-wash p-5 text-warning">
          <FilePen className="size-6 shrink-0" aria-hidden />
          <p className="min-w-56 flex-1 font-bold">
            Tienes {pending.length} documento pendiente de firma: {pending.map((d) => d.name.replace(".pdf", "")).join(", ")}.
          </p>
          <Button size="sm" onClick={() => sign(pending[0]!.id)}>
            Firmar ahora
          </Button>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
        <div className="min-w-0">
          <div className="mb-4">
            <Segmented label="Categoría" value={category} onChange={setCategory} options={CATEGORIES.map((c) => ({ value: c, label: c }))} />
          </div>
          {visible.length === 0 ? (
            <EmptyState title="No hay documentos en esta categoría" />
          ) : (
            <ul className="grid grid-cols-1 gap-3">
              {visible.map((d) => {
                const Icon = d.name.match(/\.(jpe?g|png|webp)$/i) ? FileImage : FileText;
                return (
                  <li key={d.id} className="card flex flex-wrap items-center gap-4 rounded-3xl p-4">
                    <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-accent-wash text-accent-strong">
                      <Icon className="size-6" aria-hidden />
                    </span>
                    <span className="min-w-48 flex-1">
                      <span className="block truncate font-bold">{d.name}</span>
                      <span className="block text-sm text-ink-soft">
                        {d.category} · {d.size} · {formatDate(d.date)} · subido por {d.source === "Tú" ? "ti" : "VitSync"}
                      </span>
                    </span>
                    {d.pendingSignature ? (
                      <>
                        <Badge tone="warning">Pendiente de firma</Badge>
                        <Button size="sm" onClick={() => sign(d.id)}>
                          Firmar
                        </Button>
                      </>
                    ) : (
                      <>
                        <Button variant="ghost" size="sm" onClick={() => demo("Abrir el documento")}>
                          Ver
                        </Button>
                        <Button variant="soft" size="sm" onClick={() => demo("Descargar el documento")}>
                          Descargar
                        </Button>
                      </>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        <div className="grid content-start gap-6">
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={drop}
            className={`grid justify-items-center gap-3 rounded-[2rem] border-2 border-dashed p-8 text-center transition-colors ${dragging ? "border-accent bg-accent-wash" : "border-line"}`}
          >
            <span className="grid size-14 place-items-center rounded-2xl bg-subtle">
              <Upload className="size-6" aria-hidden />
            </span>
            <p className="font-bold">Arrastra aquí tus archivos</p>
            <p className="text-sm text-ink-soft">PDF o imagen, hasta 20 MB</p>
            <Button size="sm" onClick={() => input.current?.click()}>
              Elegir archivos
            </Button>
            <input ref={input} type="file" multiple accept=".pdf,image/*" className="sr-only" tabIndex={-1} onChange={(e) => add(e.target.files)} />
          </div>

          <Panel title="Espacio">
            <div className="h-2 rounded-full bg-subtle" aria-hidden>
              <div className="h-full w-[2%] rounded-full bg-accent" />
            </div>
            <p className="mt-2 text-sm text-ink-soft">18 MB usados de 1 GB</p>
          </Panel>
        </div>
      </div>
    </>
  );
}
