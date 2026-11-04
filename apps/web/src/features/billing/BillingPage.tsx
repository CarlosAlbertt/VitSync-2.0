import { Check, CreditCard, Download, ShieldCheck } from "lucide-react";
import { insurance, invoices, type InvoiceStatus, paymentMethod } from "@/shared/demo/patient";
import { useSession } from "@/shared/api/session";
import { demo } from "@/shared/lib/demo";
import { formatDate, formatMoney, fullName } from "@/shared/lib/format";
import { Badge, type Tone } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { PageHeader, Panel } from "@/shared/ui/panel";

const STATUS: Record<InvoiceStatus, [string, Tone]> = { pending: ["Pendiente", "warning"], paid: ["Pagada", "success"], covered: ["Cubierta por tu seguro", "info"] };

export function BillingPage() {
  const user = useSession((s) => s.user);
  const pending = invoices.filter((i) => i.status === "pending").reduce((sum, i) => sum + i.amount, 0);
  const paid = invoices.filter((i) => i.status === "paid").reduce((sum, i) => sum + i.amount, 0);
  const covered = invoices.filter((i) => i.status === "covered").length;

  return (
    <>
      <PageHeader title="Pagos y seguro" description="Tu póliza, lo que cubre y las facturas de tus consultas." />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="grid min-w-0 content-start gap-6">
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            {[
              ["Pendiente de pago", formatMoney(pending)],
              ["Pagado este año", formatMoney(paid)],
              ["Consultas cubiertas", String(covered)],
            ].map(([label, value]) => (
              <li key={label} className="card p-5">
                <p className="text-sm font-semibold text-ink-soft">{label}</p>
                <p className="mt-1 text-3xl font-black tracking-tight">{value}</p>
              </li>
            ))}
          </ul>

          <Panel title="Facturas">
            <ul className="divide-y divide-line">
              {invoices.map((invoice) => {
                const [label, tone] = STATUS[invoice.status];
                return (
                  <li key={invoice.id} className="flex flex-wrap items-center gap-3 py-4">
                    <span className="min-w-48 flex-1">
                      <span className="block font-bold">{invoice.concept}</span>
                      <span className="block text-sm text-ink-soft">
                        {invoice.id} · {formatDate(invoice.date)}
                      </span>
                    </span>
                    <span className="w-24 text-right text-lg font-black tabular-nums">{formatMoney(invoice.amount)}</span>
                    <Badge tone={tone}>{label}</Badge>
                    {invoice.status === "pending" ? (
                      <Button size="sm" onClick={() => demo("Pagar la factura")}>
                        Pagar
                      </Button>
                    ) : (
                      <Button size="sm" variant="ghost" aria-label={`Descargar ${invoice.id}`} onClick={() => demo("Descargar la factura")}>
                        <Download className="size-4" aria-hidden />
                      </Button>
                    )}
                  </li>
                );
              })}
            </ul>
          </Panel>
        </div>

        <div className="grid content-start gap-6">
          <section aria-label="Tarjeta del seguro" className="rounded-[2rem] bg-brand p-6 text-white">
            <div className="flex items-center justify-between">
              <p className="font-extrabold">{insurance.insurer}</p>
              <ShieldCheck className="size-6" aria-hidden />
            </div>
            <p className="mt-8 text-sm font-semibold">Titular</p>
            <p className="text-lg font-bold">{user && fullName(user)}</p>
            <div className="mt-4 flex justify-between gap-4">
              <div>
                <p className="text-sm font-semibold">Póliza</p>
                <p className="font-bold tabular-nums">{insurance.policy}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold">Válida hasta</p>
                <p className="font-bold">{formatDate(insurance.validUntil)}</p>
              </div>
            </div>
            <p className="mt-4 inline-block rounded-full bg-white/15 px-3 py-1 text-sm font-bold">{insurance.plan}</p>
          </section>

          <Panel title="Qué cubre tu plan">
            <ul className="grid grid-cols-1 gap-2">
              {insurance.coverage.map((item) => (
                <li key={item} className="flex items-center gap-2.5">
                  <span className="grid size-6 place-items-center rounded-full bg-success-wash text-success">
                    <Check className="size-3.5" aria-hidden />
                  </span>
                  {item}
                </li>
              ))}
            </ul>
          </Panel>

          <Panel title="Método de pago">
            <div className="flex items-center gap-3 rounded-2xl bg-subtle p-4">
              <CreditCard className="size-6 shrink-0" aria-hidden />
              <span className="flex-1">
                <span className="block font-bold">
                  {paymentMethod.brand} •••• {paymentMethod.last4}
                </span>
                <span className="block text-sm text-ink-soft">Caduca {paymentMethod.expires} · tarjeta ficticia</span>
              </span>
            </div>
            <Button variant="soft" size="sm" className="mt-3 w-full" onClick={() => demo("Cambiar el método de pago")}>
              Cambiar
            </Button>
          </Panel>
        </div>
      </div>
    </>
  );
}
