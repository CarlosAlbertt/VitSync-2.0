import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Check } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { useDoctors, useSlots, useSpecialties } from "@/shared/api/catalog";
import type { Doctor, Modality, Slot, Specialty } from "@/shared/api/types";
import { errorMessage, hasCode } from "@/shared/lib/errors";
import { formatDay, formatMonth, formatShortDay, formatTime, fullName, nextWorkdays, toDateKey } from "@/shared/lib/format";
import { toast } from "@/shared/lib/toast";
import { Avatar } from "@/shared/ui/avatar";
import { Button } from "@/shared/ui/button";
import { EmptyState, ErrorState, FormError, Loading, Skeleton } from "@/shared/ui/feedback";
import { SelectField, TextAreaField } from "@/shared/ui/field";
import { SpecialtyArt } from "@/shared/ui/specialty-art";
import { useBookAppointment } from "./api";

const STEPS = ["Especialidad", "Profesional", "Horario", "Confirmación"];

function Progress({ step }: { step: number }) {
  return (
    <ol aria-label="Pasos de la reserva" className="flex items-center gap-2">
      {STEPS.map((label, i) => (
        <li key={label} aria-current={i === step ? "step" : undefined} className="flex flex-1 items-center gap-2">
          <span className={`grid size-8 shrink-0 place-items-center rounded-full text-sm font-bold tabular-nums ${i === step ? "bg-white text-brand" : i < step ? "bg-accent-wash text-brand" : "bg-white/20 text-white"}`}>
            {i < step ? <Check className="size-3.5" aria-hidden /> : i + 1}
          </span>
          <span className={`hidden text-sm text-white md:block ${i === step ? "font-bold" : ""}`}>{label}</span>
          {i < STEPS.length - 1 && <span aria-hidden className={`h-1 flex-1 rounded-full ${i < step ? "bg-accent-wash" : "bg-white/20"}`} />}
        </li>
      ))}
    </ol>
  );
}

function SpecialtyStep({ onPick }: { onPick: (specialty: Specialty) => void }) {
  const { data, isPending, isError, refetch } = useSpecialties({ size: 50 });
  if (isError) return <ErrorState onRetry={() => refetch()} />;
  if (isPending) return <Loading />;
  return (
    <ul className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
      {data.content.map((s) => (
        <li key={s.publicId}>
          <button type="button" onClick={() => onPick(s)} className="tile flex size-full cursor-pointer items-center gap-3 rounded-2xl p-3 text-left">
            <SpecialtyArt slug={s.slug} className="size-14 shrink-0 rounded-xl" />
            <span className="font-semibold">{s.name}</span>
          </button>
        </li>
      ))}
    </ul>
  );
}

function DoctorStep({ specialty, onPick }: { specialty: string; onPick: (doctor: Doctor) => void }) {
  const { data, isPending, isError, refetch } = useDoctors({ specialty, size: 20 });
  if (isError) return <ErrorState onRetry={() => refetch()} />;
  if (isPending) return <Loading />;
  if (data.content.length === 0) return <EmptyState title="Sin profesionales disponibles">Elige otra especialidad.</EmptyState>;
  return (
    <ul className="grid grid-cols-1 gap-2.5">
      {data.content.map((d) => (
        <li key={d.publicId}>
          <button type="button" onClick={() => onPick(d)} className="tile flex w-full cursor-pointer items-center gap-4 rounded-2xl p-4 text-left">
            <Avatar person={d} />
            <span>
              <span className="block font-semibold">{fullName(d)}</span>
              <span className="mt-0.5 block text-sm text-ink-soft">{d.facilities.map((f) => f.name).join(", ")}</span>
            </span>
          </button>
        </li>
      ))}
    </ul>
  );
}

function SlotStep({ doctor, notice, onPick }: { doctor: Doctor; notice: string | null; onPick: (slot: Slot) => void }) {
  const days = useMemo(() => nextWorkdays(10), []);
  const [day, setDay] = useState(() => toDateKey(days[0]!));
  const { data, isPending, isError, refetch } = useSlots(doctor.publicId, day);

  return (
    <div className="grid gap-5">
      {notice && <FormError>{notice}</FormError>}
      <div role="group" aria-label="Día" className="no-scrollbar flex gap-2 overflow-x-auto">
        {days.map((d) => {
          const key = toDateKey(d);
          const selected = key === day;
          return (
            <button
              key={key}
              type="button"
              aria-pressed={selected}
              onClick={() => setDay(key)}
              className={`w-18 shrink-0 cursor-pointer rounded-2xl border py-2.5 text-center transition-colors ${selected ? "border-accent bg-accent text-accent-ink" : "border-line bg-raised hover:border-accent"}`}
            >
              <span className="block text-xs first-letter:uppercase">{formatMonth(d.toISOString())}</span>
              <span className="block text-sm font-medium first-letter:uppercase">{formatShortDay(d.toISOString())}</span>
            </button>
          );
        })}
      </div>

      {isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : isPending ? (
        <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-5">
          {Array.from({ length: 10 }, (_, i) => (
            <Skeleton key={i} className="h-11" />
          ))}
        </div>
      ) : data.length === 0 ? (
        <EmptyState title="Sin huecos este día">Prueba con otro día.</EmptyState>
      ) : (
        <ul aria-label="Horas disponibles" className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-5">
          {data.map((slot) => (
            <li key={slot.startsAt}>
              <button type="button" onClick={() => onPick(slot)} className="tile h-12 w-full cursor-pointer rounded-xl font-semibold tabular-nums">
                {formatTime(slot.startsAt)}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

type Confirmation = { modality: Modality; facilityId?: string; reason?: string };

function ConfirmStep({ doctor, busy, error, onConfirm }: { doctor: Doctor; busy: boolean; error: string | null; onConfirm: (values: Confirmation) => void }) {
  const [modality, setModality] = useState<Modality>("IN_PERSON");
  const [facilityId, setFacilityId] = useState(doctor.facilities[0]?.publicId);
  const [reason, setReason] = useState("");

  return (
    <div className="grid gap-4">
      {error && <FormError>{error}</FormError>}
      <fieldset>
        <legend className="text-sm font-medium">Tipo de consulta</legend>
        <div className="mt-1.5 grid grid-cols-2 gap-2">
          {(
            [
              ["IN_PERSON", "Presencial"],
              ["TELEMEDICINE", "Por vídeo"],
            ] as const
          ).map(([value, label]) => (
            <label
              key={value}
              className={`flex h-12 cursor-pointer items-center justify-center rounded-xl border text-sm transition-colors has-focus-visible:outline-2 has-focus-visible:outline-accent ${modality === value ? "border-accent bg-accent-wash font-semibold text-accent-strong" : "border-line bg-raised hover:border-accent"}`}
            >
              <input type="radio" name="modality" value={value} checked={modality === value} onChange={() => setModality(value)} className="sr-only" />
              {label}
            </label>
          ))}
        </div>
      </fieldset>
      {modality === "IN_PERSON" && (
        <SelectField label="Centro" value={facilityId} onChange={(e) => setFacilityId(e.target.value)}>
          {doctor.facilities.map((f) => (
            <option key={f.publicId} value={f.publicId}>
              {f.name}, {f.city}
            </option>
          ))}
        </SelectField>
      )}
      <TextAreaField label="Motivo de la consulta (opcional)" maxLength={300} value={reason} onChange={(e) => setReason(e.target.value)} />
      <Button size="lg" disabled={busy} onClick={() => onConfirm({ modality, facilityId: modality === "IN_PERSON" ? facilityId : undefined, reason })}>
        {busy ? "Reservando…" : "Confirmar cita"}
      </Button>
    </div>
  );
}

function Summary({ specialty, doctor, slot }: { specialty?: string; doctor?: Doctor; slot?: Slot }) {
  const rows = [
    ["Especialidad", specialty ?? doctor?.specialty.name],
    ["Profesional", doctor && fullName(doctor)],
    ["Fecha", slot && formatDay(slot.startsAt)],
    ["Hora", slot && `${formatTime(slot.startsAt)} h`],
  ];
  return (
    <aside aria-label="Resumen de la cita" className="card h-fit p-6 lg:sticky lg:top-24">
      <h2 className="text-lg font-extrabold">Tu cita</h2>
      <dl className="mt-3 divide-y divide-line">
        {rows.map(([term, value]) => (
          <div key={term} className="py-2.5">
            <dt className="text-sm text-ink-soft">{term}</dt>
            <dd className={`mt-0.5 first-letter:uppercase ${value ? "font-medium" : "text-ink-soft"}`}>{value ?? "—"}</dd>
          </div>
        ))}
      </dl>
    </aside>
  );
}

export function BookingPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const preselected = params.get("profesional");
  const preselectedSlot = params.get("hora");

  const [step, setStep] = useState(0);
  const [specialty, setSpecialty] = useState<Specialty>();
  const [doctor, setDoctor] = useState<Doctor>();
  const [slot, setSlot] = useState<Slot>();
  const [notice, setNotice] = useState<string | null>(null);

  const book = useBookAppointment();
  const heading = useRef<HTMLHeadingElement>(null);

  // Viene de la agenda del cuadro médico: se salta los pasos ya resueltos.
  const all = useDoctors({ size: 100 });
  useEffect(() => {
    if (!preselected || doctor) return;
    const found = all.data?.content.find((d) => d.publicId === preselected);
    if (found) {
      setDoctor(found);
      if (preselectedSlot) setSlot({ startsAt: preselectedSlot, durationMin: 20 });
      setStep(preselectedSlot ? 3 : 2);
    }
  }, [preselected, preselectedSlot, doctor, all.data]);

  // Cada paso cambia la pantalla entera: el foco acompaña al usuario.
  useEffect(() => {
    if (step > 0) heading.current?.focus();
  }, [step]);

  const titles = ["¿Qué especialidad necesitas?", "Elige profesional", "Elige día y hora", "Revisa y confirma"];

  function confirm(values: Confirmation) {
    if (!doctor || !slot) return;
    book.mutate(
      { doctorId: doctor.publicId, startsAt: slot.startsAt, ...values },
      {
        onSuccess: () => {
          toast.success("Cita reservada. Te hemos enviado la confirmación por email.");
          navigate("/panel");
        },
        onError: (error) => {
          if (hasCode(error, "APPOINTMENT_SLOT_TAKEN")) {
            // La verdad la tiene la API: se vuelve al calendario con la disponibilidad al día.
            void queryClient.invalidateQueries({ queryKey: ["slots"] });
            setSlot(undefined);
            setNotice(errorMessage(error));
            setStep(2);
          }
        },
      },
    );
  }

  return (
    <section className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_21rem]">
      <div className="card min-w-0 overflow-hidden">
        <div className="bg-brand p-6 text-white md:p-8">
          <p className="mb-5 text-2xl font-extrabold">Pide tu cita</p>
          <Progress step={step} />
        </div>

        <div className="p-6 md:p-8">
        <div className="flex items-center gap-2">
          {step > 0 && (
            <Button variant="ghost" size="sm" aria-label="Atrás" className="-ml-2 px-2.5" onClick={() => setStep(step - 1)}>
              <ArrowLeft className="size-4.5" aria-hidden />
            </Button>
          )}
          <h1 ref={heading} tabIndex={-1} className="text-2xl font-extrabold outline-none">
            {titles[step]}
          </h1>
        </div>

        <div className="mt-5" key={step}>
          {step === 0 && (
            <SpecialtyStep
              onPick={(s) => {
                setSpecialty(s);
                setDoctor(undefined);
                setStep(1);
              }}
            />
          )}
          {step === 1 && specialty && (
            <DoctorStep
              specialty={specialty.slug}
              onPick={(d) => {
                setDoctor(d);
                setNotice(null);
                setStep(2);
              }}
            />
          )}
          {step === 2 && doctor && (
            <SlotStep
              doctor={doctor}
              notice={notice}
              onPick={(s) => {
                setSlot(s);
                setStep(3);
              }}
            />
          )}
          {step === 3 && doctor && slot && (
            <ConfirmStep doctor={doctor} busy={book.isPending} error={book.isError && !hasCode(book.error, "APPOINTMENT_SLOT_TAKEN") ? errorMessage(book.error) : null} onConfirm={confirm} />
          )}
        </div>
        </div>
      </div>

      <Summary specialty={specialty?.name} doctor={doctor} slot={slot} />
    </section>
  );
}
