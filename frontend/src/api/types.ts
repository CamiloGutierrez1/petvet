// Tipos de los contratos de API acordados con el back.
// Son la única dependencia del front con los microservicios: si un contrato
// cambia, se cambia aquí primero.

// ---------- Enums ----------

export type Especie = "PERRO" | "GATO" | "AVE" | "OTRO"
export type EstadoCita = "AGENDADA" | "COMPLETADA" | "CANCELADA"
export type TipoServicio =
  "CONSULTA_GENERAL" | "VACUNACION" | "CONTROL" | "ESPECIALISTA"

// ---------- ms-mascotas-duenos (8081) ----------

export interface Dueno {
  id: number
  nombre: string
  email: string
  telefono?: string // confirmar con back
}

export interface Mascota {
  id: number
  nombre: string
  especie: Especie
  raza: string
  fechaNacimiento: string // ISO, ej. "2021-04-18"
  peso: number // kg
  fotoUrl?: string
  duenoId: number
}

/** GET /api/mascotas/{id}: la ficha incluye nombre y correo del dueño. */
export interface MascotaDetalle extends Mascota {
  duenoNombre: string // confirmar con back
  duenoEmail: string // confirmar con back
}

export type NuevoDueno = Omit<Dueno, "id">
export type NuevaMascota = Omit<Mascota, "id" | "duenoId" | "fotoUrl">
export type EditarMascota = NuevaMascota

// ---------- ms-citas-historial (8082) ----------

export interface Veterinario {
  id: number
  nombre: string
  especialidad?: string // confirmar con back
}

export interface Cita {
  id: number
  mascotaId: number
  veterinarioId: number
  fechaHora: string // ISO, ej. "2026-11-20T09:30:00"
  tipoServicio: TipoServicio
  estado: EstadoCita
}

/** POST /api/citas */
export interface NuevaCita {
  mascotaId: number
  veterinarioId: number
  fechaHora: string
  tipoServicio: TipoServicio
}

/** GET /api/veterinarios/{id}/disponibilidad?fecha=...: horas libres del día. */
export type Disponibilidad = string[] // confirmar con back (ej. ["2026-11-20T09:30:00", ...])

/** POST /api/citas/{id}/consulta */
export interface RegistrarConsulta {
  tipoConsulta: string // confirmar con back (ej. "GENERAL", "ESPECIALISTA")
  diagnostico: string
  vacunasAplicadas: string[]
  tratamientos: string[]
  examenes: string[]
  observaciones?: string
  proximoControl?: string // ISO, ej. "2026-12-04"
}

/** GET /api/mascotas/{id}/historial: una entrada por consulta registrada. */
export interface RegistroHistorial extends RegistrarConsulta {
  id: number // confirmar con back
  citaId: number // confirmar con back
  fecha: string // confirmar con back
  veterinarioId: number // confirmar con back
}

/** GET /api/mascotas/{id}/proximas-vacunas */
export interface ProximaVacuna {
  vacuna: string // confirmar con back
  fechaLimite: string // confirmar con back
}

// ---------- Errores (comunes a los dos servicios) ----------

/** Cuerpo de error con códigos 400, 404 y 409 (horario ocupado). */
export interface ApiError {
  error: string
  detalle?: string
}
