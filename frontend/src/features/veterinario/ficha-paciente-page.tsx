import type { ReactNode } from "react"
import { useParams } from "react-router-dom"
import { ClipboardList, Mail, PawPrint, Syringe } from "lucide-react"
import { estadoHttp, mensajeDeError } from "@/api/errores"
import type { Cita, MascotaDetalle, RegistroHistorial } from "@/api/types"
import { CargandoSkeleton } from "@/components/cargando-skeleton"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoCitaBadge } from "@/components/estado-cita-badge"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { TablaDatos, type ColumnaTabla } from "@/components/tabla-datos"
import { Skeleton } from "@/components/ui/skeleton"
import { FotoMascota } from "@/features/duenos-mascotas/foto-mascota"
import {
  edadTexto,
  etiquetaEspecie,
  pesoTexto,
} from "@/features/duenos-mascotas/mascota-utils"
import {
  useCitasDeMascota,
  useMascota,
  useProximasVacunas,
} from "@/features/duenos-mascotas/use-ficha-mascota"
import { useVeterinarios } from "@/features/perfil/use-opciones-perfil"
import {
  etiquetaServicio,
  formatearFecha,
  formatearFechaHora,
  textoPlazo,
} from "@/lib/formato"
import { useHistorial } from "./use-historial"

/**
 * Ruta "/veterinario/pacientes/:mascotaId": ficha clínica que ve el
 * veterinario. Datos y dueño vienen de Mascotas; vacunas, historial y citas
 * de Citas. Cada sección carga y falla por separado.
 */
export function FichaPacientePage() {
  const mascotaId = Number(useParams().mascotaId)
  const { data: ficha, isLoading, error, refetch } = useMascota(mascotaId)

  if (isLoading) return <FichaCargando />

  if (estadoHttp(error) === 404) {
    return (
      <>
        <EncabezadoPagina
          titulo="Paciente no encontrado"
          volverA="/veterinario/agenda"
        />
        <EstadoVacio icono={PawPrint} titulo="No existe este paciente." />
      </>
    )
  }

  return (
    <>
      <EncabezadoPagina
        titulo={ficha?.nombre ?? `Paciente #${mascotaId}`}
        descripcion={
          ficha
            ? `${etiquetaEspecie(ficha.especie)} · ${ficha.raza}`
            : undefined
        }
        volverA="/veterinario/agenda"
      />

      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        {error || !ficha ? (
          // Mascotas no respondió: lo de Citas se sigue mostrando al lado.
          <EstadoError
            mensaje={mensajeDeError(
              error,
              "No se pudieron cargar los datos del paciente.",
            )}
            alReintentar={() => refetch()}
            className="h-fit"
          />
        ) : (
          <DatosPaciente ficha={ficha} />
        )}
        <div className="flex min-w-0 flex-col gap-6">
          <SeccionVacunas mascotaId={mascotaId} />
          <SeccionHistorial mascotaId={mascotaId} />
          <SeccionCitas mascotaId={mascotaId} />
        </div>
      </div>
    </>
  )
}

function Tarjeta({
  titulo,
  children,
}: {
  titulo: string
  children: ReactNode
}) {
  return (
    <section className="rounded-xl border bg-card p-5">
      <h2 className="mb-4 font-semibold">{titulo}</h2>
      {children}
    </section>
  )
}

function DatosPaciente({ ficha }: { ficha: MascotaDetalle }) {
  const datos: [string, string][] = [
    ["Especie", etiquetaEspecie(ficha.especie)],
    ["Raza", ficha.raza],
    ["Edad", edadTexto(ficha.fechaNacimiento)],
    ["Nacimiento", formatearFecha(ficha.fechaNacimiento)],
    ["Peso", pesoTexto(ficha.peso)],
  ]
  return (
    <div className="flex h-fit flex-col gap-4">
      <section className="overflow-hidden rounded-xl border bg-card">
        <FotoMascota
          nombre={ficha.nombre}
          especie={ficha.especie}
          fotoUrl={ficha.fotoUrl}
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
      <section className="rounded-xl border bg-card p-5 text-sm">
        <h2 className="mb-2 font-semibold">Dueño</h2>
        <p className="font-medium">{ficha.duenoNombre}</p>
        <a
          href={`mailto:${ficha.duenoEmail}`}
          className="mt-1 inline-flex items-center gap-1.5 text-primary hover:underline"
        >
          <Mail className="size-4" aria-hidden />
          {ficha.duenoEmail}
        </a>
      </section>
    </div>
  )
}

function SeccionVacunas({ mascotaId }: { mascotaId: number }) {
  const { data, isLoading, error, refetch } = useProximasVacunas(mascotaId)
  return (
    <Tarjeta titulo="Vacunas pendientes">
      {isLoading ? (
        <CargandoSkeleton variante="lista" cantidad={2} />
      ) : error ? (
        <EstadoError
          mensaje={mensajeDeError(error, "No se pudieron cargar las vacunas.")}
          alReintentar={() => refetch()}
          className="py-8"
        />
      ) : !data?.length ? (
        <p className="text-sm text-muted-foreground">
          No tiene vacunas pendientes.
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {data.map((v) => (
            <li
              key={`${v.vacuna}-${v.fechaLimite}`}
              className="flex items-center gap-3 rounded-lg bg-alerta-soft p-3"
            >
              <Syringe className="size-5 shrink-0 text-alerta" aria-hidden />
              <span className="flex flex-col">
                <span className="font-medium text-alerta-texto">
                  {v.vacuna}
                </span>
                <span className="text-sm text-alerta-texto/80">
                  Antes del {formatearFecha(v.fechaLimite)} ·{" "}
                  {textoPlazo(v.fechaLimite)}
                </span>
              </span>
            </li>
          ))}
        </ul>
      )}
    </Tarjeta>
  )
}

function SeccionHistorial({ mascotaId }: { mascotaId: number }) {
  const { data, isLoading, error, refetch } = useHistorial(mascotaId)
  const { data: veterinarios } = useVeterinarios()
  const nombreVeterinario = (id: number) =>
    veterinarios?.find((v) => v.id === id)?.nombre

  return (
    <Tarjeta titulo="Historial clínico">
      {isLoading ? (
        <CargandoSkeleton variante="lista" cantidad={3} />
      ) : error ? (
        <EstadoError
          mensaje={mensajeDeError(error, "No se pudo cargar el historial.")}
          alReintentar={() => refetch()}
          className="py-8"
        />
      ) : !data?.length ? (
        <EstadoVacio
          icono={ClipboardList}
          titulo="Todavía no tiene consultas registradas."
          className="py-8"
        />
      ) : (
        <ol className="relative flex flex-col gap-6 border-l pl-6">
          {data.map((registro) => (
            <EntradaHistorial
              key={registro.id}
              registro={registro}
              veterinario={nombreVeterinario(registro.veterinarioId)}
            />
          ))}
        </ol>
      )}
    </Tarjeta>
  )
}

function EntradaHistorial({
  registro,
  veterinario,
}: {
  registro: RegistroHistorial
  veterinario?: string
}) {
  const listas: [string, string[]][] = [
    ["Vacunas aplicadas", registro.vacunasAplicadas],
    ["Tratamientos", registro.tratamientos],
    ["Exámenes", registro.examenes],
  ]
  return (
    <li className="relative">
      {/* Punto de la línea de tiempo */}
      <span
        className="absolute top-1.5 -left-[31px] size-3 rounded-full border-2 border-card bg-primary"
        aria-hidden
      />
      <div className="flex flex-wrap items-baseline gap-x-2 text-sm text-muted-foreground">
        <span className="font-medium text-foreground">
          {formatearFechaHora(registro.fecha)}
        </span>
        <span>
          · Consulta{" "}
          {registro.tipoConsulta === "GENERAL" ? "general" : "con especialista"}
        </span>
        {veterinario && <span>· {veterinario}</span>}
      </div>
      <p className="mt-1 font-medium">{registro.diagnostico}</p>

      {listas.some(([, items]) => items.length > 0) && (
        <dl className="mt-3 flex flex-col gap-2 text-sm">
          {listas
            .filter(([, items]) => items.length > 0)
            .map(([etiqueta, items]) => (
              <div key={etiqueta} className="flex flex-wrap items-center gap-2">
                <dt className="text-muted-foreground">{etiqueta}:</dt>
                {items.map((item) => (
                  <dd
                    key={item}
                    className="rounded-full bg-brand-soft px-2.5 py-0.5 text-primary"
                  >
                    {item}
                  </dd>
                ))}
              </div>
            ))}
        </dl>
      )}

      {registro.observaciones && (
        <p className="mt-2 text-sm text-muted-foreground">
          {registro.observaciones}
        </p>
      )}
      {registro.proximoControl && (
        <p className="mt-2 text-sm">
          <span className="text-muted-foreground">Próximo control:</span>{" "}
          <span className="font-medium">
            {formatearFecha(registro.proximoControl)}
          </span>
        </p>
      )}
    </li>
  )
}

function SeccionCitas({ mascotaId }: { mascotaId: number }) {
  const { data, isLoading, error, refetch } = useCitasDeMascota(mascotaId)
  const { data: veterinarios } = useVeterinarios()
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
      celda: (c) =>
        veterinarios?.find((v) => v.id === c.veterinarioId)?.nombre ?? "—",
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
            filas={data}
            obtenerClave={(c) => c.id}
            cargando={isLoading}
            mensajeVacio="No tiene citas."
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
      </div>
      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        <Skeleton className="h-96 rounded-xl" />
        <div className="flex flex-col gap-6">
          <Skeleton className="h-32 rounded-xl" />
          <Skeleton className="h-64 rounded-xl" />
        </div>
      </div>
    </div>
  )
}
