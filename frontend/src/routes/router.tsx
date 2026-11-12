import { createBrowserRouter, Navigate } from "react-router-dom"
import { PaginaPendiente } from "@/components/pagina-pendiente"
import { AgendarCitaPage } from "@/features/citas/agendar-cita-page"
import { FichaMascotaPage } from "@/features/duenos-mascotas/ficha-mascota-page"
import { MisMascotasPage } from "@/features/duenos-mascotas/mis-mascotas-page"
import { RegistroDuenoPage } from "@/features/duenos-mascotas/registro-dueno-page"
import { RequierePerfil } from "@/features/perfil/requiere-perfil"
import { SelectorPerfilPage } from "@/features/perfil/selector-perfil-page"
import { LayoutDueno } from "@/layouts/LayoutDueno"
import { LayoutVeterinario } from "@/layouts/LayoutVeterinario"

// Rutas de la aplicación. Las vistas que aún no existen muestran una página
// provisional; se reemplazan a medida que se construyen.
// Cada grupo: RequierePerfil (exige ese perfil) → Layout (barra superior) → vista.
export const router = createBrowserRouter([
  { path: "/", element: <SelectorPerfilPage /> },
  { path: "/registro", element: <RegistroDuenoPage /> },

  // ---------- Dueño (Camilo) ----------
  {
    path: "/dueno",
    element: <RequierePerfil tipo="dueno" />,
    children: [
      {
        element: <LayoutDueno />,
        children: [
          {
            index: true,
            element: <PaginaPendiente titulo="Inicio" responsable="Camilo" />,
          },
          {
            path: "mascotas",
            element: <MisMascotasPage />,
          },
          {
            path: "mascotas/:mascotaId",
            element: <FichaMascotaPage />,
          },
          {
            path: "agendar",
            element: <AgendarCitaPage />,
          },
          {
            path: "citas",
            element: (
              <PaginaPendiente titulo="Mis citas" responsable="Camilo" />
            ),
          },
        ],
      },
    ],
  },

  // ---------- Veterinario (Daza) ----------
  {
    path: "/veterinario",
    element: <RequierePerfil tipo="veterinario" />,
    children: [
      {
        element: <LayoutVeterinario />,
        children: [
          { index: true, element: <Navigate to="agenda" replace /> },
          {
            path: "agenda",
            element: (
              <PaginaPendiente titulo="Agenda del día" responsable="Daza" />
            ),
          },
          {
            path: "consulta/:citaId",
            element: (
              <PaginaPendiente titulo="Registrar consulta" responsable="Daza" />
            ),
          },
          {
            path: "pacientes",
            element: <PaginaPendiente titulo="Pacientes" responsable="Daza" />,
          },
          {
            path: "pacientes/:mascotaId",
            element: (
              <PaginaPendiente titulo="Ficha del paciente" responsable="Daza" />
            ),
          },
        ],
      },
    ],
  },

  { path: "*", element: <Navigate to="/" replace /> },
])
