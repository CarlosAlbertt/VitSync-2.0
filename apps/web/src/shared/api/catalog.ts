import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { request } from "./client";
import type { Doctor, Facility, Page, Slot, Specialty } from "./types";

// El catálogo lo usan el cuadro médico y la reserva, por eso vive en shared.

export function useSpecialties(params: { q?: string; kind?: string; page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: ["specialties", params],
    queryFn: () => request<Page<Specialty>>("/specialties", { query: params }),
    placeholderData: keepPreviousData,
    staleTime: 5 * 60_000,
  });
}

export function useFacilities() {
  return useQuery({
    queryKey: ["facilities"],
    queryFn: () => request<Page<Facility>>("/facilities", { query: { size: 50 } }),
    staleTime: 5 * 60_000,
  });
}

export function useDoctors(params: { specialty?: string; facility?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: ["doctors", params],
    queryFn: () => request<Page<Doctor>>("/doctors", { query: params }),
    placeholderData: keepPreviousData,
    staleTime: 60_000,
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
