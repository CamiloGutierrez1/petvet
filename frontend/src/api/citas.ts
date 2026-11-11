// Funciones del microservicio ms-citas-historial (puerto 8082).
// Cada función llama a la API real o, si VITE_USE_MOCKS=true, responde con
// los datos de src/mocks. Las páginas no saben cuál de los dos se usa.
// Los mocks imitan las reglas del servicio real (ver docs/api.md y
// docs/decisiones.md): turnos de 30 min de 8:00 a 18:00, sin horas pasadas,
// 409 si el turno está ocupado y 404 si el recurso no existe.

import citasMock from "@/mocks/citas.json"
import historialMock from "@/mocks/historial.json"
import proximasVacunasMock from "@/mocks/proximas-vacunas.json"
import veterinariosMock from "@/mocks/veterinarios.json"
import {
  ahoraIsoLocal,
  citasApi,
  errorSimulado,
  simularRespuesta,
  USE_MOCKS,
} from "./http"
import type {
  Cita,
  Disponibilidad,
  NuevaCita,
  ProximaVacuna,
  RegistrarConsulta,
  RegistroHistorial,
  Veterinario,
} from "./types"

// Las próximas vacunas llevan mascotaId en el mock solo para poder filtrar.
type VacunaConMascota = ProximaVacuna & { mascotaId: number }

// Copia en memoria para que, en modo mocks, lo que se registra aparezca en
// las listas hasta recargar la página.
const veterinarios: Veterinario[] = [...veterinariosMock]
const citas: Cita[] = [...(citasMock as Cita[])]
const historial: RegistroHistorial[] = [
  ...(historialMock as RegistroHistorial[]),
]
const proximasVacunas: VacunaConMascota[] = [...proximasVacunasMock]

const DURACION_MINUTOS = 30
const APERTURA_MIN = 8 * 60 // 8:00
const CIERRE_MIN = 18 * 60 // 18:00

/** Turnos del día: "08:00:00", "08:30:00", ..., "17:30:00". */
const TURNOS = Array.from(
  { length: (CIERRE_MIN - APERTURA_MIN) / DURACION_MINUTOS },
  (_, i) => {
    const minutos = APERTURA_MIN + i * DURACION_MINUTOS
    const hh = String(Math.floor(minutos / 60)).padStart(2, "0")
    const mm = String(minutos % 60).padStart(2, "0")
    return `${hh}:${mm}:00`
  },
)

function buscarCita(id: number): Cita {
  const cita = citas.find((c) => c.id === id)
  if (!cita)
    throw errorSimulado(404, "RECURSO_NO_ENCONTRADO", "La cita no existe")
  return cita
}

function buscarVeterinario(id: number): Veterinario {
  const veterinario = veterinarios.find((v) => v.id === id)
  if (!veterinario) {
    throw errorSimulado(
      404,
      "RECURSO_NO_ENCONTRADO",
      "El veterinario no existe",
    )
  }
  return veterinario
}

/** Turnos libres de un veterinario en una fecha, con las mismas reglas del back. */
function turnosLibres(veterinarioId: number, fecha: string): string[] {
  const ahora = ahoraIsoLocal()
  const ocupadas = citas
    .filter(
      (c) =>
        c.veterinarioId === veterinarioId &&
        c.estado !== "CANCELADA" &&
        c.fechaHora.startsWith(fecha),
    )
    .map((c) => c.fechaHora)
  return TURNOS.map((t) => `${fecha}T${t}`).filter(
    (fechaHora) => fechaHora > ahora && !ocupadas.includes(fechaHora),
  )
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
    buscarVeterinario(veterinarioId)
    return simularRespuesta({
      veterinarioId,
      fecha,
      horarios: turnosLibres(veterinarioId, fecha),
    })
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
    buscarVeterinario(datos.veterinarioId)
    const fecha = datos.fechaHora.slice(0, 10)
    if (!turnosLibres(datos.veterinarioId, fecha).includes(datos.fechaHora)) {
      throw errorSimulado(
        409,
        "CONFLICTO",
        "El veterinario ya tiene una cita en ese horario",
      )
    }
    const mascotaOcupada = citas.some(
      (c) =>
        c.mascotaId === datos.mascotaId &&
        c.estado !== "CANCELADA" &&
        c.fechaHora === datos.fechaHora,
    )
    if (mascotaOcupada) {
      throw errorSimulado(
        409,
        "CONFLICTO",
        "La mascota ya tiene una cita en ese horario",
      )
    }
    const cita: Cita = {
      id: Math.max(0, ...citas.map((c) => c.id)) + 1,
      estado: "AGENDADA",
      duracionMinutos: DURACION_MINUTOS,
      ...datos,
    }
    citas.push(cita)
    return simularRespuesta(cita)
  }
  const { data } = await citasApi.post<Cita>("/citas", datos)
  return data
}

/** Citas de una mascota, la más reciente primero. */
export async function getCitasDeMascota(mascotaId: number): Promise<Cita[]> {
  if (USE_MOCKS) {
    return simularRespuesta(
      citas
        .filter((c) => c.mascotaId === mascotaId)
        .sort((a, b) => b.fechaHora.localeCompare(a.fechaHora)),
    )
  }
  const { data } = await citasApi.get<Cita[]>("/citas", {
    params: { mascotaId },
  })
  return data
}

/** Agenda del día de un veterinario, en orden de hora. fecha: "2026-11-20". */
export async function getAgendaVeterinario(
  veterinarioId: number,
  fecha: string,
): Promise<Cita[]> {
  if (USE_MOCKS) {
    buscarVeterinario(veterinarioId)
    return simularRespuesta(
      citas
        .filter(
          (c) =>
            c.veterinarioId === veterinarioId && c.fechaHora.startsWith(fecha),
        )
        .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora)),
    )
  }
  const { data } = await citasApi.get<Cita[]>("/citas", {
    params: { veterinarioId, fecha },
  })
  return data
}

/** Cancela una cita. Una cita completada no se puede cancelar (409). */
export async function cancelarCita(id: number): Promise<Cita> {
  if (USE_MOCKS) {
    const cita = buscarCita(id)
    if (cita.estado === "COMPLETADA") {
      throw errorSimulado(
        409,
        "CONFLICTO",
        "Una cita completada no puede cancelarse",
      )
    }
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
    if (cita.estado !== "AGENDADA") {
      throw errorSimulado(
        409,
        "CONFLICTO",
        "Solo puede registrarse la consulta de una cita agendada",
      )
    }
    cita.estado = "COMPLETADA"
    const registro: RegistroHistorial = {
      id: Math.max(0, ...historial.map((h) => h.id)) + 1,
      citaId,
      mascotaId: cita.mascotaId,
      veterinarioId: cita.veterinarioId,
      fecha: ahoraIsoLocal(),
      tipoConsulta: datos.tipoConsulta,
      diagnostico: datos.diagnostico.trim(),
      vacunasAplicadas: datos.vacunasAplicadas ?? [],
      tratamientos: datos.tratamientos ?? [],
      examenes: datos.examenes ?? [],
      observaciones: datos.observaciones?.trim() || null,
      proximoControl: datos.proximoControl ?? null,
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

/** Historial clínico de una mascota, la consulta más reciente primero. */
export async function getHistorial(
  mascotaId: number,
): Promise<RegistroHistorial[]> {
  if (USE_MOCKS) {
    return simularRespuesta(
      historial
        .filter((h) => h.mascotaId === mascotaId)
        .sort((a, b) => b.fecha.localeCompare(a.fecha)),
    )
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
