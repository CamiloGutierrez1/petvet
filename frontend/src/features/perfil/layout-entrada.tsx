import type { ReactNode } from "react"
import {
  Bell,
  CalendarDays,
  ClipboardList,
  Syringe,
  type LucideIcon,
} from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"

/**
 * Marco de las pantallas de entrada y registro (diseño V4): formulario a la
 * izquierda (46 %) y panel de marca verde a la derecha (54 %) con el texto
 * de PetVet, una foto real y tarjetas de ejemplo de lo que hace la app.
 * En celular, el panel se reduce a una franja superior.
 */
export function LayoutEntrada({ children }: { children: ReactNode }) {
  return (
    // Celular: una columna (franja de marca con su altura natural + formulario
    // que ocupa el resto). Escritorio: grilla de dos columnas 46/54.
    <div className="flex min-h-svh flex-col bg-card lg:grid lg:grid-cols-[46fr_54fr]">
      {/* Franja de marca (solo celular y tableta) */}
      <header className="flex flex-col gap-4 bg-primary px-4 pt-6 pb-5 sm:px-8 lg:hidden">
        <Logo invertido />
        <h2 className="text-xl font-bold tracking-tight text-primary-foreground">
          La salud de tus mascotas, en un solo lugar
        </h2>
        {/* Dos columnas que se ajustan al ancho: sin scroll horizontal */}
        <div className="grid grid-cols-2 gap-2 sm:gap-3">
          <TarjetasEjemplo compactas />
        </div>
      </header>

      {/* Columna del formulario */}
      <div className="flex min-w-0 flex-1 flex-col px-4 py-8 sm:px-8 lg:px-16 lg:py-12">
        <Logo className="hidden lg:inline-flex" />
        <main className="flex flex-1 items-center py-8 lg:py-12">
          <div className="mx-auto w-full max-w-[400px]">{children}</div>
        </main>
        <p className="text-xs text-muted-foreground">
          Clínica veterinaria PetVet
        </p>
      </div>

      {/* Panel de marca (escritorio) */}
      <aside className="hidden flex-col justify-center gap-8 bg-primary p-14 lg:flex">
        <div className="flex flex-col gap-3">
          <span className="w-fit rounded-full bg-brand-soft px-3 py-1 text-[13px] font-semibold text-primary">
            Para dueños y veterinarios
          </span>
          <h2 className="max-w-lg text-4xl leading-tight font-bold tracking-tight text-primary-foreground">
            La salud de tus mascotas, en un solo lugar
          </h2>
          <p className="max-w-md text-base text-primary-foreground/85">
            Agenda citas, consulta el historial clínico y recibe recordatorios
            de vacunas.
          </p>
        </div>

        <div className="relative">
          <div className="h-[360px] overflow-hidden rounded-xl">
            <img
              src="/entrada/consulta.jpg"
              alt="Veterinario en consulta con un perro sobre la mesa de examen"
              className="size-full object-cover object-[50%_35%]"
            />
          </div>
          {/* Tarjetas apoyadas sobre el borde inferior de la foto */}
          <div className="absolute inset-x-4 -bottom-8 grid grid-cols-2 gap-3">
            <TarjetasEjemplo />
          </div>
        </div>

        <ul className="mt-6 flex flex-wrap gap-x-6 gap-y-2 text-sm text-primary-foreground/85">
          <Funcion icono={CalendarDays}>Citas en línea</Funcion>
          <Funcion icono={ClipboardList}>Historial clínico</Funcion>
          <Funcion icono={Bell}>Recordatorios por correo</Funcion>
        </ul>
      </aside>
    </div>
  )
}

function Funcion({
  icono: Icono,
  children,
}: {
  icono: LucideIcon
  children: string
}) {
  return (
    <li className="flex items-center gap-2">
      <Icono className="size-4" aria-hidden />
      {children}
    </li>
  )
}

/**
 * Ejemplos de lo que muestra la app (decorativos).
 * compactas: versión de celular, sin ícono ni línea de detalle, para que dos
 * tarjetas quepan lado a lado sin scroll horizontal.
 */
function TarjetasEjemplo({ compactas = false }: { compactas?: boolean }) {
  return (
    <>
      <TarjetaEjemplo
        icono={CalendarDays}
        etiqueta="Próxima cita"
        valor="Vie 20 nov · 9:30 a. m."
        detalle="Luna · Dra. Castaño"
        compacta={compactas}
      />
      <TarjetaEjemplo
        icono={Syringe}
        etiqueta="Próxima vacuna"
        valor="Rabia, 12 dic"
        detalle="Te avisamos 7 días antes"
        alerta
        compacta={compactas}
      />
    </>
  )
}

function TarjetaEjemplo({
  icono: Icono,
  etiqueta,
  valor,
  detalle,
  alerta = false,
  compacta = false,
}: {
  icono: LucideIcon
  etiqueta: string
  valor: string
  detalle: string
  alerta?: boolean
  compacta?: boolean
}) {
  return (
    <div
      aria-hidden
      className={cn(
        "flex min-w-0 items-center gap-3 rounded-xl border bg-card shadow-sm",
        compacta ? "px-3 py-2.5" : "p-3",
      )}
    >
      <span
        className={cn(
          "size-9 shrink-0 items-center justify-center rounded-lg",
          // En la versión compacta el ícono solo aparece si hay espacio (sm+).
          compacta ? "hidden sm:flex" : "flex",
          alerta ? "bg-alerta-soft text-alerta" : "bg-brand-soft text-primary",
        )}
      >
        <Icono className="size-4" />
      </span>
      <span className="flex min-w-0 flex-col">
        <span
          className={cn(
            "text-xs",
            alerta ? "text-alerta-texto" : "text-muted-foreground",
          )}
        >
          {etiqueta}
        </span>
        <span className="truncate text-sm font-semibold">{valor}</span>
        <span
          className={cn(
            "truncate text-xs text-muted-foreground",
            compacta && "hidden sm:block",
          )}
        >
          {detalle}
        </span>
      </span>
    </div>
  )
}
