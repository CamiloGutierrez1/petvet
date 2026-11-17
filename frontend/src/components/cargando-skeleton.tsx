import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

interface CargandoSkeletonProps {
  /** tabla: filas con columnas · tarjetas: cuadrícula · lista: renglones (ej. historial) */
  variante?: "tabla" | "tarjetas" | "lista"
  cantidad?: number
  columnas?: number
  className?: string
}

/** Placeholder mientras carga una consulta. Nunca dejar la pantalla en blanco. */
export function CargandoSkeleton({
  variante = "tabla",
  cantidad = 5,
  columnas = 4,
  className,
}: CargandoSkeletonProps) {
  const items = Array.from({ length: cantidad }, (_, i) => i)

  if (variante === "tarjetas") {
    return (
      <div
        aria-busy="true"
        className={cn("grid gap-4 sm:grid-cols-2 lg:grid-cols-3", className)}
      >
        {items.map((i) => (
          <div key={i} className="flex flex-col gap-3 rounded-lg border p-4">
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-4 w-1/3" />
          </div>
        ))}
      </div>
    )
  }

  if (variante === "lista") {
    return (
      <div aria-busy="true" className={cn("flex flex-col gap-4", className)}>
        {items.map((i) => (
          <div key={i} className="flex gap-4">
            <Skeleton className="size-3 shrink-0 rounded-full" />
            <div className="flex flex-1 flex-col gap-2">
              <Skeleton className="h-4 w-1/4" />
              <Skeleton className="h-4 w-3/4" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div aria-busy="true" className={cn("flex flex-col gap-3", className)}>
      {items.map((i) => (
        <div key={i} className="flex gap-4">
          {Array.from({ length: columnas }, (_, c) => (
            <Skeleton key={c} className="h-8 flex-1" />
          ))}
        </div>
      ))}
    </div>
  )
}
