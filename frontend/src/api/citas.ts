// Funciones del microservicio ms-citas-historial (puerto 8082).
// Cada función llama a la API real o, si VITE_USE_MOCKS=true, responde con
// los datos de src/mocks. Las páginas no saben cuál de los dos se usa.

import citasMock from "@/mocks/citas.json"
import historialMock from "@/mocks/historial.json"
import proximasVacunasMock from "@/mocks/proximas-vacunas.json"
import veterinariosMock from "@/mocks/veterinarios.json"
import { citasApi, simularRespuesta, USE_MOCKS } from "./http"
import type {
  Cita,
  Disponibilidad,
  NuevaCita,
  ProximaVacuna,
  RegistrarConsulta,
  RegistroHistorial,
  Veterinario,
} from "./types"

// En los mocks, historial y vacunas llevan mascotaId solo para poder filtrar.
type ConMascota<T> = T & { mascotaId: number }

// Copia en memoria para que, en modo mocks, lo que se registra aparezca en
// las listas hasta recargar la página.
const veterinarios: Veterinario[] = [...veterinariosMock]
const citas: Cita[] = [...(citasMock as Cita[])]
const historial: ConMascota<RegistroHistorial>[] = [...historialMock]
const proximasVacunas: ConMascota<ProximaVacuna>[] = [...proximasVacunasMock]

/** Horario de atención simulado: cada 30 minutos de 8:00 a 16:30. */
const HORAS_ATENCION = Array.from({ length: 18 }, (_, i) => {
  const minutos = 8 * 60 + i * 30
  const hh = String(Math.floor(minutos / 60)).padStart(2, "0")
  const mm = String(minutos % 60).padStart(2, "0")
  return `${hh}:${mm}:00`
})

function buscarCita(id: number): Cita {
  const cita = citas.find((c) => c.id === id)
  if (!cita) throw new Error(`No existe la cita ${id}`)
  return cita
}

// ---------- Veterinarios ----------

export async function getVeterinarios(): Promise<Veterinario[]> {
  if (USE_MOCKS) return simularRespuesta(veterinarios)
  const { data } = await citasApi.get<Veterinario[]>("/veterinarios")
  return data
}

/** fecha en formato "2026-11-20". */
export async function getDisponibilidad(
  veterinarioId: number,
  fecha: string,
): Promise<Disponibilidad> {
  if (USE_MOCKS) {
    const ocupadas = citas
      .filter(
        (c) =>
          c.veterinarioId === veterinarioId &&
          c.estado === "AGENDADA" &&
          c.fechaHora.startsWith(fecha),
      )
      .map((c) => c.fechaHora)
    const libres = HORAS_ATENCION.map((h) => `${fecha}T${h}`).filter(
      (fechaHora) => !ocupadas.includes(fechaHora),
    )
    return simularRespuesta(libres)
  }
  const { data } = await citasApi.get<Disponibilidad>(
    `/veterinarios/${veterinarioId}/disponibilidad`,
    { params: { fecha } },
  )
  return data
}

// ---------- Citas ----------

export async function agendarCita(datos: NuevaCita): Promise<Cita> {
  if (USE_MOCKS) {
    const cita: Cita = {
      id: Math.max(0, ...citas.map((c) => c.id)) + 1,
      estado: "AGENDADA",
      ...datos,
    }
    citas.push(cita)
    return simularRespuesta(cita)
  }
  const { data } = await citasApi.post<Cita>("/citas", datos)
  return data
}

export async function getCitasDeMascota(mascotaId: number): Promise<Cita[]> {
  if (USE_MOCKS) {
    return simularRespuesta(citas.filter((c) => c.mascotaId === mascotaId))
  }
  const { data } = await citasApi.get<Cita[]>("/citas", {
    params: { mascotaId },
  })
  return data
}

/** Agenda del día de un veterinario. fecha en formato "2026-11-20". */
export async function getAgendaVeterinario(
  veterinarioId: number,
  fecha: string,
): Promise<Cita[]> {
  if (USE_MOCKS) {
    return simularRespuesta(
      citas.filter(
        (c) =>
          c.veterinarioId === veterinarioId && c.fechaHora.startsWith(fecha),
      ),
    )
  }
  const { data } = await citasApi.get<Cita[]>("/citas", {
    params: { veterinarioId, fecha },
  })
  return data
}

export async function cancelarCita(id: number): Promise<Cita> {
  if (USE_MOCKS) {
    const cita = buscarCita(id)
    cita.estado = "CANCELADA"
    return simularRespuesta(cita)
  }
  const { data } = await citasApi.patch<Cita>(`/citas/${id}/cancelar`)
  return data
}

/** Registra la consulta y deja la cita en estado COMPLETADA. */
export async function registrarConsulta(
  citaId: number,
  datos: RegistrarConsulta,
): Promise<RegistroHistorial> {
  if (USE_MOCKS) {
    const cita = buscarCita(citaId)
    cita.estado = "COMPLETADA"
    const registro = {
      id: Math.max(0, ...historial.map((h) => h.id)) + 1,
      citaId,
      mascotaId: cita.mascotaId,
      veterinarioId: cita.veterinarioId,
      fecha: cita.fechaHora,
      ...datos,
    }
    historial.push(registro)
    return simularRespuesta(registro)
  }
  const { data } = await citasApi.post<RegistroHistorial>(
    `/citas/${citaId}/consulta`,
    datos,
  )
  return data
}

// ---------- Historial y vacunas ----------

export async function getHistorial(
  mascotaId: number,
): Promise<RegistroHistorial[]> {
  if (USE_MOCKS) {
    return simularRespuesta(historial.filter((h) => h.mascotaId === mascotaId))
  }
  const { data } = await citasApi.get<RegistroHistorial[]>(
    `/mascotas/${mascotaId}/historial`,
  )
  return data
}

export async function getProximasVacunas(
  mascotaId: number,
): Promise<ProximaVacuna[]> {
  if (USE_MOCKS) {
    return simularRespuesta(
      proximasVacunas
        .filter((v) => v.mascotaId === mascotaId)
        .map(({ vacuna, fechaLimite }) => ({ vacuna, fechaLimite })),
    )
  }
  const { data } = await citasApi.get<ProximaVacuna[]>(
    `/mascotas/${mascotaId}/proximas-vacunas`,
  )
  return data
}
