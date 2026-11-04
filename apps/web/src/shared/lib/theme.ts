import { create } from "zustand";

type Theme = "light" | "dark";

const current = (): Theme => (document.documentElement.dataset.theme === "dark" ? "dark" : "light");

export const useTheme = create<{ theme: Theme; toggle: () => void }>((set, get) => ({
  theme: current(),
  toggle: () => {
    const theme = get().theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = theme;
    try {
      localStorage.setItem("vitsync-theme", theme);
    } catch {
      // Sin almacenamiento el tema dura lo que la pestaña; no es un error para el usuario.
    }
    set({ theme });
  },
}));
