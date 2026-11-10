import { createBrowserRouter, Navigate } from "react-router-dom"
import { PaginaPendiente } from "@/components/pagina-pendiente"

// Rutas de la aplicación. Por ahora cada una muestra una página provisional;
// se reemplazan por las vistas reales a medida que se construyen.
// Los layouts (LayoutDueno, LayoutVeterinario) se agregan el miércoles 11.
export const router = createBrowserRouter([
  {
    path: "/",
    element: (
      <PaginaPendiente titulo="Selector de perfil" responsable="Camilo" />
    ),
  },

  // ---------- Dueño (Camilo) ----------
  {
    path: "/dueno",
    children: [
      { index: true, element: <Navigate to="mascotas" replace /> },
      {
        path: "registro",
        element: (
          <PaginaPendiente titulo="Registro de dueño" responsable="Camilo" />
        ),
      },
      {
        path: "mascotas",
        element: <PaginaPendiente titulo="Mis mascotas" responsable="Camilo" />,
      },
      {
        path: "mascotas/nueva",
        element: (
          <PaginaPendiente titulo="Registrar mascota" responsable="Camilo" />
        ),
      },
      {
        path: "mascotas/:mascotaId",
        element: (
          <PaginaPendiente titulo="Ficha de la mascota" responsable="Camilo" />
        ),
      },
      {
        path: "mascotas/:mascotaId/editar",
        element: (
          <PaginaPendiente titulo="Editar mascota" responsable="Camilo" />
        ),
      },
      {
        path: "citas/nueva",
        element: <PaginaPendiente titulo="Agendar cita" responsable="Camilo" />,
      },
    ],
  },

  // ---------- Veterinario (Daza) ----------
  {
    path: "/veterinario",
    children: [
      { index: true, element: <Navigate to="agenda" replace /> },
      {
        path: "agenda",
        element: <PaginaPendiente titulo="Agenda del día" responsable="Daza" />,
      },
      {
        path: "citas/:citaId/consulta",
        element: (
          <PaginaPendiente titulo="Registrar consulta" responsable="Daza" />
        ),
      },
      {
        path: "mascotas/:mascotaId/historial",
        element: (
          <PaginaPendiente titulo="Historial clínico" responsable="Daza" />
        ),
      },
    ],
  },

  { path: "*", element: <Navigate to="/" replace /> },
])
