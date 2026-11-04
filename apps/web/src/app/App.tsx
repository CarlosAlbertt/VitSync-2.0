import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { RouterProvider } from "react-router";
import { refreshSession } from "@/shared/api/client";
import { ApiError } from "@/shared/api/client";
import { getTextSize, setTextSize } from "@/shared/lib/text-size";
import { Toaster } from "@/shared/ui/toaster";
import { router } from "./router";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Reintentar un 4xx no lo arregla.
      retry: (count, error) => count < 2 && !(error instanceof ApiError && error.status < 500),
      refetchOnWindowFocus: false,
    },
  },
});

export function App() {
  useEffect(() => {
    // El access token no sobrevive a una recarga: se recupera con la cookie de refresh.
    void refreshSession();
    setTextSize(getTextSize());
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      <Toaster />
    </QueryClientProvider>
  );
}
