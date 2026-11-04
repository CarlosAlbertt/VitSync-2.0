import { CalendarClock, MapPin } from "lucide-react";
import { useMemo } from "react";
import { Link } from "react-router";
import { useSlots } from "@/shared/api/catalog";
import type { Doctor } from "@/shared/api/types";
import { formatShortDay, formatTime, fullName, nextWorkdays, toDateKey } from "@/shared/lib/format";
import { Avatar } from "./avatar";
import { ButtonLink } from "./button";
import { Skeleton } from "./feedback";
import { SpecialtyArt } from "./specialty-art";

function NextSlot({ doctor }: { doctor: Doctor }) {
  const day = useMemo(() => toDateKey(nextWorkdays(1)[0]!), []);
  const { data, isPending } = useSlots(doctor.publicId, day);
  if (isPending) return <Skeleton className="h-11" />;

  const slot = data?.[0];
  if (!slot) return <p className="flex h-11 items-center text-sm text-ink-soft">Consulta su agenda para ver otros días</p>;

  return (
    <Link
      to={`/reservar?profesional=${doctor.publicId}&hora=${encodeURIComponent(slot.startsAt)}`}
      aria-label={`Reservar con ${fullName(doctor)}, ${formatShortDay(slot.startsAt)} a las ${formatTime(slot.startsAt)}`}
      className="flex h-11 items-center gap-2 rounded-xl bg-accent-wash px-3.5 text-sm font-semibold text-accent-strong transition-colors hover:bg-accent hover:text-accent-ink"
    >
      <CalendarClock className="size-4.5 shrink-0" aria-hidden />
      <span className="first-letter:uppercase">
        {formatShortDay(slot.startsAt)}, {formatTime(slot.startsAt)} h
      </span>
    </Link>
  );
}

export function DoctorCard({ doctor }: { doctor: Doctor }) {
  return (
    <article className="card flex h-full flex-col overflow-hidden">
      <div className="relative h-28 bg-white">
        <SpecialtyArt slug={doctor.specialty.slug} className="absolute top-0 right-0 size-28" />
        <div className="absolute -bottom-8 left-6 rounded-full bg-raised p-1">
          <Avatar person={doctor} className="size-16 text-xl" />
        </div>
      </div>
      <div className="flex flex-1 flex-col gap-3 p-6 pt-11">
        <div>
          <h3 className="text-lg font-extrabold">{fullName(doctor)}</h3>
          <p className="font-semibold text-accent">{doctor.specialty.name}</p>
        </div>
        <p className="line-clamp-2 text-[0.95rem] text-ink-soft">{doctor.bio}</p>
        <p className="flex items-start gap-1.5 text-sm text-ink-soft">
          <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden />
          {doctor.facilities.map((f) => `${f.name}, ${f.city}`).join(" · ")}
        </p>
        <div className="mt-auto grid gap-2 pt-2">
          <p className="text-xs font-semibold text-ink-soft">Primer hueco</p>
          <NextSlot doctor={doctor} />
          <ButtonLink to={`/reservar?profesional=${doctor.publicId}`} variant="soft">
            Ver agenda
          </ButtonLink>
        </div>
      </div>
    </article>
  );
}
