import { differenceInCalendarDays, format, parseISO } from "date-fns"
import { es } from "date-fns/locale"
import type { TipoServicio } from "@/api/types"

// Formatos compartidos por las vistas del dueño y del veterinario.
// El back manda fechas ISO locales sin zona ("2026-11-20T09:30:00" o
// "2026-12-12"); parseISO las interpreta en la hora local del navegador.

/** "2026-12-12" → "12 dic 2026" */
export function formatearFecha(fecha: string): string {
  return format(parseISO(fecha), "d MMM yyyy", { locale: es }).replace(".", "")
}

/**
 * Texto corto como "Rabia, 12 dic". Si la fecha no es del año actual, agrega
 * el año ("12 nov 2023") para que no parezca de este mes.
 */
export function formatearFechaCorta(fecha: string, hoy = new Date()): string {
  const f = parseISO(fecha)
  const patron = f.getFullYear() === hoy.getFullYear() ? "d MMM" : "d MMM yyyy"
  return format(f, patron, { locale: es }).replace(".", "")
}

/** "2026-11-20T09:30:00" → "9:30 a. m." */
export function formatearHora(fechaHora: string): string {
  const fecha = parseISO(fechaHora)
  const sufijo = fecha.getHours() < 12 ? "a. m." : "p. m."
  return `${format(fecha, "h:mm")} ${sufijo}`
}

/** Horario de un veterinario: ("08:00", "18:00") → "8:00 a. m. a 6:00 p. m." */
export function formatearHorario(horaInicio: string, horaFin: string): string {
  const hora = (hhmm: string) => formatearHora(`2000-01-01T${hhmm}:00`)
  return `${hora(horaInicio)} a ${hora(horaFin)}`
}

/** "2026-11-20T09:30:00" → "vie 20 nov 2026, 9:30 a. m." */
export function formatearFechaHora(fechaHora: string): string {
  const fecha = parseISO(fechaHora)
  const dia = format(fecha, "EEE d MMM yyyy", { locale: es }).replaceAll(
    ".",
    "",
  )
  return `${dia}, ${formatearHora(fechaHora)}`
}

/** Días desde hoy hasta la fecha (negativo si ya pasó). */
export function diasHasta(fecha: string, hoy = new Date()): number {
  return differenceInCalendarDays(parseISO(fecha), hoy)
}

/**
 * Plazo legible: "en 31 días", "mañana", "hoy", "venció ayer",
 * "venció hace 3 días", "venció hace 2 meses", "venció hace más de un año".
 */
export function textoPlazo(fecha: string, hoy = new Date()): string {
  const dias = diasHasta(fecha, hoy)
  if (dias === 0) return "hoy"
  if (dias === 1) return "mañana"
  if (dias > 1) return `en ${dias} días`
  if (dias === -1) return "venció ayer"
  const pasados = -dias
  if (pasados <= 30) return `venció hace ${pasados} días`
  if (pasados < 365) {
    const meses = Math.floor(pasados / 30)
    return `venció hace ${meses} ${meses === 1 ? "mes" : "meses"}`
  }
  return "venció hace más de un año"
}

const SERVICIOS: Record<TipoServicio, string> = {
  CONSULTA_GENERAL: "Consulta general",
  VACUNACION: "Vacunación",
  CONTROL: "Control",
  ESPECIALISTA: "Especialista",
}

export function etiquetaServicio(tipo: TipoServicio): string {
  return SERVICIOS[tipo]
}

export const TIPOS_SERVICIO = Object.entries(SERVICIOS).map(
  ([valor, etiqueta]) => ({ valor: valor as TipoServicio, etiqueta }),
)
