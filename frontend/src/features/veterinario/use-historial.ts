import { useQuery } from "@tanstack/react-query"
import { getHistorial } from "@/api/citas"

/** Historial clínico de una mascota, la consulta más reciente primero. */
export function useHistorial(mascotaId: number) {
  return useQuery({
    // Misma clave que invalida useRegistrarConsulta.
    queryKey: ["historial", mascotaId],
    queryFn: () => getHistorial(mascotaId),
  })
}
