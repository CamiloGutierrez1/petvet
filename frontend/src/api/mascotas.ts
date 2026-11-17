// Funciones del microservicio ms-mascotas-duenos (puerto 8081).
// Cada función llama a la API real o, si VITE_USE_MOCKS=true, responde con
// los datos de src/mocks. Las páginas no saben cuál de los dos se usa.

import duenosMock from "@/mocks/duenos.json"
import mascotasMock from "@/mocks/mascotas.json"
import {
  errorSimulado,
  mascotasApi,
  simularRespuesta,
  USE_MOCKS_MASCOTAS,
} from "./http"
import type {
  Dueno,
  EditarMascota,
  Mascota,
  MascotaDetalle,
  NuevaMascota,
  NuevoDueno,
} from "./types"

// Copia en memoria para que, en modo mocks, lo que se registra aparezca en
// las listas hasta recargar la página.
const duenos: Dueno[] = [...(duenosMock as Dueno[])]
const mascotas: Mascota[] = [...(mascotasMock as Mascota[])]

const siguienteId = (items: { id: number }[]) =>
  Math.max(0, ...items.map((i) => i.id)) + 1

function buscarMascota(id: number): Mascota {
  const mascota = mascotas.find((m) => m.id === id)
  if (!mascota) {
    throw errorSimulado(
      404,
      "RECURSO_NO_ENCONTRADO",
      `No existe la mascota ${id}`,
    )
  }
  return mascota
}

// ---------- Dueños ----------

export async function registrarDueno(datos: NuevoDueno): Promise<Dueno> {
  if (USE_MOCKS_MASCOTAS) {
    // Igual que el back: correo en minúsculas y único, opcionales vacíos como null.
    const correo = datos.email.trim().toLowerCase()
    if (duenos.some((d) => d.email.toLowerCase() === correo)) {
      throw errorSimulado(
        409,
        "CONFLICTO",
        "Ya existe un dueño registrado con ese correo.",
      )
    }
    const opcional = (valor?: string) => valor?.trim() || null
    const dueno: Dueno = {
      id: siguienteId(duenos),
      nombre: datos.nombre.trim(),
      documento: opcional(datos.documento),
      email: correo,
      telefono: opcional(datos.telefono),
      direccion: opcional(datos.direccion),
    }
    duenos.push(dueno)
    return simularRespuesta(dueno)
  }
  const { data } = await mascotasApi.post<Dueno>("/duenos", datos)
  return data
}

export async function getDuenos(): Promise<Dueno[]> {
  if (USE_MOCKS_MASCOTAS) return simularRespuesta(duenos)
  const { data } = await mascotasApi.get<Dueno[]>("/duenos")
  return data
}

export async function getDueno(id: number): Promise<Dueno> {
  if (USE_MOCKS_MASCOTAS) {
    const dueno = duenos.find((d) => d.id === id)
    if (!dueno) {
      throw errorSimulado(
        404,
        "RECURSO_NO_ENCONTRADO",
        `No existe el dueño ${id}`,
      )
    }
    return simularRespuesta(dueno)
  }
  const { data } = await mascotasApi.get<Dueno>(`/duenos/${id}`)
  return data
}

// ---------- Mascotas ----------

export async function registrarMascota(
  duenoId: number,
  datos: NuevaMascota,
): Promise<Mascota> {
  if (USE_MOCKS_MASCOTAS) {
    const mascota: Mascota = {
      id: siguienteId(mascotas),
      duenoId,
      fotoUrl: null,
      ...datos,
    }
    mascotas.push(mascota)
    return simularRespuesta(mascota)
  }
  const { data } = await mascotasApi.post<Mascota>(
    `/duenos/${duenoId}/mascotas`,
    datos,
  )
  return data
}

export async function getMascotasDeDueno(duenoId: number): Promise<Mascota[]> {
  if (USE_MOCKS_MASCOTAS) {
    return simularRespuesta(mascotas.filter((m) => m.duenoId === duenoId))
  }
  const { data } = await mascotasApi.get<Mascota[]>(
    `/duenos/${duenoId}/mascotas`,
  )
  return data
}

export async function getMascota(id: number): Promise<MascotaDetalle> {
  if (USE_MOCKS_MASCOTAS) {
    const mascota = buscarMascota(id)
    const dueno = duenos.find((d) => d.id === mascota.duenoId)
    return simularRespuesta({
      ...mascota,
      duenoNombre: dueno?.nombre ?? "",
      duenoEmail: dueno?.email ?? "",
    })
  }
  const { data } = await mascotasApi.get<MascotaDetalle>(`/mascotas/${id}`)
  return data
}

export async function editarMascota(
  id: number,
  datos: EditarMascota,
): Promise<Mascota> {
  if (USE_MOCKS_MASCOTAS) {
    const mascota = buscarMascota(id)
    Object.assign(mascota, datos)
    return simularRespuesta(mascota)
  }
  const { data } = await mascotasApi.put<Mascota>(`/mascotas/${id}`, datos)
  return data
}

export async function subirFotoMascota(
  id: number,
  foto: File,
): Promise<Mascota> {
  if (USE_MOCKS_MASCOTAS) {
    const mascota = buscarMascota(id)
    mascota.fotoUrl = URL.createObjectURL(foto)
    return simularRespuesta(mascota)
  }
  const formData = new FormData()
  formData.append("foto", foto) // JPG, PNG o WebP de hasta 5 MB
  const { data } = await mascotasApi.post<Mascota>(
    `/mascotas/${id}/foto`,
    formData,
  )
  return data
}
