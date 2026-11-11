import { isAxiosError } from "axios"
import type { ApiError } from "./types"

/**
 * Mensaje para mostrarle al usuario a partir de cualquier error de src/api.
 * El back (y los mocks, con errorSimulado) mandan { error: CODIGO, detalle };
 * el texto en español está en `detalle`. Si no hay respuesta del servidor
 * (servicio apagado o sin red), devuelve un mensaje genérico.
 */
export function mensajeDeError(
  error: unknown,
  porDefecto = "Ocurrió un error inesperado. Intenta de nuevo.",
): string {
  if (isAxiosError<ApiError>(error)) {
    const detalle = error.response?.data?.detalle
    if (detalle) return detalle
    if (!error.response) {
      return "No se pudo conectar con el servidor. Revisa que el servicio esté encendido."
    }
  }
  return porDefecto
}

/** Código HTTP del error (409, 404, 503...), o undefined si no hubo respuesta. */
export function estadoHttp(error: unknown): number | undefined {
  return isAxiosError(error) ? error.response?.status : undefined
}
