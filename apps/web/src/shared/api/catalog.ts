import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { request } from "./client";
import type { Doctor, Facility, Page, Slot, Specialty } from "./types";

// El catálogo lo usan el cuadro médico y la reserva, por eso vive en shared.
//
// Listas con staleTime 0 (VIT-80): al volver a una pantalla ya visitada se pinta al instante desde
// la caché (gcTime, 5 minutos por defecto) y se revalida en segundo plano. Son peticiones pequeñas
// y paginadas; a cambio, un médico o un centro nuevo aparece en la siguiente visita.

export function useSpecialties(params: { q?: string; kind?: string; page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: ["specialties", params],
    queryFn: () => request<Page<Specialty>>("/specialties", { query: params }),
    placeholderData: keepPreviousData,
    staleTime: 0,
  });
}

export function useFacilities() {
  return useQuery({
    queryKey: ["facilities"],
    queryFn: () => request<Page<Facility>>("/facilities", { query: { size: 50 } }),
    staleTime: 0,
  });
}

export function useDoctors(params: { specialty?: string; facility?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: ["doctors", params],
    queryFn: () => request<Page<Doctor>>("/doctors", { query: params }),
    placeholderData: keepPreviousData,
    staleTime: 0,
  });
}

export function useSlots(doctorId: string | undefined, date: string | undefined) {
  return useQuery({
    queryKey: ["slots", doctorId, date],
    queryFn: () => request<Slot[]>(`/doctors/${doctorId}/slots`, { query: { date } }),
    enabled: Boolean(doctorId && date),
    staleTime: 0, // la disponibilidad cambia: siempre se revalida
  });
}
