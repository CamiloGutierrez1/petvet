import { Navigate, Outlet } from "react-router-dom"
import { usePerfil, type TipoPerfil } from "./perfil-context"

interface RequierePerfilProps {
  tipo: TipoPerfil
}

/**
 * Protege un grupo de rutas: si no hay perfil elegido, o es de otro tipo
 * (un veterinario entrando a /dueno), vuelve al selector de perfil.
 */
export function RequierePerfil({ tipo }: RequierePerfilProps) {
  const { perfil } = usePerfil()
  if (!perfil || perfil.tipo !== tipo) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
