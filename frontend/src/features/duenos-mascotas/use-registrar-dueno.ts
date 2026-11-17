import { useMutation, useQueryClient } from "@tanstack/react-query"
import { registrarDueno } from "@/api/mascotas"

/** Registra un dueño y refresca la lista del selector de perfil. */
export function useRegistrarDueno() {
  const queryClient = useQueryClient()
  return useMutation({
    meta: { errorEnPantalla: true }, // la vista muestra el error
    mutationFn: registrarDueno,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["duenos"] }),
  })
}
