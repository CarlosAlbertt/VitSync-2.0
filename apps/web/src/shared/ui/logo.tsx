/** `onDark`: sobre una banda de color, el símbolo va en una pastilla blanca para que se distinga. */
export function Logo({ onDark = false }: { onDark?: boolean }) {
  return (
    <span className="flex items-center gap-2">
      <img src="/logo.png" alt="" className={onDark ? "h-9 w-auto rounded-lg bg-white p-1" : "h-9 w-auto"} />
      <span className={`text-xl font-extrabold tracking-tight ${onDark ? "text-white" : "text-accent-strong"}`}>VitSync</span>
    </span>
  );
}
