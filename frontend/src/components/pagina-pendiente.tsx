import { Link } from "react-router-dom"

interface PaginaPendienteProps {
  titulo: string
  responsable: "Camilo" | "Daza"
}

/** Marcador temporal de las rutas que aún no tienen vista. */
export function PaginaPendiente({ titulo, responsable }: PaginaPendienteProps) {
  return (
    <main className="mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-2 p-8">
      <p className="text-sm text-muted-foreground">Vista pendiente</p>
      <h1 className="text-2xl font-semibold">{titulo}</h1>
      <p className="text-muted-foreground">Responsable: {responsable}</p>
      <Link to="/" className="mt-4 text-sm text-primary underline">
        Volver al inicio
      </Link>
    </main>
  )
}
