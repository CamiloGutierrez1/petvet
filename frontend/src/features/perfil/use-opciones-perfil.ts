import { useQuery } from "@tanstack/react-query"
import { getVeterinarios } from "@/api/citas"
import { getDuenos } from "@/api/mascotas"

// Consultas del selector de perfil. Las páginas no llaman a la API
// directamente: usan estos hooks.

export function useDuenos() {
  return useQuery({ queryKey: ["duenos"], queryFn: getDuenos })
}

export function useVeterinarios() {
  return useQuery({ queryKey: ["veterinarios"], queryFn: getVeterinarios })
}
