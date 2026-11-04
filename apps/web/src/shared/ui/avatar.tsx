import { initials } from "@/shared/lib/format";

type Person = { firstName: string; lastName: string };

export function Avatar({ person, className = "size-12 text-base" }: { person: Person; className?: string }) {
  return (
    <span aria-hidden className={`grid shrink-0 place-items-center rounded-full bg-accent-wash font-bold text-accent-strong ${className}`}>
      {initials(person.firstName, person.lastName)}
    </span>
  );
}
