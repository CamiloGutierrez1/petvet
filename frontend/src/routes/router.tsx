import { createBrowserRouter, Navigate } from "react-router-dom"
import { PaginaPendiente } from "@/components/pagina-pendiente"
import { RequierePerfil } from "@/features/perfil/requiere-perfil"
import { SelectorPerfilPage } from "@/features/perfil/selector-perfil-page"
import { LayoutDueno } from "@/layouts/LayoutDueno"
import { LayoutVeterinario } from "@/layouts/LayoutVeterinario"
import {
  AgendaPage,
  AgendarCitaPage,
  FichaMascotaPage,
  FichaPacientePage,
  InicioDuenoPage,
  MisCitasPage,
  MisMascotasPage,
  RegistrarConsultaPage,
  RegistroDuenoPage,
} from "./paginas"

// Rutas de la aplicación. Las pantallas se cargan bajo demanda (./paginas);
// el selector de perfil se carga de inmediato porque es la primera.
// Cada grupo: RequierePerfil (exige ese perfil) → Layout (barra superior) → vista.
export const router = createBrowserRouter([
  { path: "/", element: <SelectorPerfilPage /> },
  { path: "/registro", element: <RegistroDuenoPage /> },

  // ---------- Dueño ----------
  {
    path: "/dueno",
    element: <RequierePerfil tipo="dueno" />,
    children: [
      {
        element: <LayoutDueno />,
        children: [
          { index: true, element: <InicioDuenoPage /> },
          { path: "mascotas", element: <MisMascotasPage /> },
          { path: "mascotas/:mascotaId", element: <FichaMascotaPage /> },
          { path: "agendar", element: <AgendarCitaPage /> },
          { path: "citas", element: <MisCitasPage /> },
        ],
      },
    ],
  },

  // ---------- Veterinario ----------
  {
    path: "/veterinario",
    element: <RequierePerfil tipo="veterinario" />,
    children: [
      {
        element: <LayoutVeterinario />,
        children: [
          { index: true, element: <Navigate to="agenda" replace /> },
          { path: "agenda", element: <AgendaPage /> },
          { path: "consulta/:citaId", element: <RegistrarConsultaPage /> },
          {
            path: "pacientes",
            element: <PaginaPendiente titulo="Pacientes" responsable="Daza" />,
          },
          { path: "pacientes/:mascotaId", element: <FichaPacientePage /> },
        ],
      },
    ],
  },

  { path: "*", element: <Navigate to="/" replace /> },
])
