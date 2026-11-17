import { useQuery } from "@tanstack/react-query"
import { getCitasDeMascota, getProximasVacunas } from "@/api/citas"
import { getDueno, getMascota } from "@/api/mascotas"

// Consultas de la ficha de una mascota. Cada una va a un microservicio
// distinto: los datos a Mascotas (8081) y las vacunas y citas a Citas (8082).
// Si uno falla, las otras secciones siguen mostrándose.

export function useMascota(mascotaId: number) {
  return useQuery({
    queryKey: ["mascota", mascotaId],
    queryFn: () => getMascota(mascotaId),
  })
}

/** Datos de contacto del dueño (documento, teléfono y dirección). */
export function useDueno(duenoId: number | undefined) {
  return useQuery({
    queryKey: ["dueno", duenoId],
    queryFn: () => getDueno(duenoId!),
    enabled: duenoId !== undefined,
  })
}

export function useProximasVacunas(mascotaId: number) {
  return useQuery({
    queryKey: ["vacunas", mascotaId],
    queryFn: () => getProximasVacunas(mascotaId),
  })
}

export function useCitasDeMascota(mascotaId: number) {
  return useQuery({
    queryKey: ["citas", "mascota", mascotaId],
    queryFn: () => getCitasDeMascota(mascotaId),
  })
}
