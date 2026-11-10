import axios from "axios"

// Un cliente por microservicio: el front habla con dos servicios
// independientes, no con un servidor único.
export const mascotasApi = axios.create({
  baseURL: import.meta.env.VITE_API_MASCOTAS,
})

export const citasApi = axios.create({
  baseURL: import.meta.env.VITE_API_CITAS,
})

/** true: las funciones de src/api devuelven los JSON de src/mocks. */
export const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === "true"

/**
 * Simula la latencia de red en modo mocks, para que los estados de carga
 * (skeletons) se vean igual que con la API real.
 */
export function simularRespuesta<T>(datos: T, ms = 400): Promise<T> {
  return new Promise((resolve) =>
    setTimeout(() => resolve(structuredClone(datos)), ms),
  )
}
