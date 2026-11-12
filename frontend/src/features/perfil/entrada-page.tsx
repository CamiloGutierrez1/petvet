import { useState, type ReactNode } from "react"
import { Link, useNavigate, useSearchParams } from "react-router-dom"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useQueryClient } from "@tanstack/react-query"
import {
  ArrowLeft,
  ChevronRight,
  CircleAlert,
  LoaderCircle,
  PawPrint,
  Stethoscope,
  type LucideIcon,
} from "lucide-react"
import { getDuenos } from "@/api/mascotas"
import { mensajeDeError } from "@/api/errores"
import { CargandoSkeleton } from "@/components/cargando-skeleton"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { LayoutEntrada } from "./layout-entrada"
import { iniciales, usePerfil, type TipoPerfil } from "./perfil-context"
import { useVeterinarios } from "./use-opciones-perfil"

type Paso = "rol" | "dueno" | "veterinario"

const DESTINO: Record<TipoPerfil, string> = {
  dueno: "/dueno",
  veterinario: "/veterinario/agenda",
}
const ROL: Record<TipoPerfil, string> = {
  dueno: "Dueño",
  veterinario: "Veterinario",
}

/**
 * Ruta "/": entrada a PetVet. Sin login real (decisión del proyecto): el
 * dueño se identifica con su correo y el veterinario eligiéndose de la
 * lista. El perfil queda guardado con usePerfil(); un login real solo
 * cambiaría esta pantalla.
 */
export function EntradaPage() {
  const [params] = useSearchParams()
  const inicial = params.get("paso")
  const [paso, setPaso] = useState<Paso>(
    inicial === "dueno" || inicial === "veterinario" ? inicial : "rol",
  )

  return (
    <LayoutEntrada>
      {paso === "rol" && <PasoRol onElegir={setPaso} />}
      {paso === "dueno" && <PasoDueno onVolver={() => setPaso("rol")} />}
      {paso === "veterinario" && (
        <PasoVeterinario onVolver={() => setPaso("rol")} />
      )}
    </LayoutEntrada>
  )
}

// ---------- Paso 1: tipo de perfil ----------

function PasoRol({ onElegir }: { onElegir: (paso: Paso) => void }) {
  const { perfil } = usePerfil()
  const navigate = useNavigate()

  return (
    <div className="flex flex-col gap-6">
      {perfil && (
        <button
          type="button"
          onClick={() => navigate(DESTINO[perfil.tipo])}
          className="flex items-center gap-3 rounded-xl border border-primary/30 bg-brand-soft/60 p-3 text-left transition-colors hover:border-primary/60"
        >
          <Avatar>
            <AvatarFallback className="bg-card font-semibold text-primary">
              {iniciales(perfil.nombre)}
            </AvatarFallback>
          </Avatar>
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="text-xs text-muted-foreground">
              Continuar como
            </span>
            <span className="truncate font-medium">
              {perfil.nombre} · {ROL[perfil.tipo]}
            </span>
          </span>
          <ChevronRight className="size-4 text-primary" aria-hidden />
        </button>
      )}

      <Titulo
        titulo="¿Cómo quieres entrar?"
        apoyo="Elige tu perfil para continuar."
      />

      <div className="flex flex-col gap-3">
        <OpcionRol
          icono={PawPrint}
          titulo="Dueño"
          descripcion="Consulta a tus mascotas, agenda citas y revisa sus vacunas."
          onClick={() => onElegir("dueno")}
        />
        <OpcionRol
          icono={Stethoscope}
          titulo="Veterinario"
          descripcion="Revisa tu agenda del día y registra las consultas."
          onClick={() => onElegir("veterinario")}
        />
      </div>

      <p className="text-sm text-muted-foreground">
        ¿Es tu primera vez?{" "}
        <Link
          to="/registro"
          className="font-medium text-primary underline-offset-4 hover:underline"
        >
          Regístrate como dueño
        </Link>
      </p>
    </div>
  )
}

function OpcionRol({
  icono: Icono,
  titulo,
  descripcion,
  onClick,
}: {
  icono: LucideIcon
  titulo: string
  descripcion: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-4 rounded-xl border bg-card p-4 text-left transition-colors hover:border-primary/40 hover:bg-brand-soft/40 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-soft text-primary">
        <Icono className="size-5" aria-hidden />
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="font-semibold">{titulo}</span>
        <span className="text-sm text-muted-foreground">{descripcion}</span>
      </span>
      <ChevronRight className="size-4 text-muted-foreground" aria-hidden />
    </button>
  )
}

// ---------- Paso 2a: dueño con su correo ----------

const esquemaCorreo = z.object({
  email: z
    .string()
    .trim()
    .email("Escribe un correo válido, ej. ana@correo.com."),
})

function PasoDueno({ onVolver }: { onVolver: () => void }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { elegirPerfil } = usePerfil()
  const [buscando, setBuscando] = useState(false)
  const [noEncontrado, setNoEncontrado] = useState<string | null>(null)
  const [errorCarga, setErrorCarga] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.infer<typeof esquemaCorreo>>({
    resolver: zodResolver(esquemaCorreo),
    defaultValues: { email: "" },
  })

  async function entrar({ email }: z.infer<typeof esquemaCorreo>) {
    setBuscando(true)
    setNoEncontrado(null)
    setErrorCarga(null)
    try {
      // Lista actualizada de dueños (incluye a quien se acaba de registrar).
      const duenos = await queryClient.fetchQuery({
        queryKey: ["duenos"],
        queryFn: getDuenos,
        staleTime: 0,
      })
      const dueno = duenos.find(
        (d) => d.email.toLowerCase() === email.toLowerCase(),
      )
      if (!dueno) {
        setNoEncontrado(email)
        return
      }
      elegirPerfil({ tipo: "dueno", id: dueno.id, nombre: dueno.nombre })
      navigate(DESTINO.dueno)
    } catch (error) {
      setErrorCarga(mensajeDeError(error, "No se pudo verificar el correo."))
    } finally {
      setBuscando(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <BotonVolver onClick={onVolver} />
      <Titulo
        titulo="Entra con tu correo"
        apoyo="Escribe el correo con el que te registraste."
      />

      <form
        noValidate
        onSubmit={handleSubmit(entrar)}
        className="flex flex-col gap-4"
      >
        <Field data-invalid={!!errors.email}>
          <FieldLabel htmlFor="email-entrada">Correo electrónico</FieldLabel>
          <Input
            id="email-entrada"
            type="email"
            autoComplete="email"
            autoFocus
            placeholder="laura@correo.com"
            aria-invalid={!!errors.email}
            {...register("email")}
          />
          <FieldError errors={[errors.email]} />
        </Field>

        {noEncontrado && (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/5 p-3 text-sm text-destructive"
          >
            <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
            <span>
              No encontramos una cuenta con ese correo.{" "}
              <Link
                to={`/registro?email=${encodeURIComponent(noEncontrado)}`}
                className="font-medium underline underline-offset-4"
              >
                Regístrate como dueño
              </Link>
            </span>
          </div>
        )}
        {errorCarga && (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/5 p-3 text-sm text-destructive"
          >
            <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
            {errorCarga}
          </div>
        )}

        <Button type="submit" size="lg" disabled={buscando}>
          {buscando && <LoaderCircle className="animate-spin" />}
          {buscando ? "Entrando…" : "Entrar"}
        </Button>
      </form>

      <p className="text-sm text-muted-foreground">
        ¿Es tu primera vez?{" "}
        <Link
          to="/registro"
          className="font-medium text-primary underline-offset-4 hover:underline"
        >
          Regístrate
        </Link>
      </p>
    </div>
  )
}

// ---------- Paso 2b: veterinario de la lista ----------

function PasoVeterinario({ onVolver }: { onVolver: () => void }) {
  const navigate = useNavigate()
  const { perfil, elegirPerfil } = usePerfil()
  const { data, isLoading, isError, error, refetch } = useVeterinarios()

  return (
    <div className="flex flex-col gap-6">
      <BotonVolver onClick={onVolver} />
      <Titulo
        titulo="¿Quién eres?"
        apoyo="Elige tu nombre para ver tu agenda."
      />

      {isLoading ? (
        <CargandoSkeleton variante="lista" cantidad={3} />
      ) : isError ? (
        <EstadoError
          mensaje={mensajeDeError(
            error,
            "No se pudo cargar la lista de veterinarios.",
          )}
          alReintentar={() => refetch()}
          className="py-8"
        />
      ) : !data?.length ? (
        <EstadoVacio
          titulo="No hay veterinarios registrados."
          className="py-8"
        />
      ) : (
        <ul className="divide-y overflow-hidden rounded-xl border bg-card">
          {data.map((v) => {
            const esActual =
              perfil?.tipo === "veterinario" && perfil.id === v.id
            return (
              <li key={v.id}>
                <button
                  type="button"
                  onClick={() => {
                    elegirPerfil({
                      tipo: "veterinario",
                      id: v.id,
                      nombre: v.nombre,
                    })
                    navigate(DESTINO.veterinario)
                  }}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-accent focus-visible:bg-accent focus-visible:outline-none"
                >
                  <Avatar>
                    <AvatarFallback className="bg-brand-soft font-semibold text-primary">
                      {iniciales(v.nombre)}
                    </AvatarFallback>
                  </Avatar>
                  <span className="flex min-w-0 flex-1 flex-col">
                    <span className="truncate font-medium">{v.nombre}</span>
                    <span className="truncate text-sm text-muted-foreground">
                      {v.especialidad}
                    </span>
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
    </div>
  )
}

// ---------- Piezas comunes ----------

function Titulo({ titulo, apoyo }: { titulo: string; apoyo: ReactNode }) {
  return (
    <div className="flex flex-col gap-1">
      <h1 className="text-2xl font-semibold tracking-tight">{titulo}</h1>
      <p className="text-sm text-muted-foreground">{apoyo}</p>
    </div>
  )
}

function BotonVolver({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="inline-flex w-fit items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
    >
      <ArrowLeft className="size-4" aria-hidden />
      Cambiar tipo de perfil
    </button>
  )
}
