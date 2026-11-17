import { useCallback, useMemo, useState, type ReactNode } from "react"
import { PerfilContext, type Perfil } from "./perfil-context"

const CLAVE = "petvet.perfil"

/** Lee el perfil guardado en el navegador; si no hay o está dañado, null. */
function leerPerfilGuardado(): Perfil | null {
  try {
    const guardado = JSON.parse(localStorage.getItem(CLAVE) ?? "null")
    if (
      guardado &&
      (guardado.tipo === "dueno" || guardado.tipo === "veterinario") &&
      Number.isInteger(guardado.id) &&
      typeof guardado.nombre === "string"
    ) {
      return guardado
    }
  } catch {
    // JSON inválido o localStorage bloqueado: se elige de nuevo.
  }
  return null
}

/** Guarda el perfil elegido en el navegador para que sobreviva a recargar. */
export function PerfilProvider({ children }: { children: ReactNode }) {
  const [perfil, setPerfil] = useState<Perfil | null>(leerPerfilGuardado)

  const elegirPerfil = useCallback((nuevo: Perfil) => {
    setPerfil(nuevo)
    try {
      localStorage.setItem(CLAVE, JSON.stringify(nuevo))
    } catch {
      // Sin almacenamiento: el perfil dura hasta recargar la página.
    }
  }, [])

  const salirDelPerfil = useCallback(() => {
    setPerfil(null)
    try {
      localStorage.removeItem(CLAVE)
    } catch {
      // Nada que limpiar.
    }
  }, [])

  const valor = useMemo(
    () => ({ perfil, elegirPerfil, salirDelPerfil }),
    [perfil, elegirPerfil, salirDelPerfil],
  )

  return (
    <PerfilContext.Provider value={valor}>{children}</PerfilContext.Provider>
  )
}
