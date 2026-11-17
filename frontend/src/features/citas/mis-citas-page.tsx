import { useState } from "react"
import { Link } from "react-router-dom"
import { parseISO } from "date-fns"
import { CalendarPlus, CalendarX2 } from "lucide-react"
import { ahoraIsoLocal } from "@/api/http"
import { mensajeDeError } from "@/api/errores"
import type { Cita, Mascota } from "@/api/types"
import { CargandoSkeleton } from "@/components/cargando-skeleton"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoCitaBadge } from "@/components/estado-cita-badge"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { Button } from "@/components/ui/button"
import { FotoMascota } from "@/features/duenos-mascotas/foto-mascota"
import { useMascotasDeDueno } from "@/features/duenos-mascotas/use-mascotas"
import { usePerfil } from "@/features/perfil/perfil-context"
import { useVeterinarios } from "@/features/perfil/use-opciones-perfil"
import { etiquetaServicio, formatearHora } from "@/lib/formato"
import { cn } from "@/lib/utils"
import { CancelarCitaDialog } from "./cancelar-cita-dialog"
import { useCitasDeMascotas } from "./use-mis-citas"

type Pestana = "proximas" | "anteriores"

const MES = new Intl.DateTimeFormat("es-CO", { month: "short" })
const DIA_SEMANA = new Intl.DateTimeFormat("es-CO", { weekday: "long" })

/** Próxima = agendada y todavía no ha pasado. Lo demás va en "Anteriores". */
function esProxima(cita: Cita, ahora: string) {
  return cita.estado === "AGENDADA" && cita.fechaHora >= ahora
}

/** Ruta "/dueno/citas": citas de todas las mascotas del dueño. */
export function MisCitasPage() {
  const { perfil } = usePerfil()
  const mascotas = useMascotasDeDueno(perfil!.id)
  const { citas, isLoading, error, refetch } = useCitasDeMascotas(mascotas.data)
  const { data: veterinarios } = useVeterinarios()

  const [pestana, setPestana] = useState<Pestana>("proximas")
  const [aCancelar, setACancelar] = useState<Cita | null>(null)

  const ahora = ahoraIsoLocal()
  const proximas = citas
    .filter((c) => esProxima(c, ahora))
    .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora))
  const anteriores = citas
    .filter((c) => !esProxima(c, ahora))
    .sort((a, b) => b.fechaHora.localeCompare(a.fechaHora))
  const visibles = pestana === "proximas" ? proximas : anteriores

  const mascotaDe = (cita: Cita) =>
    mascotas.data?.find((m) => m.id === cita.mascotaId)
  const veterinarioDe = (cita: Cita) =>
    veterinarios?.find((v) => v.id === cita.veterinarioId)?.nombre ?? "—"

  const cargando = mascotas.isLoading || isLoading
  const fallo = mascotas.error ?? error

  return (
    <>
      <EncabezadoPagina
        titulo="Mis citas"
        descripcion="Las citas de todas tus mascotas."
        acciones={
          <Button asChild>
            <Link to="/dueno/agendar">
              <CalendarPlus />
              Agendar cita
            </Link>
          </Button>
        }
      />

      <div
        role="tablist"
        aria-label="Citas"
        className="mb-6 inline-flex gap-1 rounded-lg border bg-card p-1"
      >
        <BotonPestana
          activa={pestana === "proximas"}
          onClick={() => setPestana("proximas")}
          cantidad={cargando ? undefined : proximas.length}
        >
          Próximas
        </BotonPestana>
        <BotonPestana
          activa={pestana === "anteriores"}
          onClick={() => setPestana("anteriores")}
          cantidad={cargando ? undefined : anteriores.length}
        >
          Anteriores
        </BotonPestana>
      </div>

      <section
        role="tabpanel"
        aria-label={pestana === "proximas" ? "Próximas" : "Anteriores"}
      >
        {cargando ? (
          <CargandoSkeleton variante="lista" cantidad={4} />
        ) : fallo ? (
          <EstadoError
            mensaje={mensajeDeError(fallo, "No se pudieron cargar tus citas.")}
            alReintentar={() => {
              mascotas.refetch()
              refetch()
            }}
          />
        ) : visibles.length === 0 ? (
          <EstadoVacio
            icono={CalendarX2}
            titulo={
              pestana === "proximas"
                ? "No tienes citas próximas."
                : "Todavía no hay citas anteriores."
            }
            accion={
              pestana === "proximas" && (mascotas.data?.length ?? 0) > 0 ? (
                <Button asChild>
                  <Link to="/dueno/agendar">Agendar cita</Link>
                </Button>
              ) : undefined
            }
          />
        ) : (
          <ul className="flex flex-col gap-3">
            {visibles.map((cita) => (
              <li key={cita.id}>
                <FilaCita
                  cita={cita}
                  mascota={mascotaDe(cita)}
                  veterinario={veterinarioDe(cita)}
                  cancelable={esProxima(cita, ahora)}
                  alCancelar={() => setACancelar(cita)}
                />
              </li>
            ))}
          </ul>
        )}
      </section>

      <CancelarCitaDialog
        cita={aCancelar}
        nombreMascota={
          aCancelar ? (mascotaDe(aCancelar)?.nombre ?? "tu mascota") : ""
        }
        onCerrar={() => setACancelar(null)}
      />
    </>
  )
}

function BotonPestana({
  activa,
  onClick,
  cantidad,
  children,
}: {
  activa: boolean
  onClick: () => void
  cantidad?: number
  children: string
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={activa}
      onClick={onClick}
      className={cn(
        "flex h-9 items-center gap-2 rounded-md px-4 text-sm font-medium transition-colors",
        activa
          ? "bg-brand-soft text-primary"
          : "text-muted-foreground hover:text-foreground",
      )}
    >
      {children}
      {cantidad !== undefined && (
        <span
          className={cn(
            "rounded-full px-1.5 text-xs",
            activa ? "bg-card" : "bg-muted",
          )}
        >
          {cantidad}
        </span>
      )}
    </button>
  )
}

interface FilaCitaProps {
  cita: Cita
  mascota?: Mascota
  veterinario: string
  cancelable: boolean
  alCancelar: () => void
}

function FilaCita({
  cita,
  mascota,
  veterinario,
  cancelable,
  alCancelar,
}: FilaCitaProps) {
  const fecha = parseISO(cita.fechaHora)
  return (
    <article className="flex flex-col gap-4 rounded-xl border bg-card p-4 sm:flex-row sm:items-center">
      {/* Fecha tipo calendario */}
      <div className="flex items-center gap-4 sm:w-44 sm:shrink-0">
        <div className="flex size-14 shrink-0 flex-col items-center justify-center rounded-lg bg-brand-soft text-primary">
          <span className="text-xl leading-none font-bold">
            {fecha.getDate()}
          </span>
          <span className="text-xs font-medium uppercase">
            {MES.format(fecha).replace(".", "")}
          </span>
        </div>
        <div className="flex flex-col text-sm">
          <span className="font-medium capitalize">
            {DIA_SEMANA.format(fecha)}
          </span>
          <span className="text-muted-foreground">
            {formatearHora(cita.fechaHora)}
          </span>
        </div>
      </div>

      {/* Mascota y detalle */}
      <div className="flex min-w-0 flex-1 items-center gap-3">
        {mascota && (
          <FotoMascota
            nombre={mascota.nombre}
            especie={mascota.especie}
            fotoUrl={mascota.fotoUrl}
            className="size-10 shrink-0 rounded-full [&_svg]:size-5"
          />
        )}
        <div className="flex min-w-0 flex-col text-sm">
          <span className="truncate font-medium">
            {mascota ? (
              <Link
                to={`/dueno/mascotas/${mascota.id}`}
                className="hover:underline"
              >
                {mascota.nombre}
              </Link>
            ) : (
              "Mascota"
            )}{" "}
            · {etiquetaServicio(cita.tipoServicio)}
          </span>
          <span className="truncate text-muted-foreground">{veterinario}</span>
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 sm:justify-end">
        <EstadoCitaBadge estado={cita.estado} />
        {cancelable && (
          <Button variant="outline" size="sm" onClick={alCancelar}>
            Cancelar
          </Button>
        )}
      </div>
    </article>
  )
}
