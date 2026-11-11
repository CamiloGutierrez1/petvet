import { Badge } from "@/components/ui/badge"
import type { EstadoCita } from "@/api/types"
import { cn } from "@/lib/utils"

const ESTILOS: Record<EstadoCita, { etiqueta: string; clase: string }> = {
  AGENDADA: {
    etiqueta: "Agendada",
    clase: "bg-estado-agendada-soft text-estado-agendada",
  },
  COMPLETADA: {
    etiqueta: "Completada",
    clase: "bg-estado-completada-soft text-estado-completada",
  },
  CANCELADA: {
    etiqueta: "Cancelada",
    clase: "bg-estado-cancelada-soft text-estado-cancelada",
  },
}

interface EstadoCitaBadgeProps {
  estado: EstadoCita
  className?: string
}

/**
 * Etiqueta de color con el estado de una cita. Todas las vistas (dueño y
 * veterinario) usan este componente para que los colores sean los mismos.
 * Los colores son tokens en src/index.css (--estado-*).
 */
export function EstadoCitaBadge({ estado, className }: EstadoCitaBadgeProps) {
  const { etiqueta, clase } = ESTILOS[estado]
  return <Badge className={cn(clase, className)}>{etiqueta}</Badge>
}
