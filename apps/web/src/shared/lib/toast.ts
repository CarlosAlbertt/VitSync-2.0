import { create } from "zustand";

export type Toast = { id: number; tone: "success" | "error" | "info"; text: string };

type State = {
  toasts: Toast[];
  push: (tone: Toast["tone"], text: string) => void;
  dismiss: (id: number) => void;
};

let nextId = 1;

export const useToasts = create<State>((set, get) => ({
  toasts: [],
  push: (tone, text) => {
    const id = nextId++;
    set({ toasts: [...get().toasts, { id, tone, text }] });
    setTimeout(() => get().dismiss(id), 5000);
  },
  dismiss: (id) => set({ toasts: get().toasts.filter((t) => t.id !== id) }),
}));

export const toast = {
  success: (text: string) => useToasts.getState().push("success", text),
  error: (text: string) => useToasts.getState().push("error", text),
  info: (text: string) => useToasts.getState().push("info", text),
};
