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

/** "2026-12-12" → "12 dic" (para textos cortos como "Rabia, 12 dic") */
export function formatearFechaCorta(fecha: string): string {
  return format(parseISO(fecha), "d MMM", { locale: es }).replace(".", "")
}

/** "2026-11-20T09:30:00" → "9:30 a. m." */
export function formatearHora(fechaHora: string): string {
  const fecha = parseISO(fechaHora)
  const sufijo = fecha.getHours() < 12 ? "a. m." : "p. m."
  return `${format(fecha, "h:mm")} ${sufijo}`
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

/** "en 31 días", "mañana", "hoy", "venció hace 3 días". */
export function textoPlazo(fecha: string, hoy = new Date()): string {
  const dias = diasHasta(fecha, hoy)
  if (dias === 0) return "hoy"
  if (dias === 1) return "mañana"
  if (dias > 1) return `en ${dias} días`
  if (dias === -1) return "venció ayer"
  return `venció hace ${-dias} días`
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
