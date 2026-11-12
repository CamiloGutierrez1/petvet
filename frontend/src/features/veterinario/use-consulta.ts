import { useMutation, useQueryClient } from "@tanstack/react-query"
import { registrarConsulta } from "@/api/citas"
import type { RegistrarConsulta } from "@/api/types"
import { useCitasDeMascota } from "@/features/duenos-mascotas/use-ficha-mascota"

/**
 * Una cita por su id. El contrato no tiene GET /api/citas/{id} (pedido a
 * Jersson), así que se busca entre las citas de su mascota.
 */
export function useCita(mascotaId: number, citaId: number) {
  const consulta = useCitasDeMascota(mascotaId)
  return {
    ...consulta,
    cita: consulta.data?.find((c) => c.id === citaId),
  }
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
