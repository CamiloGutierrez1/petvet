import { createContext, useContext } from "react"

// No hay login real: el usuario elige con qué perfil entra (Dueño o
// Veterinario) y la app recuerda su id. Este contexto lo comparte con
// todas las vistas sin pasarlo de componente en componente.

export type TipoPerfil = "dueno" | "veterinario"

export interface Perfil {
  tipo: TipoPerfil
  id: number
  nombre: string
}

export interface PerfilContextValue {
  /** Perfil activo, o null si todavía no se ha elegido. */
  perfil: Perfil | null
  elegirPerfil: (perfil: Perfil) => void
  /** Olvida el perfil activo (botón "Cambiar perfil"). */
  salirDelPerfil: () => void
}

export const PerfilContext = createContext<PerfilContextValue | null>(null)

/** Perfil activo y acciones para cambiarlo. Usar dentro de <PerfilProvider>. */
export function usePerfil(): PerfilContextValue {
  const contexto = useContext(PerfilContext)
  if (!contexto) {
    throw new Error("usePerfil debe usarse dentro de <PerfilProvider>")
  }
  return contexto
}

/** Iniciales para el avatar: "Laura Méndez" → "LM". */
export function iniciales(nombre: string): string {
  return nombre
    .replace(/^(Dra?\.)\s+/i, "")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]!.toUpperCase())
    .join("")
}
