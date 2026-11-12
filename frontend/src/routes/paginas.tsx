import { lazy } from "react"

// Carga bajo demanda (code splitting): cada pantalla es un archivo aparte que
// se descarga solo cuando se visita. Mientras llega, se muestra un skeleton
// (Suspense en LayoutDueno, LayoutVeterinario y main.tsx).
// Van en este archivo, separado de router.tsx, para que la recarga rápida de
// Vite funcione (un archivo que exporta componentes no debe exportar nada más).

export const RegistroDuenoPage = lazy(() =>
  import("@/features/duenos-mascotas/registro-dueno-page").then((m) => ({
    default: m.RegistroDuenoPage,
  })),
)

// ---------- Dueño ----------

export const InicioDuenoPage = lazy(() =>
  import("@/features/duenos-mascotas/inicio-dueno-page").then((m) => ({
    default: m.InicioDuenoPage,
  })),
)
export const MisMascotasPage = lazy(() =>
  import("@/features/duenos-mascotas/mis-mascotas-page").then((m) => ({
    default: m.MisMascotasPage,
  })),
)
export const FichaMascotaPage = lazy(() =>
  import("@/features/duenos-mascotas/ficha-mascota-page").then((m) => ({
    default: m.FichaMascotaPage,
  })),
)
export const AgendarCitaPage = lazy(() =>
  import("@/features/citas/agendar-cita-page").then((m) => ({
    default: m.AgendarCitaPage,
  })),
)
export const MisCitasPage = lazy(() =>
  import("@/features/citas/mis-citas-page").then((m) => ({
    default: m.MisCitasPage,
  })),
)

// ---------- Veterinario ----------

export const AgendaPage = lazy(() =>
  import("@/features/veterinario/agenda-page").then((m) => ({
    default: m.AgendaPage,
  })),
)
export const RegistrarConsultaPage = lazy(() =>
  import("@/features/veterinario/registrar-consulta-page").then((m) => ({
    default: m.RegistrarConsultaPage,
  })),
)
export const FichaPacientePage = lazy(() =>
  import("@/features/veterinario/ficha-paciente-page").then((m) => ({
    default: m.FichaPacientePage,
  })),
)
