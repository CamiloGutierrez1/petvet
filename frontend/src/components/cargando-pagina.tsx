import { Skeleton } from "@/components/ui/skeleton"

/**
 * Se muestra mientras se descarga el código de una pantalla (las rutas se
 * cargan bajo demanda con React.lazy).
 */
export function CargandoPagina() {
  return (
    <div aria-busy="true" aria-label="Cargando" className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-8 w-56" />
        <Skeleton className="h-4 w-72" />
      </div>
      <Skeleton className="h-64 rounded-xl" />
    </div>
  )
}
