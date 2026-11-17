import { useQueries, useQuery } from "@tanstack/react-query"
import { getAgendaVeterinario } from "@/api/citas"
import { getMascota } from "@/api/mascotas"
import type { MascotaDetalle } from "@/api/types"

/** Citas de un veterinario en un día ("2026-11-20"), en orden de hora. */
export function useAgenda(veterinarioId: number, fecha: string) {
  return useQuery({
    // Empieza por "citas" para que cancelar o registrar consulta la refresque.
    queryKey: ["citas", "agenda", veterinarioId, fecha],
    queryFn: () => getAgendaVeterinario(veterinarioId, fecha),
  })
}

/**
 * Fichas (con datos del dueño) de varias mascotas, desde el servicio de
 * Mascotas. Si alguna falla, las demás se muestran igual: la agenda no
 * depende de que Mascotas responda.
 */
export function useFichas(mascotaIds: number[]) {
  const ids = [...new Set(mascotaIds)]
  return useQueries({
    queries: ids.map((id) => ({
      queryKey: ["mascota", id],
      queryFn: () => getMascota(id),
    })),
    combine: (resultados) => {
      const porId = new Map<number, MascotaDetalle>()
      resultados.forEach((r, i) => {
        if (r.data) porId.set(ids[i]!, r.data)
      })
      return { porId, cargando: resultados.some((r) => r.isLoading) }
    },
  })
}
