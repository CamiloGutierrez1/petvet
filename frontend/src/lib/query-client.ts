import { QueryClient } from "@tanstack/react-query"

// Configuración común de TanStack Query para toda la app.
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000, // 30 s antes de volver a pedir los mismos datos
      retry: 1, // un reintento ante fallos de red
      refetchOnWindowFocus: false,
    },
  },
})
