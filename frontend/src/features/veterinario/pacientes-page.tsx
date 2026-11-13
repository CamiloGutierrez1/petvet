import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { PawPrint, Search } from "lucide-react"
import { ahoraIsoLocal } from "@/api/http"
import { mensajeDeError } from "@/api/errores"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { TablaDatos, type ColumnaTabla } from "@/components/tabla-datos"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { FotoMascota } from "@/features/duenos-mascotas/foto-mascota"
import { etiquetaEspecie } from "@/features/duenos-mascotas/mascota-utils"
import { usePerfil } from "@/features/perfil/perfil-context"
import { formatearFecha, formatearFechaHora } from "@/lib/formato"
import { useFichas } from "./use-agenda"
import {
  resumirPacientes,
  useCitasDeVeterinario,
  type ResumenPaciente,
} from "./use-pacientes"

/**
 * Ruta "/veterinario/pacientes": mascotas que han tenido cita con el
 * veterinario conectado. Las citas vienen de Citas y los datos de cada
 * paciente de Mascotas; si Mascotas falla, la lista se muestra igual.
 */
export function PacientesPage() {
  const navigate = useNavigate()
  const { perfil } = usePerfil()
  const citas = useCitasDeVeterinario(perfil!.id)
  const pacientes = resumirPacientes(citas.data ?? [], ahoraIsoLocal())
  const fichas = useFichas(pacientes.map((p) => p.mascotaId))
  const [busqueda, setBusqueda] = useState("")

  const texto = busqueda.trim().toLowerCase()
  const visibles = texto
    ? pacientes.filter((p) => {
        const ficha = fichas.porId.get(p.mascotaId)
        return (
          ficha?.nombre.toLowerCase().includes(texto) ||
          ficha?.duenoNombre.toLowerCase().includes(texto)
        )
      })
    : pacientes

  const columnas: ColumnaTabla<ResumenPaciente>[] = [
    {
      id: "paciente",
      encabezado: "Paciente",
      celda: (p) => {
        const ficha = fichas.porId.get(p.mascotaId)
        if (!ficha) {
          return fichas.cargando ? (
            <Skeleton className="h-10 w-40" />
          ) : (
            <span className="text-muted-foreground">
              Mascota #{p.mascotaId}
            </span>
          )
        }
        return (
          <span className="flex items-center gap-3">
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
          </span>
        )
      },
    },
    {
      id: "dueno",
      encabezado: "Dueño",
      celda: (p) => fichas.porId.get(p.mascotaId)?.duenoNombre ?? "—",
    },
    {
      id: "ultima",
      encabezado: "Última consulta",
      className: "whitespace-nowrap",
      celda: (p) =>
        p.ultimaConsulta ? (
          formatearFecha(p.ultimaConsulta.slice(0, 10))
        ) : (
          <span className="text-muted-foreground">Sin consultas</span>
        ),
    },
    {
      id: "proxima",
      encabezado: "Próxima cita",
      className: "whitespace-nowrap",
      celda: (p) =>
        p.proximaCita ? (
          <span className="font-medium text-primary">
            {formatearFechaHora(p.proximaCita)}
          </span>
        ) : (
          <span className="text-muted-foreground">—</span>
        ),
    },
    {
      id: "citas",
      encabezado: "Citas",
      className: "text-right",
      celda: (p) => p.totalCitas,
    },
  ]

  return (
    <>
      <EncabezadoPagina
        titulo="Pacientes"
        descripcion={
          citas.data
            ? `${pacientes.length} ${pacientes.length === 1 ? "mascota ha" : "mascotas han"} tenido cita contigo.`
            : undefined
        }
        acciones={
          <div className="relative w-full sm:w-72">
            <Search
              className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              type="search"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar paciente o dueño"
              aria-label="Buscar paciente o dueño"
              className="pl-9"
            />
          </div>
        }
      />

      {citas.isError ? (
        <EstadoError
          mensaje={mensajeDeError(
            citas.error,
            "No se pudo cargar la lista de pacientes.",
          )}
          alReintentar={() => citas.refetch()}
        />
      ) : !citas.isLoading && pacientes.length === 0 ? (
        <EstadoVacio
          icono={PawPrint}
          titulo="Todavía no tienes pacientes."
          descripcion="Aquí aparecerán las mascotas que tengan citas contigo."
        />
      ) : !citas.isLoading && visibles.length === 0 ? (
        <EstadoVacio
          icono={Search}
          titulo={`No hay pacientes que coincidan con "${busqueda.trim()}".`}
        />
      ) : (
        <div className="overflow-x-auto rounded-xl bg-card">
          <TablaDatos
            columnas={columnas}
            filas={visibles}
            obtenerClave={(p) => p.mascotaId}
            cargando={citas.isLoading}
            alHacerClicFila={(p) =>
              navigate(`/veterinario/pacientes/${p.mascotaId}`)
            }
          />
        </div>
      )}
    </>
  )
}
