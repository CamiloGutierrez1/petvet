import { useState, type ReactNode } from "react"
import { Link, useNavigate } from "react-router-dom"
import { ChevronRight, Stethoscope, UserRound } from "lucide-react"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { CargandoSkeleton } from "@/components/cargando-skeleton"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { iniciales, usePerfil, type TipoPerfil } from "./perfil-context"
import { useDuenos, useVeterinarios } from "./use-opciones-perfil"

interface Opcion {
  id: number
  nombre: string
  detalle?: string
}

const DESTINO: Record<TipoPerfil, string> = {
  dueno: "/dueno",
  veterinario: "/veterinario/agenda",
}

/**
 * Ruta "/". Sin login real: se elige si se entra como dueño o como
 * veterinario, y con qué persona. La elección queda guardada.
 */
export function SelectorPerfilPage() {
  const { perfil, elegirPerfil } = usePerfil()
  const navigate = useNavigate()
  const [tipo, setTipo] = useState<TipoPerfil>(perfil?.tipo ?? "dueno")

  const duenos = useDuenos()
  const veterinarios = useVeterinarios()
  const consulta = tipo === "dueno" ? duenos : veterinarios

  const opciones: Opcion[] | undefined =
    tipo === "dueno"
      ? duenos.data?.map((d) => ({
          id: d.id,
          nombre: d.nombre,
          detalle: d.email,
        }))
      : veterinarios.data?.map((v) => ({
          id: v.id,
          nombre: v.nombre,
          detalle: v.especialidad,
        }))

  function entrar(opcion: Opcion) {
    elegirPerfil({ tipo, id: opcion.id, nombre: opcion.nombre })
    navigate(DESTINO[tipo])
  }

  return (
    <main className="flex min-h-svh flex-col items-center px-4 py-12 sm:py-20">
      <div className="flex w-full max-w-md flex-col gap-8">
        <Logo className="self-center" />

        <div className="flex flex-col gap-1 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">
            Elige tu perfil
          </h1>
          <p className="text-sm text-muted-foreground">
            Entra como dueño para ver tus mascotas y citas, o como veterinario
            para ver la agenda de la clínica.
          </p>
        </div>

        <div
          role="tablist"
          aria-label="Tipo de perfil"
          className="grid grid-cols-2 gap-1 rounded-lg border bg-card p-1"
        >
          <BotonTipo
            activo={tipo === "dueno"}
            onClick={() => setTipo("dueno")}
            icono={<UserRound />}
          >
            Dueño
          </BotonTipo>
          <BotonTipo
            activo={tipo === "veterinario"}
            onClick={() => setTipo("veterinario")}
            icono={<Stethoscope />}
          >
            Veterinario
          </BotonTipo>
        </div>

        <section role="tabpanel" aria-label="Personas">
          {consulta.isLoading ? (
            <CargandoSkeleton variante="lista" cantidad={3} />
          ) : consulta.isError ? (
            <EstadoError
              mensaje="No se pudo cargar la lista de perfiles."
              alReintentar={() => consulta.refetch()}
            />
          ) : !opciones || opciones.length === 0 ? (
            <EstadoVacio
              titulo={
                tipo === "dueno"
                  ? "Todavía no hay dueños registrados."
                  : "No hay veterinarios registrados."
              }
            />
          ) : (
            <ul className="divide-y overflow-hidden rounded-xl border bg-card">
              {opciones.map((opcion) => {
                const esActual =
                  perfil?.tipo === tipo && perfil.id === opcion.id
                return (
                  <li key={opcion.id}>
                    <button
                      type="button"
                      onClick={() => entrar(opcion)}
                      className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-accent focus-visible:bg-accent focus-visible:outline-none"
                    >
                      <Avatar>
                        <AvatarFallback className="bg-brand-soft font-semibold text-primary">
                          {iniciales(opcion.nombre)}
                        </AvatarFallback>
                      </Avatar>
                      <span className="flex min-w-0 flex-1 flex-col">
                        <span className="truncate font-medium">
                          {opcion.nombre}
                        </span>
                        {opcion.detalle && (
                          <span className="truncate text-sm text-muted-foreground">
                            {opcion.detalle}
                          </span>
                        )}
                      </span>
                      {esActual && (
                        <span className="text-xs font-medium text-primary">
                          Perfil actual
                        </span>
                      )}
                      <ChevronRight
                        className="size-4 text-muted-foreground"
                        aria-hidden
                      />
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </section>

        {tipo === "dueno" && (
          <p className="text-center text-sm text-muted-foreground">
            ¿Es tu primera vez?{" "}
            <Link
              to="/registro"
              className="font-medium text-primary underline-offset-4 hover:underline"
            >
              Regístrate como dueño
            </Link>
          </p>
        )}
      </div>
    </main>
  )
}

interface BotonTipoProps {
  activo: boolean
  onClick: () => void
  icono: ReactNode
  children: ReactNode
}

function BotonTipo({ activo, onClick, icono, children }: BotonTipoProps) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={activo}
      onClick={onClick}
      className={cn(
        "flex h-9 items-center justify-center gap-2 rounded-md text-sm font-medium transition-colors [&_svg]:size-4",
        activo
          ? "bg-brand-soft text-primary"
          : "text-muted-foreground hover:text-foreground",
      )}
    >
      {icono}
      {children}
    </button>
  )
}
