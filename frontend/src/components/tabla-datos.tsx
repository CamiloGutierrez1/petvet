import type { Key, ReactNode } from "react"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { EstadoVacio } from "@/components/estado-vacio"
import { cn } from "@/lib/utils"

export interface ColumnaTabla<T> {
  id: string
  encabezado: ReactNode
  celda: (fila: T) => ReactNode
  className?: string
}

interface TablaDatosProps<T> {
  columnas: ColumnaTabla<T>[]
  filas: T[] | undefined
  obtenerClave: (fila: T) => Key
  cargando?: boolean
  /** Texto cuando no hay filas. */
  mensajeVacio?: string
  /** Si se pasa, la fila completa es clicable (ej. abrir la cita). */
  alHacerClicFila?: (fila: T) => void
}

/**
 * Tabla genérica sobre la Table de shadcn. Cada columna dice cómo pintar
 * su celda, así la misma tabla sirve para agenda, mascotas o citas.
 */
export function TablaDatos<T>({
  columnas,
  filas,
  obtenerClave,
  cargando = false,
  mensajeVacio = "No hay datos para mostrar.",
  alHacerClicFila,
}: TablaDatosProps<T>) {
  if (!cargando && (!filas || filas.length === 0)) {
    return <EstadoVacio titulo={mensajeVacio} />
  }

  return (
    <div className="rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            {columnas.map((col) => (
              <TableHead key={col.id} className={cn("px-4", col.className)}>
                {col.encabezado}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {cargando
            ? Array.from({ length: 5 }, (_, i) => (
                <TableRow key={i}>
                  {columnas.map((col) => (
                    <TableCell key={col.id} className="px-4">
                      <Skeleton className="h-5 w-full" />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            : filas!.map((fila) => (
                <TableRow
                  key={obtenerClave(fila)}
                  onClick={
                    alHacerClicFila ? () => alHacerClicFila(fila) : undefined
                  }
                  className={cn(alHacerClicFila && "cursor-pointer")}
                >
                  {columnas.map((col) => (
                    <TableCell
                      key={col.id}
                      className={cn("px-4", col.className)}
                    >
                      {col.celda(fila)}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
        </TableBody>
      </Table>
    </div>
  )
}
