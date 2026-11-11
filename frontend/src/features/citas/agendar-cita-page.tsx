import { useState, type ReactNode } from "react"
import { Link, useNavigate, useSearchParams } from "react-router-dom"
import { addMonths, format, startOfToday } from "date-fns"
import { es } from "react-day-picker/locale"
import {
  CalendarCheck,
  CircleAlert,
  LoaderCircle,
  PawPrint,
} from "lucide-react"
import { toast } from "sonner"
import { estadoHttp, mensajeDeError } from "@/api/errores"
import type { TipoServicio } from "@/api/types"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Skeleton } from "@/components/ui/skeleton"
import { FotoMascota } from "@/features/duenos-mascotas/foto-mascota"
import { useMascotasDeDueno } from "@/features/duenos-mascotas/use-mascotas"
import { iniciales, usePerfil } from "@/features/perfil/perfil-context"
import { useVeterinarios } from "@/features/perfil/use-opciones-perfil"
import { cn } from "@/lib/utils"
import {
  etiquetaServicio,
  formatearFechaHora,
  formatearHora,
  TIPOS_SERVICIO,
} from "@/lib/formato"
import { useAgendarCita, useDisponibilidad } from "./use-agendar"

/**
 * Ruta "/dueno/agendar". El dueño elige mascota, veterinario, día, hora
 * libre y tipo de servicio. Las reglas (horario de atención, turnos
 * ocupados) las decide el back; el front solo muestra lo que devuelve la
 * disponibilidad y el 409 si alguien ganó el turno antes.
 */
export function AgendarCitaPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { perfil } = usePerfil()
  const duenoId = perfil!.id

  const mascotas = useMascotasDeDueno(duenoId)
  const veterinarios = useVeterinarios()
  const agendar = useAgendarCita()

  const [mascotaId, setMascotaId] = useState<number | undefined>(
    () => Number(params.get("mascota")) || undefined,
  )
  const [veterinarioId, setVeterinarioId] = useState<number>()
  const [dia, setDia] = useState<Date>()
  const [hora, setHora] = useState<string>()
  const [tipoServicio, setTipoServicio] = useState<TipoServicio>()

  const fecha = dia ? format(dia, "yyyy-MM-dd") : undefined
  const disponibilidad = useDisponibilidad(veterinarioId, fecha)

  // La mascota de ?mascota= solo cuenta si de verdad es de este dueño.
  const mascota = mascotas.data?.find((m) => m.id === mascotaId)
  const veterinario = veterinarios.data?.find((v) => v.id === veterinarioId)
  const listo = mascota && veterinario && hora && tipoServicio

  function elegirVeterinario(id: number) {
    setVeterinarioId(id)
    setHora(undefined) // las horas libres dependen del veterinario
  }

  function elegirDia(nuevo: Date | undefined) {
    setDia(nuevo)
    setHora(undefined) // y del día
  }

  function confirmar() {
    if (!listo) return
    agendar.mutate(
      {
        mascotaId: mascota.id,
        veterinarioId: veterinario.id,
        fechaHora: hora,
        tipoServicio,
      },
      {
        onSuccess: (cita) => {
          toast.success(
            `Cita agendada para ${mascota.nombre}: ${formatearFechaHora(cita.fechaHora)}.`,
          )
          navigate("/dueno/citas")
        },
        onError: (error) => {
          if (estadoHttp(error) === 409) setHora(undefined)
        },
      },
    )
  }

  if (mascotas.data && mascotas.data.length === 0) {
    return (
      <>
        <EncabezadoPagina titulo="Agendar cita" />
        <EstadoVacio
          icono={PawPrint}
          titulo="Primero registra a tu mascota."
          descripcion="Para agendar una cita necesitas al menos una mascota registrada."
          accion={
            <Button asChild>
              <Link to="/dueno/mascotas">Ir a Mis mascotas</Link>
            </Button>
          }
        />
      </>
    )
  }

  const horarios = disponibilidad.data?.horarios ?? []
  const manana = horarios.filter((h) => Number(h.slice(11, 13)) < 12)
  const tarde = horarios.filter((h) => Number(h.slice(11, 13)) >= 12)

  return (
    <>
      <EncabezadoPagina
        titulo="Agendar cita"
        descripcion="Elige la mascota, el veterinario y una hora libre."
      />

      <div className="grid items-start gap-6 lg:grid-cols-[1fr_340px]">
        <div className="flex flex-col gap-6">
          {/* 1. Mascota */}
          <Paso numero={1} titulo="Mascota">
            {mascotas.isLoading ? (
              <OpcionesCargando />
            ) : mascotas.isError ? (
              <EstadoError
                mensaje={mensajeDeError(
                  mascotas.error,
                  "No se pudieron cargar tus mascotas.",
                )}
                alReintentar={() => mascotas.refetch()}
                className="py-6"
              />
            ) : (
              <GrupoOpciones etiqueta="Mascota">
                {mascotas.data?.map((m) => (
                  <Opcion
                    key={m.id}
                    elegida={m.id === mascotaId}
                    onClick={() => setMascotaId(m.id)}
                  >
                    <FotoMascota
                      nombre={m.nombre}
                      especie={m.especie}
                      fotoUrl={m.fotoUrl}
                      className="size-10 shrink-0 rounded-md [&_svg]:size-5"
                    />
                    <span className="flex flex-col text-left">
                      <span className="font-medium">{m.nombre}</span>
                      <span className="text-xs text-muted-foreground">
                        {m.raza}
                      </span>
                    </span>
                  </Opcion>
                ))}
              </GrupoOpciones>
            )}
          </Paso>

          {/* 2. Veterinario */}
          <Paso numero={2} titulo="Veterinario">
            {veterinarios.isLoading ? (
              <OpcionesCargando />
            ) : veterinarios.isError ? (
              <EstadoError
                mensaje={mensajeDeError(
                  veterinarios.error,
                  "No se pudieron cargar los veterinarios.",
                )}
                alReintentar={() => veterinarios.refetch()}
                className="py-6"
              />
            ) : (
              <GrupoOpciones etiqueta="Veterinario">
                {veterinarios.data?.map((v) => (
                  <Opcion
                    key={v.id}
                    elegida={v.id === veterinarioId}
                    onClick={() => elegirVeterinario(v.id)}
                  >
                    <Avatar className="size-10">
                      <AvatarFallback className="bg-brand-soft text-sm font-semibold text-primary">
                        {iniciales(v.nombre)}
                      </AvatarFallback>
                    </Avatar>
                    <span className="flex flex-col text-left">
                      <span className="font-medium">{v.nombre}</span>
                      <span className="text-xs text-muted-foreground">
                        {v.especialidad}
                      </span>
                    </span>
                  </Opcion>
                ))}
              </GrupoOpciones>
            )}
          </Paso>

          {/* 3. Día y hora */}
          <Paso numero={3} titulo="Día y hora">
            <div className="grid gap-6 md:grid-cols-[auto_1fr]">
              <Calendar
                mode="single"
                locale={es}
                selected={dia}
                onSelect={elegirDia}
                disabled={{ before: startOfToday() }}
                startMonth={startOfToday()}
                endMonth={addMonths(startOfToday(), 2)}
                className="rounded-lg border p-3"
              />
              <div className="flex flex-col gap-4">
                {!veterinarioId || !fecha ? (
                  <p className="text-sm text-muted-foreground">
                    Elige un veterinario y un día para ver las horas libres.
                  </p>
                ) : disponibilidad.isLoading ? (
                  <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
                    {Array.from({ length: 8 }, (_, i) => (
                      <Skeleton key={i} className="h-9" />
                    ))}
                  </div>
                ) : disponibilidad.isError ? (
                  <EstadoError
                    mensaje={mensajeDeError(
                      disponibilidad.error,
                      "No se pudieron cargar las horas libres.",
                    )}
                    alReintentar={() => disponibilidad.refetch()}
                    className="py-6"
                  />
                ) : horarios.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    No hay horas libres ese día. Prueba otro día u otro
                    veterinario.
                  </p>
                ) : (
                  <>
                    <GrupoHoras
                      titulo="Mañana"
                      horas={manana}
                      elegida={hora}
                      onElegir={setHora}
                    />
                    <GrupoHoras
                      titulo="Tarde"
                      horas={tarde}
                      elegida={hora}
                      onElegir={setHora}
                    />
                  </>
                )}
              </div>
            </div>
          </Paso>

          {/* 4. Servicio */}
          <Paso numero={4} titulo="Tipo de servicio">
            <GrupoOpciones etiqueta="Tipo de servicio">
              {TIPOS_SERVICIO.map((s) => (
                <Opcion
                  key={s.valor}
                  elegida={s.valor === tipoServicio}
                  onClick={() => setTipoServicio(s.valor)}
                >
                  <span className="font-medium">{s.etiqueta}</span>
                </Opcion>
              ))}
            </GrupoOpciones>
          </Paso>
        </div>

        {/* Resumen */}
        <aside className="flex flex-col gap-4 rounded-xl border bg-card p-5 lg:sticky lg:top-24">
          <h2 className="font-semibold">Resumen</h2>
          <dl className="flex flex-col gap-3 text-sm">
            <FilaResumen etiqueta="Mascota" valor={mascota?.nombre} />
            <FilaResumen etiqueta="Veterinario" valor={veterinario?.nombre} />
            <FilaResumen
              etiqueta="Fecha y hora"
              valor={hora ? formatearFechaHora(hora) : undefined}
            />
            <FilaResumen
              etiqueta="Servicio"
              valor={tipoServicio ? etiquetaServicio(tipoServicio) : undefined}
            />
          </dl>

          {agendar.isError && (
            <div
              role="alert"
              className="flex items-start gap-2 rounded-lg bg-destructive/5 p-3 text-sm text-destructive"
            >
              <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
              {estadoHttp(agendar.error) === 409
                ? "Ese horario ya está ocupado. Elige otra hora."
                : mensajeDeError(agendar.error, "No se pudo agendar la cita.")}
            </div>
          )}

          <Button
            size="lg"
            disabled={!listo || agendar.isPending}
            onClick={confirmar}
          >
            {agendar.isPending ? (
              <LoaderCircle className="animate-spin" />
            ) : (
              <CalendarCheck />
            )}
            {agendar.isPending ? "Agendando…" : "Confirmar cita"}
          </Button>
          <p className="text-xs text-muted-foreground">
            Te enviaremos la confirmación a tu correo.
          </p>
        </aside>
      </div>
    </>
  )
}

function Paso({
  numero,
  titulo,
  children,
}: {
  numero: number
  titulo: string
  children: ReactNode
}) {
  return (
    <section className="rounded-xl border bg-card p-5">
      <h2 className="mb-4 flex items-center gap-2 font-semibold">
        <span className="flex size-6 items-center justify-center rounded-full bg-brand-soft text-xs text-primary">
          {numero}
        </span>
        {titulo}
      </h2>
      {children}
    </section>
  )
}

function GrupoOpciones({
  etiqueta,
  children,
}: {
  etiqueta: string
  children: ReactNode
}) {
  return (
    <div
      role="radiogroup"
      aria-label={etiqueta}
      className="grid gap-2 sm:grid-cols-2"
    >
      {children}
    </div>
  )
}

function Opcion({
  elegida,
  onClick,
  children,
}: {
  elegida: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={elegida}
      onClick={onClick}
      className={cn(
        "flex items-center gap-3 rounded-lg border p-3 text-sm transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
        elegida
          ? "border-primary bg-brand-soft"
          : "bg-card hover:border-primary/40",
      )}
    >
      {children}
    </button>
  )
}

function GrupoHoras({
  titulo,
  horas,
  elegida,
  onElegir,
}: {
  titulo: string
  horas: string[]
  elegida?: string
  onElegir: (hora: string) => void
}) {
  if (horas.length === 0) return null
  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-xs font-medium text-muted-foreground">{titulo}</h3>
      <div
        role="radiogroup"
        aria-label={`Horas de la ${titulo.toLowerCase()}`}
        className="grid grid-cols-3 gap-2 sm:grid-cols-4"
      >
        {horas.map((h) => (
          <button
            key={h}
            type="button"
            role="radio"
            aria-checked={h === elegida}
            onClick={() => onElegir(h)}
            className={cn(
              "h-9 rounded-md border text-sm font-medium transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
              h === elegida
                ? "border-primary bg-primary text-primary-foreground"
                : "bg-card hover:border-primary/40 hover:bg-accent",
            )}
          >
            {formatearHora(h)}
          </button>
        ))}
      </div>
    </div>
  )
}

function FilaResumen({
  etiqueta,
  valor,
}: {
  etiqueta: string
  valor?: string
}) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd className={cn("font-medium", !valor && "text-muted-foreground")}>
        {valor ?? "Sin elegir"}
      </dd>
    </div>
  )
}

function OpcionesCargando() {
  return (
    <div className="grid gap-2 sm:grid-cols-2">
      <Skeleton className="h-16" />
      <Skeleton className="h-16" />
    </div>
  )
}
