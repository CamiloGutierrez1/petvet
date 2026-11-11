import { Link } from "react-router-dom"
import { Construction } from "lucide-react"
import { Logo } from "@/components/logo"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoVacio } from "@/components/estado-vacio"

interface PaginaPendienteProps {
  titulo: string
  responsable: "Camilo" | "Daza"
  /**
   * true para las rutas fuera de los layouts (ej. /registro), que no tienen
   * barra superior: muestra el logo y un enlace al selector de perfil.
   */
  independiente?: boolean
}

/** Marcador temporal de las rutas que aún no tienen vista. */
export function PaginaPendiente({
  titulo,
  responsable,
  independiente = false,
}: PaginaPendienteProps) {
  const contenido = (
    <>
      <EncabezadoPagina titulo={titulo} />
      <EstadoVacio
        icono={Construction}
        titulo="Esta vista todavía no está construida."
        descripcion={`Responsable: ${responsable}`}
      />
    </>
  )

  if (!independiente) return contenido

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-xl flex-col gap-8 px-4 py-12">
      <Logo />
      <div>{contenido}</div>
      <Link
        to="/"
        className="text-sm font-medium text-primary underline-offset-4 hover:underline"
      >
        Volver al selector de perfil
      </Link>
    </main>
  )
}
