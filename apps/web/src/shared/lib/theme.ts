import { create } from "zustand";

type Theme = "light" | "dark";
/** Lo que elige el usuario. "system" sigue al sistema operativo y no guarda nada. */
export type ThemePreference = Theme | "system";

// Misma clave que el script de index.html, que aplica el tema antes de pintar.
const KEY = "vitsync-theme";

const systemQuery = () => (typeof matchMedia === "function" ? matchMedia("(prefers-color-scheme: dark)") : null);

function storedPreference(): ThemePreference {
  try {
    const saved = localStorage.getItem(KEY);
    return saved === "light" || saved === "dark" ? saved : "system";
  } catch {
    return "system";
  }
}

function resolve(preference: ThemePreference): Theme {
  if (preference !== "system") return preference;
  return systemQuery()?.matches ? "dark" : "light";
}

function save(preference: ThemePreference) {
  try {
    if (preference === "system") localStorage.removeItem(KEY);
    else localStorage.setItem(KEY, preference);
  } catch {
    // Sin almacenamiento el tema dura lo que la pestaña; no es un error para el usuario.
  }
}

type State = {
  preference: ThemePreference;
  theme: Theme;
  setPreference: (preference: ThemePreference) => void;
  /** Alterna claro y oscuro (botón de la web pública). Deja de seguir al sistema. */
  toggle: () => void;
};

export const useTheme = create<State>((set, get) => {
  const preference = storedPreference();
  return {
    preference,
    theme: resolve(preference),
    setPreference: (next) => {
      const theme = resolve(next);
      document.documentElement.dataset.theme = theme;
      save(next);
      set({ preference: next, theme });
    },
    toggle: () => get().setPreference(get().theme === "dark" ? "light" : "dark"),
  };
});

// Con "sistema", si el usuario cambia el modo del sistema operativo, la página le sigue al momento.
systemQuery()?.addEventListener("change", () => {
  if (useTheme.getState().preference === "system") useTheme.getState().setPreference("system");
});
