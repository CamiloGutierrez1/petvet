import { createBrowserRouter, Navigate } from "react-router-dom"
import { RequierePerfil } from "@/features/perfil/requiere-perfil"
import { EntradaPage } from "@/features/perfil/entrada-page"
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
  PacientesPage,
  RegistrarConsultaPage,
  RegistroDuenoPage,
} from "./paginas"

// Rutas de la aplicación. Las pantallas se cargan bajo demanda (./paginas);
// la entrada se carga de inmediato porque es la primera.
// Cada grupo: RequierePerfil (exige ese perfil) → Layout (barra superior) → vista.
export const router = createBrowserRouter([
  { path: "/", element: <EntradaPage /> },
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
          { path: "pacientes", element: <PacientesPage /> },
          { path: "pacientes/:mascotaId", element: <FichaPacientePage /> },
        ],
      },
    ],
  },

  { path: "*", element: <Navigate to="/" replace /> },
])
