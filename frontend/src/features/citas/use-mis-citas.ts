import { useMutation, useQueries, useQueryClient } from "@tanstack/react-query"
import { cancelarCita, getCitasDeMascota } from "@/api/citas"
import type { Cita, Mascota } from "@/api/types"

/**
 * Citas de todas las mascotas del dueño. El back entrega las citas por
 * mascota (GET /api/citas?mascotaId=), así que se consultan en paralelo y se
 * juntan. Comparte caché con la ficha de cada mascota.
 */
export function useCitasDeMascotas(mascotas: Mascota[] | undefined) {
  return useQueries({
    queries: (mascotas ?? []).map((m) => ({
      queryKey: ["citas", "mascota", m.id],
      queryFn: () => getCitasDeMascota(m.id),
    })),
    combine: (resultados) => ({
      citas: resultados.flatMap((r) => r.data ?? []) as Cita[],
      isLoading: resultados.some((r) => r.isLoading),
      error: resultados.find((r) => r.error)?.error ?? null,
      refetch: () => resultados.forEach((r) => r.refetch()),
    }),
  })
}

/** Cancela una cita y refresca las citas y las horas libres. */
export function useCancelarCita() {
  const queryClient = useQueryClient()
  return useMutation({
    meta: { errorEnPantalla: true }, // la vista muestra el error
    mutationFn: cancelarCita,
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["citas"] })
      queryClient.invalidateQueries({ queryKey: ["disponibilidad"] })
    },
  })
}
