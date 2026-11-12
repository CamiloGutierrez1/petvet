import type { ReactNode } from "react"
import { Link } from "react-router-dom"
import { parseISO } from "date-fns"
import {
  ArrowRight,
  CalendarDays,
  CalendarPlus,
  PawPrint,
  Syringe,
  type LucideIcon,
} from "lucide-react"
import { ahoraIsoLocal } from "@/api/http"
import { mensajeDeError } from "@/api/errores"
import type { Cita, Mascota } from "@/api/types"
import { EstadoError } from "@/components/estado-error"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useCitasDeMascotas } from "@/features/citas/use-mis-citas"
import { usePerfil } from "@/features/perfil/perfil-context"
import { useVeterinarios } from "@/features/perfil/use-opciones-perfil"
import {
  etiquetaServicio,
  formatearFechaCorta,
  formatearHora,
  textoPlazo,
} from "@/lib/formato"
import { cn } from "@/lib/utils"
import { FotoMascota } from "./foto-mascota"
import { edadTexto } from "./mascota-utils"
import { useMascotasDeDueno } from "./use-mascotas"
import { useVacunasDeMascotas } from "./use-vacunas-de-mascotas"

const HOY = new Intl.DateTimeFormat("es-CO", {
  weekday: "long",
  day: "numeric",
  month: "long",
})
const MES = new Intl.DateTimeFormat("es-CO", { month: "short" })
const DIA_SEMANA = new Intl.DateTimeFormat("es-CO", { weekday: "long" })

/** Ruta "/dueno": resumen del dueño con accesos a cada sección. */
export function InicioDuenoPage() {
  const { perfil } = usePerfil()
  const mascotas = useMascotasDeDueno(perfil!.id)
  const citas = useCitasDeMascotas(mascotas.data)
  const vacunas = useVacunasDeMascotas(mascotas.data)
  const { data: veterinarios } = useVeterinarios()

  const ahora = ahoraIsoLocal()
  const proximas = citas.citas
    .filter((c) => c.estado === "AGENDADA" && c.fechaHora >= ahora)
    .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora))

  const mascotaDe = (cita: Cita) =>
    mascotas.data?.find((m) => m.id === cita.mascotaId)
  const veterinarioDe = (cita: Cita) =>
    veterinarios?.find((v) => v.id === cita.veterinarioId)?.nombre ?? "—"

  const primerNombre = perfil!.nombre.split(" ")[0]
  const cargandoMascotas = mascotas.isLoading
  const sinMascotas = !cargandoMascotas && mascotas.data?.length === 0

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="flex flex-col gap-1">
          <p className="text-sm text-muted-foreground first-letter:uppercase">
            {HOY.format(new Date())}
          </p>
          <h1 className="text-2xl font-semibold tracking-tight">
            Hola, {primerNombre}
          </h1>
        </div>
      </header>

      {/* Cifras rápidas */}
      <div className="grid gap-4 sm:grid-cols-3">
        <Cifra
          icono={PawPrint}
          etiqueta="Mascotas"
          valor={mascotas.data?.length}
          cargando={cargandoMascotas}
          to="/dueno/mascotas"
        />
        <Cifra
          icono={CalendarDays}
          etiqueta="Citas próximas"
          valor={proximas.length}
          cargando={cargandoMascotas || citas.isLoading}
          to="/dueno/citas"
        />
        <Cifra
          icono={Syringe}
          etiqueta="Vacunas pendientes"
          valor={vacunas.vacunas.length}
          cargando={cargandoMascotas || vacunas.isLoading}
          to="#vacunas"
          alerta={vacunas.vacunas.length > 0}
        />
      </div>

      {sinMascotas ? (
        <section className="flex flex-col items-start gap-3 rounded-xl border bg-card p-6">
          <h2 className="text-lg font-semibold">
            Empieza registrando a tu mascota
          </h2>
          <p className="max-w-prose text-sm text-muted-foreground">
            Con sus datos podrás agendar citas y la clínica calculará sus
            próximas vacunas.
          </p>
          <Button asChild>
            <Link to="/dueno/mascotas">
              <PawPrint />
              Registrar mascota
            </Link>
          </Button>
        </section>
      ) : (
        <>
          <div className="grid gap-6 lg:grid-cols-2">
            {/* Próximas citas */}
            <Seccion titulo="Próximas citas" verTodas="/dueno/citas">
              {cargandoMascotas || citas.isLoading ? (
                <FilasCargando />
              ) : citas.error ? (
                <EstadoError
                  mensaje={mensajeDeError(
                    citas.error,
                    "No se pudieron cargar las citas.",
                  )}
                  alReintentar={citas.refetch}
                  className="py-8"
                />
              ) : proximas.length === 0 ? (
                <div className="flex flex-col items-start gap-3">
                  <p className="text-sm text-muted-foreground">
                    No tienes citas próximas.
                  </p>
                  <Button asChild variant="outline">
                    <Link to="/dueno/agendar">
                      <CalendarPlus />
                      Agendar cita
                    </Link>
                  </Button>
                </div>
              ) : (
                <ul className="flex flex-col gap-3">
                  {proximas.slice(0, 3).map((cita, i) => (
                    <li key={cita.id}>
                      <CitaCompacta
                        cita={cita}
                        mascota={mascotaDe(cita)}
                        veterinario={veterinarioDe(cita)}
                        destacada={i === 0}
                      />
                    </li>
                  ))}
                </ul>
              )}
            </Seccion>

            {/* Vacunas pendientes */}
            <Seccion id="vacunas" titulo="Vacunas pendientes">
              {cargandoMascotas || vacunas.isLoading ? (
                <FilasCargando />
              ) : vacunas.error ? (
                <EstadoError
                  mensaje={mensajeDeError(
                    vacunas.error,
                    "No se pudieron cargar las vacunas.",
                  )}
                  alReintentar={vacunas.refetch}
                  className="py-8"
                />
              ) : vacunas.vacunas.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Tus mascotas no tienen vacunas pendientes.
                </p>
              ) : (
                <ul className="flex flex-col gap-2">
                  {vacunas.vacunas.slice(0, 4).map((v) => (
                    <li key={`${v.mascota.id}-${v.vacuna}-${v.fechaLimite}`}>
                      <Link
                        to={`/dueno/mascotas/${v.mascota.id}`}
                        className="flex items-center gap-3 rounded-lg bg-alerta-soft p-3 transition-opacity hover:opacity-90"
                      >
                        <Syringe
                          className="size-5 shrink-0 text-alerta"
                          aria-hidden
                        />
                        <span className="flex min-w-0 flex-1 flex-col">
                          <span className="truncate font-medium text-alerta-texto">
                            {v.mascota.nombre} · {v.vacuna},{" "}
                            {formatearFechaCorta(v.fechaLimite)}
                          </span>
                          <span className="text-sm text-alerta-texto/80">
                            {textoPlazo(v.fechaLimite)}
                          </span>
                        </span>
                        <ArrowRight
                          className="size-4 text-alerta-texto/70"
                          aria-hidden
                        />
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </Seccion>
          </div>

          {/* Mis mascotas */}
          <Seccion titulo="Mis mascotas" verTodas="/dueno/mascotas">
            {cargandoMascotas ? (
              <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                {Array.from({ length: 4 }, (_, i) => (
                  <Skeleton key={i} className="h-40 rounded-xl" />
                ))}
              </div>
            ) : mascotas.error ? (
              <EstadoError
                mensaje={mensajeDeError(
                  mascotas.error,
                  "No se pudieron cargar tus mascotas.",
                )}
                alReintentar={() => mascotas.refetch()}
                className="py-8"
              />
            ) : (
              <ul className="grid grid-cols-2 gap-4 md:grid-cols-4">
                {mascotas.data?.slice(0, 4).map((m) => (
                  <li key={m.id}>
                    <Link
                      to={`/dueno/mascotas/${m.id}`}
                      className="block overflow-hidden rounded-xl border bg-card transition-colors hover:border-primary/40"
                    >
                      <FotoMascota
                        nombre={m.nombre}
                        especie={m.especie}
                        fotoUrl={m.fotoUrl}
                      />
                      <div className="p-3">
                        <p className="truncate font-medium">{m.nombre}</p>
                        <p className="text-xs text-muted-foreground">
                          {edadTexto(m.fechaNacimiento)}
                        </p>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Seccion>
        </>
      )}
    </div>
  )
}

function Cifra({
  icono: Icono,
  etiqueta,
  valor,
  cargando,
  to,
  alerta = false,
}: {
  icono: LucideIcon
  etiqueta: string
  valor?: number
  cargando: boolean
  to: string
  alerta?: boolean
}) {
  const contenido = (
    <>
      <span
        className={cn(
          "flex size-10 items-center justify-center rounded-lg",
          alerta ? "bg-alerta-soft text-alerta" : "bg-brand-soft text-primary",
        )}
      >
        <Icono className="size-5" aria-hidden />
      </span>
      <span className="flex flex-col">
        {cargando ? (
          <Skeleton className="h-7 w-8" />
        ) : (
          <span className="text-2xl leading-tight font-semibold">
            {valor ?? 0}
          </span>
        )}
        <span className="text-sm text-muted-foreground">{etiqueta}</span>
      </span>
    </>
  )
  const clases =
    "flex items-center gap-4 rounded-xl border bg-card p-4 transition-colors hover:border-primary/40"
  return to.startsWith("#") ? (
    <a href={to} className={clases}>
      {contenido}
    </a>
  ) : (
    <Link to={to} className={clases}>
      {contenido}
    </Link>
  )
}

function Seccion({
  id,
  titulo,
  verTodas,
  children,
}: {
  id?: string
  titulo: string
  verTodas?: string
  children: ReactNode
}) {
  return (
    <section id={id} className="scroll-mt-24 rounded-xl border bg-card p-5">
      <div className="mb-4 flex items-center justify-between gap-4">
        <h2 className="font-semibold">{titulo}</h2>
        {verTodas && (
          <Link
            to={verTodas}
            className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline"
          >
            Ver todas
            <ArrowRight className="size-4" aria-hidden />
          </Link>
        )}
      </div>
      {children}
    </section>
  )
}

function CitaCompacta({
  cita,
  mascota,
  veterinario,
  destacada,
}: {
  cita: Cita
  mascota?: Mascota
  veterinario: string
  destacada: boolean
}) {
  const fecha = parseISO(cita.fechaHora)
  return (
    <div
      className={cn(
        "flex items-center gap-4 rounded-lg p-3",
        destacada ? "border border-primary/30 bg-brand-soft/60" : "border",
      )}
    >
      <div className="flex size-12 shrink-0 flex-col items-center justify-center rounded-lg bg-card text-primary shadow-xs">
        <span className="text-lg leading-none font-bold">
          {fecha.getDate()}
        </span>
        <span className="text-[0.7rem] font-medium uppercase">
          {MES.format(fecha).replace(".", "")}
        </span>
      </div>
      <div className="flex min-w-0 flex-1 flex-col text-sm">
        <span className="truncate font-medium">
          {mascota?.nombre ?? "Mascota"} · {etiquetaServicio(cita.tipoServicio)}
        </span>
        <span className="truncate text-muted-foreground">
          <span className="capitalize">{DIA_SEMANA.format(fecha)}</span>,{" "}
          {formatearHora(cita.fechaHora)} · {veterinario}
        </span>
      </div>
      {/* Todas las de esta sección están agendadas; la etiqueta solo marca
          cuál es la siguiente. */}
      {destacada && (
        <span className="hidden rounded-full bg-primary px-2.5 py-0.5 text-xs font-medium text-primary-foreground sm:inline-flex">
          Siguiente
        </span>
      )}
    </div>
  )
}

function FilasCargando() {
  return (
    <div className="flex flex-col gap-3">
      <Skeleton className="h-16 rounded-lg" />
      <Skeleton className="h-16 rounded-lg" />
    </div>
  )
}
