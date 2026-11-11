import { useState } from "react"
import { Link } from "react-router-dom"
import { PawPrint, Pencil, Plus } from "lucide-react"
import { mensajeDeError } from "@/api/errores"
import type { Mascota } from "@/api/types"
import { CargandoSkeleton } from "@/components/cargando-skeleton"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { Button } from "@/components/ui/button"
import { usePerfil } from "@/features/perfil/perfil-context"
import { FormularioMascotaDialog } from "./formulario-mascota-dialog"
import { FotoMascota } from "./foto-mascota"
import { edadTexto, etiquetaEspecie, pesoTexto } from "./mascota-utils"
import { useMascotasDeDueno } from "./use-mascotas"

/** Ruta "/dueno/mascotas": tarjetas con las mascotas del dueño. */
export function MisMascotasPage() {
  const { perfil } = usePerfil()
  const duenoId = perfil!.id // RequierePerfil garantiza que hay perfil de dueño
  const {
    data: mascotas,
    isLoading,
    error,
    refetch,
  } = useMascotasDeDueno(duenoId)

  // Diálogo: cerrado, registrando (null) o editando una mascota.
  const [dialogo, setDialogo] = useState<{ mascota?: Mascota } | null>(null)

  const botonRegistrar = (
    <Button onClick={() => setDialogo({})}>
      <Plus />
      Registrar mascota
    </Button>
  )

  return (
    <>
      <EncabezadoPagina
        titulo="Mis mascotas"
        descripcion={
          mascotas && mascotas.length > 0
            ? `${mascotas.length} ${mascotas.length === 1 ? "mascota registrada" : "mascotas registradas"}`
            : undefined
        }
        acciones={botonRegistrar}
      />

      {isLoading ? (
        <CargandoSkeleton variante="tarjetas" cantidad={3} />
      ) : error ? (
        <EstadoError
          mensaje={mensajeDeError(error, "No se pudieron cargar tus mascotas.")}
          alReintentar={() => refetch()}
        />
      ) : !mascotas || mascotas.length === 0 ? (
        <EstadoVacio
          icono={PawPrint}
          titulo="Todavía no tienes mascotas registradas."
          descripcion="Registra a tu mascota para agendar citas y ver su historial y vacunas."
          accion={botonRegistrar}
        />
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {mascotas.map((mascota) => (
            <li key={mascota.id}>
              <TarjetaMascota
                mascota={mascota}
                alEditar={() => setDialogo({ mascota })}
              />
            </li>
          ))}
        </ul>
      )}

      <FormularioMascotaDialog
        abierto={dialogo !== null}
        onAbiertoChange={(abierto) => !abierto && setDialogo(null)}
        duenoId={duenoId}
        mascota={dialogo?.mascota}
      />
    </>
  )
}

interface TarjetaMascotaProps {
  mascota: Mascota
  alEditar: () => void
}

function TarjetaMascota({ mascota, alEditar }: TarjetaMascotaProps) {
  return (
    <article className="group relative overflow-hidden rounded-xl border bg-card transition-colors hover:border-primary/40">
      <Link
        to={`/dueno/mascotas/${mascota.id}`}
        className="block focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
      >
        <FotoMascota
          nombre={mascota.nombre}
          especie={mascota.especie}
          fotoUrl={mascota.fotoUrl}
        />
        <div className="flex flex-col gap-1 p-4">
          <h2 className="text-lg font-semibold tracking-tight">
            {mascota.nombre}
          </h2>
          <p className="text-sm text-muted-foreground">
            {etiquetaEspecie(mascota.especie)} · {mascota.raza}
          </p>
          <p className="text-sm text-muted-foreground">
            {edadTexto(mascota.fechaNacimiento)} · {pesoTexto(mascota.peso)}
          </p>
        </div>
      </Link>
      <Button
        variant="outline"
        size="icon"
        onClick={alEditar}
        aria-label={`Editar a ${mascota.nombre}`}
        className="absolute top-3 right-3 bg-card/90 backdrop-blur-sm"
      >
        <Pencil />
      </Button>
    </article>
  )
}
