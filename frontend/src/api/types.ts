// Tipos de los contratos de API acordados con el back.
// Son la única dependencia del front con los microservicios: si un contrato
// cambia, se cambia aquí primero.

// ---------- Enums ----------

export type Especie = "PERRO" | "GATO" | "AVE" | "OTRO"
export type EstadoCita = "AGENDADA" | "COMPLETADA" | "CANCELADA"
export type TipoServicio =
  "CONSULTA_GENERAL" | "VACUNACION" | "CONTROL" | "ESPECIALISTA"

// ---------- ms-mascotas-duenos (8081) ----------

// Contrato confirmado en docs/api.md (PR #27). Los opcionales llegan como null.

export interface Dueno {
  id: number
  nombre: string
  documento: string | null
  email: string
  telefono: string | null // solo dígitos, entre 7 y 10
  direccion: string | null
}

export interface Mascota {
  id: number
  nombre: string
  especie: Especie
  raza: string
  fechaNacimiento: string // ISO, ej. "2021-04-18"
  peso: number // kg, hasta 3 decimales
  /** Relativa al front en los datos de ejemplo; absoluta al 8081 si se subió una foto. */
  fotoUrl: string | null
  duenoId: number
}

/** GET /api/mascotas/{id}: la ficha incluye nombre y correo del dueño. */
export interface MascotaDetalle extends Mascota {
  duenoNombre: string
  duenoEmail: string
}

/** POST /api/duenos: los opcionales se pueden omitir. */
export interface NuevoDueno {
  nombre: string
  email: string
  documento?: string
  telefono?: string
  direccion?: string
}
export type NuevaMascota = Omit<Mascota, "id" | "duenoId" | "fotoUrl">
export type EditarMascota = NuevaMascota

// ---------- ms-citas-historial (8082) ----------
// Contrato confirmado por Jersson en docs/api.md (PR #2 y #29).

export type TipoConsulta = "GENERAL" | "ESPECIALISTA"

export interface Veterinario {
  id: number
  nombre: string
  especialidad: string
  horaInicio: string // "HH:mm", ej. "08:00"
  horaFin: string // "HH:mm", ej. "18:00"
}

export interface Cita {
  id: number
  mascotaId: number
  veterinarioId: number
  fechaHora: string // ISO local, ej. "2026-11-20T09:30:00"
  tipoServicio: TipoServicio
  estado: EstadoCita
  duracionMinutos: number // hoy siempre 30
}

/** POST /api/citas (responde 201 con la Cita creada). */
export interface NuevaCita {
  mascotaId: number
  veterinarioId: number
  fechaHora: string // debe ser una hora devuelta por la disponibilidad
  tipoServicio: TipoServicio
}

/**
 * GET /api/veterinarios/{id}/disponibilidad?fecha=2026-11-20
 * Turnos libres de 30 min dentro del horario del veterinario, sin horas pasadas.
 */
export interface Disponibilidad {
  veterinarioId: number
  fecha: string // "2026-11-20"
  horarios: string[] // ej. ["2026-11-20T08:00:00", "2026-11-20T08:30:00", ...]
}

/** POST /api/citas/{id}/consulta (responde 201 con el RegistroHistorial). */
export interface RegistrarConsulta {
  tipoConsulta: TipoConsulta
  diagnostico: string // obligatorio
  vacunasAplicadas?: string[]
  tratamientos?: string[]
  examenes?: string[]
  cirugias?: string[]
  observaciones?: string
  proximoControl?: string // "2026-12-04", hoy o después
}

/** GET /api/mascotas/{id}/historial: una entrada por consulta, la más reciente primero. */
export interface RegistroHistorial {
  id: number
  citaId: number
  mascotaId: number
  veterinarioId: number
  fecha: string // fecha y hora en que se registró la consulta
  tipoConsulta: TipoConsulta
  diagnostico: string
  vacunasAplicadas: string[]
  tratamientos: string[]
  examenes: string[]
  cirugias: string[]
  observaciones: string | null
  proximoControl: string | null
}

/** GET /api/mascotas/{id}/proximas-vacunas */
export interface ProximaVacuna {
  vacuna: string
  fechaLimite: string // "2026-12-12"
}

// ---------- Errores (comunes a los servicios) ----------

/** Código en el campo `error`, igual en los tres servicios. */
export type CodigoError =
  | "DATOS_INVALIDOS" // 400
  | "RECURSO_NO_ENCONTRADO" // 404
  | "CONFLICTO" // 409: turno ocupado, correo repetido o estado incompatible
  | "DEPENDENCIA_NO_DISPONIBLE" // 503: otro microservicio no responde
  | "ERROR_INTERNO" // 500

/**
 * Cuerpo de todos los errores. `error` es un código para el programa;
 * `detalle` es el mensaje en español que se le muestra al usuario.
 */
export interface ApiError {
  error: CodigoError | (string & {})
  detalle?: string
}
