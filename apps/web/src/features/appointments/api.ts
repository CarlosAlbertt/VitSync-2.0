import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { request } from "@/shared/api/client";
import type { Appointment, Modality, Page } from "@/shared/api/types";

export type BookAppointment = {
  doctorId: string;
  startsAt: string;
  modality: Modality;
  facilityId?: string;
  reason?: string;
};

export function useMyAppointments(when: "upcoming" | "past") {
  return useQuery({
    queryKey: ["appointments", "me", when],
    queryFn: () => request<Page<Appointment>>("/appointments/me", { query: { when } }),
  });
}

export function useBookAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: BookAppointment) => request<Appointment>("/appointments", { method: "POST", body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["appointments"] }),
  });
}

export function useCancelAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => request(`/appointments/${id}/cancel`, { method: "POST" }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["appointments"] });
      void queryClient.invalidateQueries({ queryKey: ["slots"] });
    },
  });
}
