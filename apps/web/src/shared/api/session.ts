import { create } from "zustand";
import type { Me, Session } from "./types";

type State = {
  // "checking": al cargar, aún no sabemos si la cookie de refresh sigue viva.
  status: "checking" | "anonymous" | "authenticated";
  accessToken: string | null;
  user: Me | null;
  signIn: (session: Session) => void;
  signOut: () => void;
  setUser: (user: Me) => void;
};

// El access token vive solo en memoria (ADR-0004): nunca en localStorage ni sessionStorage.
export const useSession = create<State>((set) => ({
  status: "checking",
  accessToken: null,
  user: null,
  signIn: ({ accessToken, user }) => set({ status: "authenticated", accessToken, user }),
  signOut: () => set({ status: "anonymous", accessToken: null, user: null }),
  setUser: (user) => set({ user }),
}));
