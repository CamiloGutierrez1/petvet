import { useQuery } from "@tanstack/react-query"
import { getCitasDeVeterinario } from "@/api/citas"
import type { Cita } from "@/api/types"

/** Todas las citas de un veterinario (GET /api/citas?veterinarioId=). */
export function useCitasDeVeterinario(veterinarioId: number) {
  return useQuery({
    queryKey: ["citas", "veterinario", veterinarioId],
    queryFn: () => getCitasDeVeterinario(veterinarioId),
  })
}

export interface ResumenPaciente {
  mascotaId: number
  totalCitas: number
  /** Fecha y hora de la última cita completada (la última consulta). */
  ultimaConsulta?: string
  /** Fecha y hora de la próxima cita agendada que aún no pasa. */
  proximaCita?: string
}

/**
 * Agrupa las citas por mascota: cada mascota distinta es un paciente del
 * veterinario. `ahora` en formato ISO local ("2026-11-13T10:00:00").
 */
export function resumirPacientes(
  citas: Cita[],
  ahora: string,
): ResumenPaciente[] {
  const porMascota = new Map<number, ResumenPaciente>()
  for (const cita of citas) {
    const resumen = porMascota.get(cita.mascotaId) ?? {
      mascotaId: cita.mascotaId,
      totalCitas: 0,
    }
    resumen.totalCitas += 1
    if (
      cita.estado === "COMPLETADA" &&
      (!resumen.ultimaConsulta || cita.fechaHora > resumen.ultimaConsulta)
    ) {
      resumen.ultimaConsulta = cita.fechaHora
    }
    if (
      cita.estado === "AGENDADA" &&
      cita.fechaHora >= ahora &&
      (!resumen.proximaCita || cita.fechaHora < resumen.proximaCita)
    ) {
      resumen.proximaCita = cita.fechaHora
    }
    porMascota.set(cita.mascotaId, resumen)
  }
  // Primero los que tienen próxima cita (la más cercana arriba); luego el resto.
  return [...porMascota.values()].sort((a, b) => {
    if (a.proximaCita && b.proximaCita)
      return a.proximaCita.localeCompare(b.proximaCita)
    if (a.proximaCita) return -1
    if (b.proximaCita) return 1
    return (b.ultimaConsulta ?? "").localeCompare(a.ultimaConsulta ?? "")
  })
}
