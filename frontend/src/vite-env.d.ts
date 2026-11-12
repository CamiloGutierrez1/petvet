/// <reference types="vite/client" />

// Variables de entorno del front (ver .env.example).
interface ImportMetaEnv {
  readonly VITE_API_MASCOTAS: string
  readonly VITE_API_CITAS: string
  /** Valor por defecto para los dos servicios ("true" o "false"). */
  readonly VITE_USE_MOCKS: string
  /** Opcional: solo para Mascotas. Si no está, usa VITE_USE_MOCKS. */
  readonly VITE_USE_MOCKS_MASCOTAS?: string
  /** Opcional: solo para Citas. Si no está, usa VITE_USE_MOCKS. */
  readonly VITE_USE_MOCKS_CITAS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
