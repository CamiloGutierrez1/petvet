import axios, { AxiosError, AxiosHeaders } from "axios"
import type { ApiError } from "./types"

// Un cliente por microservicio: el front habla con dos servicios
// independientes, no con un servidor único.
export const mascotasApi = axios.create({
  baseURL: import.meta.env.VITE_API_MASCOTAS,
})

export const citasApi = axios.create({
  baseURL: import.meta.env.VITE_API_CITAS,
})

/**
 * true: las funciones de ese servicio devuelven los JSON de src/mocks.
 * Cada microservicio tiene su propio interruptor, para conectar uno a la API
 * real mientras el otro sigue simulado. Si no se define el específico, se
 * usa VITE_USE_MOCKS para los dos.
 */
function usarMocks(especifico: string | undefined): boolean {
  return (especifico ?? import.meta.env.VITE_USE_MOCKS) === "true"
}

export const USE_MOCKS_MASCOTAS = usarMocks(
  import.meta.env.VITE_USE_MOCKS_MASCOTAS,
)
export const USE_MOCKS_CITAS = usarMocks(import.meta.env.VITE_USE_MOCKS_CITAS)

/**
 * Simula la latencia de red en modo mocks, para que los estados de carga
 * (skeletons) se vean igual que con la API real.
 */
export function simularRespuesta<T>(datos: T, ms = 400): Promise<T> {
  return new Promise((resolve) =>
    setTimeout(() => resolve(structuredClone(datos)), ms),
  )
}

/**
 * En modo mocks, crea un error con la misma forma que devuelve Axios cuando
 * el back responde con error ({ error, detalle } y su código HTTP). Así el
 * manejo de errores de las vistas funciona igual con mocks y con la API real.
 */
export function errorSimulado(
  status: number,
  error: ApiError["error"],
  detalle: string,
): AxiosError<ApiError> {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError<ApiError>(
    detalle,
    AxiosError.ERR_BAD_REQUEST,
    config,
    null,
    {
      status,
      statusText: error,
      data: { error, detalle },
      headers: {},
      config,
    },
  )
}

/** Fecha y hora local actual en ISO sin zona, como la usa el back: "2026-11-10T15:30:00". */
export function ahoraIsoLocal(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, "0")
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
