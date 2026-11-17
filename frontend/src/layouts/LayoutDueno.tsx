import { Suspense } from "react"
import { Link, Outlet } from "react-router-dom"
import { CalendarPlus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { CargandoPagina } from "@/components/cargando-pagina"
import { BarraSuperior, type EnlaceNav } from "./barra-superior"

const ENLACES: EnlaceNav[] = [
  { to: "/dueno", etiqueta: "Inicio", exacto: true },
  { to: "/dueno/mascotas", etiqueta: "Mis mascotas" },
  { to: "/dueno/citas", etiqueta: "Mis citas" },
]

/** Marco de todas las vistas del dueño: barra superior + contenido centrado. */
export function LayoutDueno() {
  return (
    <div className="min-h-svh">
      <BarraSuperior
        inicio="/dueno"
        enlaces={ENLACES}
        accion={
          <Button asChild className="w-full sm:w-auto">
            <Link to="/dueno/agendar">
              <CalendarPlus />
              Agendar cita
            </Link>
          </Button>
        }
      />
      <main className="mx-auto w-full max-w-[1200px] px-4 py-8 sm:px-8">
        <Suspense fallback={<CargandoPagina />}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  )
}
