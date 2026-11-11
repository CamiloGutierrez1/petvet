import { CircleAlert, RotateCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface EstadoErrorProps {
  mensaje?: string
  /** Normalmente el `refetch` de useQuery. */
  alReintentar?: () => void
  className?: string
}

/** Se muestra cuando una consulta falla, con botón "Reintentar". */
export function EstadoError({
  mensaje = "No se pudieron cargar los datos.",
  alReintentar,
  className,
}: EstadoErrorProps) {
  return (
    <div
      role="alert"
      className={cn(
        "flex flex-col items-center justify-center gap-2 rounded-lg border border-destructive/30 bg-destructive/5 px-6 py-12 text-center",
        className,
      )}
    >
      <CircleAlert className="size-8 text-destructive" aria-hidden />
      <p className="font-medium">{mensaje}</p>
      {alReintentar && (
        <Button variant="outline" className="mt-2" onClick={alReintentar}>
          <RotateCw />
          Reintentar
        </Button>
      )}
    </div>
  )
}
