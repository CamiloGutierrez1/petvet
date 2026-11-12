import { useState } from "react"
import { Link } from "react-router-dom"
import { addDays, format, isToday, parseISO } from "date-fns"
import {
  CalendarX2,
  ChevronLeft,
  ChevronRight,
  ClipboardPlus,
  MoreHorizontal,
  UserRound,
  XCircle,
} from "lucide-react"
import { ahoraIsoLocal } from "@/api/http"
import { mensajeDeError } from "@/api/errores"
import type { Cita, MascotaDetalle } from "@/api/types"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoCitaBadge } from "@/components/estado-cita-badge"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { TablaDatos, type ColumnaTabla } from "@/components/tabla-datos"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { CancelarCitaDialog } from "@/features/citas/cancelar-cita-dialog"
import { FotoMascota } from "@/features/duenos-mascotas/foto-mascota"
import { etiquetaEspecie } from "@/features/duenos-mascotas/mascota-utils"
import { usePerfil } from "@/features/perfil/perfil-context"
import { etiquetaServicio, formatearHora } from "@/lib/formato"
import { useAgenda, useFichas } from "./use-agenda"

const DIA = new Intl.DateTimeFormat("es-CO", {
  weekday: "long",
  day: "numeric",
  month: "long",
})

/** Ruta "/veterinario/agenda": citas del veterinario conectado, por día. */
export function AgendaPage() {
  const { perfil } = usePerfil()
  const veterinarioId = perfil!.id
  const [dia, setDia] = useState(() => new Date())
  const fecha = format(dia, "yyyy-MM-dd")

  const agenda = useAgenda(veterinarioId, fecha)
  const citas = agenda.data ?? []
  const fichas = useFichas(citas.map((c) => c.mascotaId))
  const [aCancelar, setACancelar] = useState<Cita | null>(null)

  const ahora = ahoraIsoLocal()
  const conteo = {
    total: citas.length,
    agendadas: citas.filter((c) => c.estado === "AGENDADA").length,
    completadas: citas.filter((c) => c.estado === "COMPLETADA").length,
    canceladas: citas.filter((c) => c.estado === "CANCELADA").length,
  }

  const columnas: ColumnaTabla<Cita>[] = [
    {
      id: "hora",
      encabezado: "Hora",
      celda: (c) => (
        <span className="font-medium whitespace-nowrap">
          {formatearHora(c.fechaHora)}
        </span>
      ),
    },
    {
      id: "paciente",
      encabezado: "Paciente",
      celda: (c) => (
        <Paciente
          mascotaId={c.mascotaId}
          ficha={fichas.porId.get(c.mascotaId)}
          cargando={fichas.cargando}
        />
      ),
    },
    {
      id: "dueno",
      encabezado: "Dueño",
      celda: (c) => {
        const ficha = fichas.porId.get(c.mascotaId)
        if (!ficha) return <span className="text-muted-foreground">—</span>
        return (
          <span className="flex flex-col text-sm">
            <span>{ficha.duenoNombre}</span>
            <span className="text-muted-foreground">{ficha.duenoEmail}</span>
          </span>
        )
      },
    },
    {
      id: "servicio",
      encabezado: "Servicio",
      celda: (c) => etiquetaServicio(c.tipoServicio),
    },
    {
      id: "estado",
      encabezado: "Estado",
      celda: (c) => <EstadoCitaBadge estado={c.estado} />,
    },
    {
      id: "acciones",
      encabezado: <span className="sr-only">Acciones</span>,
      className: "text-right",
      celda: (c) => (
        <Acciones
          cita={c}
          nombre={fichas.porId.get(c.mascotaId)?.nombre}
          // Solo se atiende una cita agendada; cancelar, si aún no pasa.
          puedeCancelar={c.estado === "AGENDADA" && c.fechaHora >= ahora}
          alCancelar={() => setACancelar(c)}
        />
      ),
    },
  ]

  return (
    <>
      <EncabezadoPagina
        titulo="Agenda"
        descripcion={
          <span className="capitalize">
            {DIA.format(dia)}
            {isToday(dia) && " · hoy"}
          </span>
        }
        acciones={
          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="outline"
              size="icon"
              onClick={() => setDia((d) => addDays(d, -1))}
              aria-label="Día anterior"
            >
              <ChevronLeft />
            </Button>
            <Button
              variant="outline"
              onClick={() => setDia(new Date())}
              disabled={isToday(dia)}
            >
              Hoy
            </Button>
            <Button
              variant="outline"
              size="icon"
              onClick={() => setDia((d) => addDays(d, 1))}
              aria-label="Día siguiente"
            >
              <ChevronRight />
            </Button>
            <Input
              type="date"
              value={fecha}
              onChange={(e) =>
                e.target.value && setDia(parseISO(e.target.value))
              }
              aria-label="Elegir fecha"
              className="w-auto"
            />
          </div>
        }
      />

      <dl className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Conteo
          etiqueta="Citas"
          valor={conteo.total}
          cargando={agenda.isLoading}
        />
        <Conteo
          etiqueta="Agendadas"
          valor={conteo.agendadas}
          cargando={agenda.isLoading}
        />
        <Conteo
          etiqueta="Completadas"
          valor={conteo.completadas}
          cargando={agenda.isLoading}
        />
        <Conteo
          etiqueta="Canceladas"
          valor={conteo.canceladas}
          cargando={agenda.isLoading}
        />
      </dl>

      {agenda.isError ? (
        <EstadoError
          mensaje={mensajeDeError(agenda.error, "No se pudo cargar la agenda.")}
          alReintentar={() => agenda.refetch()}
        />
      ) : !agenda.isLoading && citas.length === 0 ? (
        <EstadoVacio
          icono={CalendarX2}
          titulo="No hay citas para este día."
          descripcion="Usa las flechas o elige otra fecha."
        />
      ) : (
        <div className="overflow-x-auto rounded-xl bg-card">
          <TablaDatos
            columnas={columnas}
            filas={citas}
            obtenerClave={(c) => c.id}
            cargando={agenda.isLoading}
          />
        </div>
      )}

      <CancelarCitaDialog
        cita={aCancelar}
        nombreMascota={
          aCancelar
            ? (fichas.porId.get(aCancelar.mascotaId)?.nombre ?? "la mascota")
            : ""
        }
        onCerrar={() => setACancelar(null)}
      />
    </>
  )
}

function Conteo({
  etiqueta,
  valor,
  cargando,
}: {
  etiqueta: string
  valor: number
  cargando: boolean
}) {
  return (
    <div className="rounded-xl border bg-card px-4 py-3">
      <dt className="text-sm text-muted-foreground">{etiqueta}</dt>
      <dd className="text-2xl font-semibold">
        {cargando ? <Skeleton className="mt-1 h-7 w-8" /> : valor}
      </dd>
    </div>
  )
}

function Paciente({
  mascotaId,
  ficha,
  cargando,
}: {
  mascotaId: number
  ficha?: MascotaDetalle
  cargando: boolean
}) {
  if (!ficha) {
    return cargando ? (
      <Skeleton className="h-10 w-40" />
    ) : (
      // Mascotas no respondió: la agenda se muestra igual.
      <span className="text-muted-foreground">Mascota #{mascotaId}</span>
    )
  }
  return (
    <Link
      to={`/veterinario/pacientes/${ficha.id}`}
      className="flex items-center gap-3 hover:underline"
    >
      <FotoMascota
        nombre={ficha.nombre}
        especie={ficha.especie}
        fotoUrl={ficha.fotoUrl}
        className="size-10 shrink-0 rounded-full [&_svg]:size-5"
      />
      <span className="flex flex-col">
        <span className="font-medium">{ficha.nombre}</span>
        <span className="text-sm text-muted-foreground">
          {etiquetaEspecie(ficha.especie)} · {ficha.raza}
        </span>
      </span>
    </Link>
  )
}

function Acciones({
  cita,
  nombre,
  puedeCancelar,
  alCancelar,
}: {
  cita: Cita
  nombre?: string
  puedeCancelar: boolean
  alCancelar: () => void
}) {
  return (
    <div className="flex items-center justify-end gap-2">
      {cita.estado === "AGENDADA" && (
        <Button asChild size="sm">
          <Link to={`/veterinario/consulta/${cita.id}`}>
            <ClipboardPlus />
            Registrar consulta
          </Link>
        </Button>
      )}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            aria-label={`Más acciones${nombre ? ` para ${nombre}` : ""}`}
          >
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem asChild>
            <Link to={`/veterinario/pacientes/${cita.mascotaId}`}>
              <UserRound />
              Ver paciente
            </Link>
          </DropdownMenuItem>
          {puedeCancelar && (
            <DropdownMenuItem onSelect={alCancelar} variant="destructive">
              <XCircle />
              Cancelar cita
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  )
}
