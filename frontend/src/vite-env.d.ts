/// <reference types="vite/client" />

// Variables de entorno del front (ver .env.example).
interface ImportMetaEnv {
  readonly VITE_API_MASCOTAS: string
  readonly VITE_API_CITAS: string
  readonly VITE_USE_MOCKS: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
