import { Moon, Sun } from "lucide-react";
import { useTheme } from "@/shared/lib/theme";

export function ThemeToggle() {
  const { theme, toggle } = useTheme();
  const dark = theme === "dark";
  const Icon = dark ? Sun : Moon;
  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={dark ? "Cambiar a modo claro" : "Cambiar a modo oscuro"}
      className="grid size-10 cursor-pointer place-items-center rounded-xl text-ink-soft transition-colors hover:bg-subtle hover:text-ink"
    >
      <Icon className="size-4.5" aria-hidden />
    </button>
  );
}
