import { useState, type ReactNode } from "react"
import { Link, useParams } from "react-router-dom"
import { CalendarPlus, PawPrint, Pencil, Syringe } from "lucide-react"
import { estadoHttp, mensajeDeError } from "@/api/errores"
import type { Cita, MascotaDetalle } from "@/api/types"
import { CargandoSkeleton } from "@/components/cargando-skeleton"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoCitaBadge } from "@/components/estado-cita-badge"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { TablaDatos, type ColumnaTabla } from "@/components/tabla-datos"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { usePerfil } from "@/features/perfil/perfil-context"
import { useVeterinarios } from "@/features/perfil/use-opciones-perfil"
import {
  etiquetaServicio,
  formatearFecha,
  formatearFechaHora,
  textoPlazo,
} from "@/lib/formato"
import { FormularioMascotaDialog } from "./formulario-mascota-dialog"
import { FotoMascota } from "./foto-mascota"
import { edadTexto, etiquetaEspecie, pesoTexto } from "./mascota-utils"
import {
  useCitasDeMascota,
  useMascota,
  useProximasVacunas,
} from "./use-ficha-mascota"

/** Ruta "/dueno/mascotas/:mascotaId": datos, próximas vacunas y citas. */
export function FichaMascotaPage() {
  const mascotaId = Number(useParams().mascotaId)
  const { perfil } = usePerfil()
  const { data: mascota, isLoading, error, refetch } = useMascota(mascotaId)
  const [editando, setEditando] = useState(false)

  if (isLoading) return <FichaCargando />

  // 404, o una mascota que no es de este dueño: se trata igual.
  if (
    estadoHttp(error) === 404 ||
    (mascota && mascota.duenoId !== perfil?.id)
  ) {
    return (
      <>
        <EncabezadoPagina
          titulo="Mascota no encontrada"
          volverA="/dueno/mascotas"
        />
        <EstadoVacio
          icono={PawPrint}
          titulo="No encontramos esta mascota entre las tuyas."
          accion={
            <Button asChild variant="outline">
              <Link to="/dueno/mascotas">Ver mis mascotas</Link>
            </Button>
          }
        />
      </>
    )
  }

  if (error || !mascota) {
    return (
      <>
        <EncabezadoPagina
          titulo="Ficha de la mascota"
          volverA="/dueno/mascotas"
        />
        <EstadoError
          mensaje={mensajeDeError(error, "No se pudo cargar la ficha.")}
          alReintentar={() => refetch()}
        />
      </>
    )
  }

  return (
    <>
      <EncabezadoPagina
        titulo={mascota.nombre}
        descripcion={`${etiquetaEspecie(mascota.especie)} · ${mascota.raza}`}
        volverA="/dueno/mascotas"
        acciones={
          <>
            <Button variant="outline" onClick={() => setEditando(true)}>
              <Pencil />
              Editar
            </Button>
            <Button asChild>
              <Link to={`/dueno/agendar?mascota=${mascota.id}`}>
                <CalendarPlus />
                Agendar cita
              </Link>
            </Button>
          </>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        <DatosMascota mascota={mascota} />
        <div className="flex min-w-0 flex-col gap-6">
          <SeccionVacunas mascotaId={mascota.id} />
          <SeccionCitas mascotaId={mascota.id} />
        </div>
      </div>

      <FormularioMascotaDialog
        abierto={editando}
        onAbiertoChange={setEditando}
        duenoId={mascota.duenoId}
        mascota={mascota}
      />
    </>
  )
}

function Tarjeta({
  titulo,
  children,
}: {
  titulo?: string
  children: ReactNode
}) {
  return (
    <section className="rounded-xl border bg-card p-5">
      {titulo && <h2 className="mb-4 font-semibold">{titulo}</h2>}
      {children}
    </section>
  )
}

function DatosMascota({ mascota }: { mascota: MascotaDetalle }) {
  const datos: [string, string][] = [
    ["Especie", etiquetaEspecie(mascota.especie)],
    ["Raza", mascota.raza],
    ["Edad", edadTexto(mascota.fechaNacimiento)],
    ["Nacimiento", formatearFecha(mascota.fechaNacimiento)],
    ["Peso", pesoTexto(mascota.peso)],
  ]
  return (
    <section className="h-fit overflow-hidden rounded-xl border bg-card">
      <FotoMascota
        nombre={mascota.nombre}
        especie={mascota.especie}
        fotoUrl={mascota.fotoUrl}
      />
      <dl className="divide-y px-5">
        {datos.map(([etiqueta, valor]) => (
          <div
            key={etiqueta}
            className="flex justify-between gap-4 py-3 text-sm"
          >
            <dt className="text-muted-foreground">{etiqueta}</dt>
            <dd className="text-right font-medium">{valor}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}

function SeccionVacunas({ mascotaId }: { mascotaId: number }) {
  const {
    data: vacunas,
    isLoading,
    error,
    refetch,
  } = useProximasVacunas(mascotaId)

  return (
    <Tarjeta titulo="Próximas vacunas">
      {isLoading ? (
        <CargandoSkeleton variante="lista" cantidad={2} />
      ) : error ? (
        <EstadoError
          mensaje={mensajeDeError(error, "No se pudieron cargar las vacunas.")}
          alReintentar={() => refetch()}
          className="py-8"
        />
      ) : !vacunas || vacunas.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          No tiene vacunas pendientes.
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {vacunas.map((vacuna) => (
            <li
              key={`${vacuna.vacuna}-${vacuna.fechaLimite}`}
              className="flex items-center gap-3 rounded-lg bg-alerta-soft p-3"
            >
              <Syringe className="size-5 shrink-0 text-alerta" aria-hidden />
              <div className="flex flex-col">
                <span className="font-medium text-alerta-texto">
                  {vacuna.vacuna}
                </span>
                <span className="text-sm text-alerta-texto/80">
                  Antes del {formatearFecha(vacuna.fechaLimite)} ·{" "}
                  {textoPlazo(vacuna.fechaLimite)}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Tarjeta>
  )
}

function SeccionCitas({ mascotaId }: { mascotaId: number }) {
  const {
    data: citas,
    isLoading,
    error,
    refetch,
  } = useCitasDeMascota(mascotaId)
  const { data: veterinarios } = useVeterinarios()
  const nombreVeterinario = (id: number) =>
    veterinarios?.find((v) => v.id === id)?.nombre ?? "—"

  const columnas: ColumnaTabla<Cita>[] = [
    {
      id: "fecha",
      encabezado: "Fecha y hora",
      celda: (c) => formatearFechaHora(c.fechaHora),
      className: "whitespace-nowrap",
    },
    {
      id: "servicio",
      encabezado: "Servicio",
      celda: (c) => etiquetaServicio(c.tipoServicio),
    },
    {
      id: "veterinario",
      encabezado: "Veterinario",
      celda: (c) => nombreVeterinario(c.veterinarioId),
    },
    {
      id: "estado",
      encabezado: "Estado",
      celda: (c) => <EstadoCitaBadge estado={c.estado} />,
    },
  ]

  return (
    <Tarjeta titulo="Citas">
      {error ? (
        <EstadoError
          mensaje={mensajeDeError(error, "No se pudieron cargar las citas.")}
          alReintentar={() => refetch()}
          className="py-8"
        />
      ) : (
        <div className="overflow-x-auto">
          <TablaDatos
            columnas={columnas}
            filas={citas}
            obtenerClave={(c) => c.id}
            cargando={isLoading}
            mensajeVacio="Todavía no tiene citas."
          />
        </div>
      )}
    </Tarjeta>
  )
}

function FichaCargando() {
  return (
    <div aria-busy="true" className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-4 w-16" />
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-4 w-40" />
      </div>
      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        <Skeleton className="h-96 rounded-xl" />
        <div className="flex flex-col gap-6">
          <Skeleton className="h-40 rounded-xl" />
          <Skeleton className="h-56 rounded-xl" />
        </div>
      </div>
    </div>
  )
}
