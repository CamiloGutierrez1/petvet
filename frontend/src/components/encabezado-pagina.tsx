import type { ReactNode } from "react"
import { Link } from "react-router-dom"
import { ArrowLeft } from "lucide-react"

interface EncabezadoPaginaProps {
  titulo: string
  descripcion?: ReactNode
  /** Botones a la derecha del título (ej. "Agendar cita"). */
  acciones?: ReactNode
  /** Si se pasa, muestra un enlace "Volver" a esa ruta. */
  volverA?: string
}

/** Título de cada vista, con descripción, acciones y enlace de volver opcionales. */
export function EncabezadoPagina({
  titulo,
  descripcion,
  acciones,
  volverA,
}: EncabezadoPaginaProps) {
  return (
    <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="flex flex-col gap-1">
        {volverA && (
          <Link
            to={volverA}
            className="mb-2 inline-flex w-fit items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-4" />
            Volver
          </Link>
        )}
        <h1 className="font-heading text-2xl font-semibold tracking-tight">
          {titulo}
        </h1>
        {descripcion && (
          <p className="text-sm text-muted-foreground">{descripcion}</p>
        )}
      </div>
      {acciones && <div className="flex items-center gap-2">{acciones}</div>}
    </header>
  )
}
