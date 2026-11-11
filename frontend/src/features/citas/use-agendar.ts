import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { agendarCita, getDisponibilidad } from "@/api/citas"

/**
 * Horas libres de un veterinario en un día ("2026-11-20"). Solo consulta
 * cuando ya se eligieron los dos.
 */
export function useDisponibilidad(veterinarioId?: number, fecha?: string) {
  return useQuery({
    queryKey: ["disponibilidad", veterinarioId, fecha],
    queryFn: () => getDisponibilidad(veterinarioId!, fecha!),
    enabled: veterinarioId !== undefined && fecha !== undefined,
    staleTime: 0, // las horas libres cambian cuando otros agendan
  })
}

/** Agenda una cita y refresca las citas y la disponibilidad. */
export function useAgendarCita() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: agendarCita,
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["citas"] })
      queryClient.invalidateQueries({ queryKey: ["disponibilidad"] })
    },
  })
}
