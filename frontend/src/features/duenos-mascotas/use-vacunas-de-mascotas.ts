import { useQueries } from "@tanstack/react-query"
import { getProximasVacunas } from "@/api/citas"
import type { Mascota, ProximaVacuna } from "@/api/types"

export interface VacunaDeMascota extends ProximaVacuna {
  mascota: Mascota
}

/**
 * Próximas vacunas de todas las mascotas del dueño, de la más urgente a la
 * menos urgente. Se consultan en paralelo y comparten caché con la ficha.
 */
export function useVacunasDeMascotas(mascotas: Mascota[] | undefined) {
  return useQueries({
    queries: (mascotas ?? []).map((m) => ({
      queryKey: ["vacunas", m.id],
      queryFn: () => getProximasVacunas(m.id),
    })),
    combine: (resultados) => ({
      vacunas: resultados
        .flatMap((r, i) =>
          (r.data ?? []).map((v) => ({ ...v, mascota: mascotas![i]! })),
        )
        .sort((a, b) =>
          a.fechaLimite.localeCompare(b.fechaLimite),
        ) as VacunaDeMascota[],
      isLoading: resultados.some((r) => r.isLoading),
      error: resultados.find((r) => r.error)?.error ?? null,
      refetch: () => resultados.forEach((r) => r.refetch()),
    }),
  })
}
