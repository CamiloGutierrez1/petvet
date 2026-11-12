import { Suspense } from "react"
import { Outlet } from "react-router-dom"
import { CargandoPagina } from "@/components/cargando-pagina"
import { BarraSuperior, type EnlaceNav } from "./barra-superior"

const ENLACES: EnlaceNav[] = [
  { to: "/veterinario/agenda", etiqueta: "Agenda" },
  { to: "/veterinario/pacientes", etiqueta: "Pacientes" },
]

/** Marco de todas las vistas del veterinario: barra superior + contenido centrado. */
export function LayoutVeterinario() {
  return (
    <div className="min-h-svh">
      <BarraSuperior
        inicio="/veterinario/agenda"
        enlaces={ENLACES}
        etiqueta="Clínica"
      />
      <main className="mx-auto w-full max-w-[1200px] px-4 py-8 sm:px-8">
        <Suspense fallback={<CargandoPagina />}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  )
}
