import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  editarMascota,
  getMascotasDeDueno,
  registrarMascota,
  subirFotoMascota,
} from "@/api/mascotas"
import type { Mascota, NuevaMascota } from "@/api/types"

/** Mascotas de un dueño. */
export function useMascotasDeDueno(duenoId: number) {
  return useQuery({
    queryKey: ["mascotas", duenoId],
    queryFn: () => getMascotasDeDueno(duenoId),
  })
}

interface GuardarMascotaArgs {
  /** Si viene, se edita esa mascota; si no, se registra una nueva. */
  mascotaId?: number
  datos: NuevaMascota
  foto?: File
}

interface GuardarMascotaResultado {
  mascota: Mascota
  /** La mascota se guardó, pero la foto no se pudo subir. */
  errorFoto?: unknown
}

/**
 * Registra o edita una mascota y, si se eligió una foto, la sube después.
 * Si falla solo la foto, la mascota igual queda guardada y se informa aparte.
 */
export function useGuardarMascota(duenoId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    meta: { errorEnPantalla: true }, // la vista muestra el error
    mutationFn: async ({
      mascotaId,
      datos,
      foto,
    }: GuardarMascotaArgs): Promise<GuardarMascotaResultado> => {
      const mascota = mascotaId
        ? await editarMascota(mascotaId, datos)
        : await registrarMascota(duenoId, datos)
      if (!foto) return { mascota }
      try {
        return { mascota: await subirFotoMascota(mascota.id, foto) }
      } catch (errorFoto) {
        return { mascota, errorFoto }
      }
    },
    onSuccess: ({ mascota }) => {
      queryClient.invalidateQueries({ queryKey: ["mascotas", duenoId] })
      queryClient.invalidateQueries({ queryKey: ["mascota", mascota.id] })
    },
  })
}
