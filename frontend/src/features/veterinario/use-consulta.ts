import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { getCita, registrarConsulta } from "@/api/citas"
import type { RegistrarConsulta } from "@/api/types"

/** Una cita por su id (GET /api/citas/{id}). */
export function useCita(citaId: number) {
  return useQuery({
    // Empieza por "citas" para que registrar o cancelar la refresque.
    queryKey: ["citas", "detalle", citaId],
    queryFn: () => getCita(citaId),
  })
}

/** Registra la consulta (la cita queda COMPLETADA) y refresca lo afectado. */
export function useRegistrarConsulta(mascotaId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    meta: { errorEnPantalla: true }, // el formulario muestra el error
    mutationFn: ({
      citaId,
      datos,
    }: {
      citaId: number
      datos: RegistrarConsulta
    }) => registrarConsulta(citaId, datos),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["citas"] })
      queryClient.invalidateQueries({ queryKey: ["historial", mascotaId] })
      queryClient.invalidateQueries({ queryKey: ["vacunas", mascotaId] })
    },
  })
}
