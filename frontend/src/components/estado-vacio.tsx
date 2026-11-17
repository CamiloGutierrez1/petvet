import type { ReactNode } from "react"
import { Inbox, type LucideIcon } from "lucide-react"
import { cn } from "@/lib/utils"

interface EstadoVacioProps {
  titulo: string
  descripcion?: ReactNode
  icono?: LucideIcon
  /** Botón opcional, ej. "Agendar cita". */
  accion?: ReactNode
  className?: string
}

/** Se muestra cuando una consulta responde con una lista vacía. */
export function EstadoVacio({
  titulo,
  descripcion,
  icono: Icono = Inbox,
  accion,
  className,
}: EstadoVacioProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed px-6 py-12 text-center",
        className,
      )}
    >
      <Icono className="size-8 text-muted-foreground" aria-hidden />
      <p className="font-medium">{titulo}</p>
      {descripcion && (
        <p className="max-w-sm text-sm text-muted-foreground">{descripcion}</p>
      )}
      {accion && <div className="mt-2">{accion}</div>}
    </div>
  )
}
