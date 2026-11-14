import type { ReactNode } from "react"
import { Controller, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Link, useNavigate, useParams } from "react-router-dom"
import {
  CircleAlert,
  ClipboardCheck,
  ClipboardX,
  LoaderCircle,
  Syringe,
} from "lucide-react"
import { toast } from "sonner"
import { ahoraIsoLocal } from "@/api/http"
import { estadoHttp, mensajeDeError } from "@/api/errores"
import type { Cita, MascotaDetalle } from "@/api/types"
import { EncabezadoPagina } from "@/components/encabezado-pagina"
import { EstadoError } from "@/components/estado-error"
import { EstadoVacio } from "@/components/estado-vacio"
import { ListaTextos } from "@/components/lista-textos"
import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { FotoMascota } from "@/features/duenos-mascotas/foto-mascota"
import {
  edadTexto,
  etiquetaEspecie,
} from "@/features/duenos-mascotas/mascota-utils"
import {
  useMascota,
  useProximasVacunas,
} from "@/features/duenos-mascotas/use-ficha-mascota"
import { usePerfil } from "@/features/perfil/perfil-context"
import {
  etiquetaServicio,
  formatearFechaCorta,
  formatearFechaHora,
} from "@/lib/formato"
import { cn } from "@/lib/utils"
import { useCita, useRegistrarConsulta } from "./use-consulta"

const hoy = () => ahoraIsoLocal().slice(0, 10)

// Mismas reglas que valida el back (CitasController.NuevaConsulta):
// diagnóstico obligatorio, listas de hasta 50 textos, próximo control hoy o
// después. El back vuelve a validar todo.
const esquema = z.object({
  tipoConsulta: z.enum(["GENERAL", "ESPECIALISTA"]),
  diagnostico: z
    .string()
    .trim()
    .min(3, "Escribe el diagnóstico.")
    .max(4000, "Máximo 4000 caracteres."),
  vacunasAplicadas: z.array(z.string().max(200)).max(50),
  tratamientos: z.array(z.string().max(1000)).max(50),
  examenes: z.array(z.string().max(1000)).max(50),
  cirugias: z.array(z.string().max(1000)).max(50),
  observaciones: z.string().trim().max(4000, "Máximo 4000 caracteres."),
  proximoControl: z
    .string()
    .refine(
      (f) => f === "" || f >= hoy(),
      "El próximo control debe ser hoy o una fecha posterior.",
    ),
})

type FormularioConsulta = z.infer<typeof esquema>

/** Ruta "/veterinario/consulta/:citaId". */
export function RegistrarConsultaPage() {
  const citaId = Number(useParams().citaId)
  const { perfil } = usePerfil()

  const { data: cita, isLoading, error, refetch } = useCita(citaId)
  const volverA = cita
    ? `/veterinario/agenda?fecha=${cita.fechaHora.slice(0, 10)}`
    : "/veterinario/agenda"

  if (!citaId) return <CitaNoEncontrada />
  if (isLoading) return <ConsultaCargando />
  if (estadoHttp(error) === 404) return <CitaNoEncontrada />
  if (error) {
    return (
      <>
        <EncabezadoPagina titulo="Registrar consulta" volverA={volverA} />
        <EstadoError
          mensaje={mensajeDeError(error, "No se pudo cargar la cita.")}
          alReintentar={() => refetch()}
        />
      </>
    )
  }
  if (!cita || cita.veterinarioId !== perfil?.id) return <CitaNoEncontrada />

  if (cita.estado !== "AGENDADA") {
    return (
      <>
        <EncabezadoPagina titulo="Registrar consulta" volverA={volverA} />
        <EstadoVacio
          icono={cita.estado === "COMPLETADA" ? ClipboardCheck : ClipboardX}
          titulo={
            cita.estado === "COMPLETADA"
              ? "Esta cita ya tiene la consulta registrada."
              : "Esta cita fue cancelada."
          }
          descripcion="Solo se puede registrar la consulta de una cita agendada."
          accion={
            <Button asChild variant="outline">
              <Link to={volverA}>Volver a la agenda</Link>
            </Button>
          }
        />
      </>
    )
  }

  return <ConsultaDeCita cita={cita} volverA={volverA} />
}

/** Con la cita ya cargada: datos del paciente (de Mascotas) + formulario. */
function ConsultaDeCita({ cita, volverA }: { cita: Cita; volverA: string }) {
  const ficha = useMascota(cita.mascotaId)
  return (
    <>
      <EncabezadoPagina
        titulo="Registrar consulta"
        descripcion={`${ficha.data?.nombre ?? `Mascota #${cita.mascotaId}`} · ${formatearFechaHora(cita.fechaHora)}`}
        volverA={volverA}
      />
      <div className="grid items-start gap-6 lg:grid-cols-[1fr_340px]">
        <FormularioConsulta cita={cita} ficha={ficha.data} volverA={volverA} />
        <ContextoPaciente
          cita={cita}
          ficha={ficha.data}
          cargando={ficha.isLoading}
        />
      </div>
    </>
  )
}

function FormularioConsulta({
  cita,
  ficha,
  volverA,
}: {
  cita: Cita
  ficha?: MascotaDetalle
  volverA: string
}) {
  const navigate = useNavigate()
  const registrar = useRegistrarConsulta(cita.mascotaId)
  const vacunasPendientes = useProximasVacunas(cita.mascotaId)

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormularioConsulta>({
    resolver: zodResolver(esquema),
    defaultValues: {
      tipoConsulta:
        cita.tipoServicio === "ESPECIALISTA" ? "ESPECIALISTA" : "GENERAL",
      diagnostico: "",
      vacunasAplicadas: [],
      tratamientos: [],
      examenes: [],
      cirugias: [],
      observaciones: "",
      proximoControl: "",
    },
  })

  function enviar(valores: FormularioConsulta) {
    registrar.mutate(
      {
        citaId: cita.id,
        datos: {
          ...valores,
          observaciones: valores.observaciones || undefined,
          proximoControl: valores.proximoControl || undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success(
            ficha
              ? `Consulta registrada. El resumen se enviará a ${ficha.duenoEmail}.`
              : "Consulta registrada.",
          )
          navigate(volverA)
        },
      },
    )
  }

  return (
    <form
      noValidate
      onSubmit={handleSubmit(enviar)}
      className="flex flex-col gap-6 rounded-xl border bg-card p-5"
    >
      <FieldGroup>
        <Field>
          <FieldLabel>Tipo de consulta</FieldLabel>
          <Controller
            control={control}
            name="tipoConsulta"
            render={({ field }) => (
              <div
                role="radiogroup"
                aria-label="Tipo de consulta"
                className="grid grid-cols-2 gap-2 sm:max-w-sm"
              >
                {(["GENERAL", "ESPECIALISTA"] as const).map((tipo) => (
                  <button
                    key={tipo}
                    type="button"
                    role="radio"
                    aria-checked={field.value === tipo}
                    onClick={() => field.onChange(tipo)}
                    className={cn(
                      "h-9 rounded-md border text-sm font-medium transition-colors",
                      field.value === tipo
                        ? "border-primary bg-brand-soft text-primary"
                        : "bg-card hover:border-primary/40",
                    )}
                  >
                    {tipo === "GENERAL" ? "General" : "Especialista"}
                  </button>
                ))}
              </div>
            )}
          />
        </Field>

        <Field data-invalid={!!errors.diagnostico}>
          <FieldLabel htmlFor="diagnostico">Diagnóstico</FieldLabel>
          <Textarea
            id="diagnostico"
            rows={3}
            placeholder="Ej. Otitis leve en oído derecho"
            aria-invalid={!!errors.diagnostico}
            {...register("diagnostico")}
          />
          <FieldError errors={[errors.diagnostico]} />
        </Field>

        <Field>
          <FieldLabel htmlFor="vacunas">Vacunas aplicadas</FieldLabel>
          <Controller
            control={control}
            name="vacunasAplicadas"
            render={({ field }) => (
              <ListaTextos
                id="vacunas"
                valor={field.value}
                onChange={field.onChange}
                placeholder="Ej. Rabia"
                sugerencias={vacunasPendientes.data?.map((v) => v.vacuna)}
              />
            )}
          />
          <FieldDescription>
            Las vacunas aplicadas recalculan el esquema de la mascota.
          </FieldDescription>
        </Field>

        <Field>
          <FieldLabel htmlFor="tratamientos">Tratamientos</FieldLabel>
          <Controller
            control={control}
            name="tratamientos"
            render={({ field }) => (
              <ListaTextos
                id="tratamientos"
                valor={field.value}
                onChange={field.onChange}
                placeholder="Ej. Gotas óticas 7 días"
                maxLargo={1000}
              />
            )}
          />
        </Field>

        <Field>
          <FieldLabel htmlFor="examenes">Exámenes</FieldLabel>
          <Controller
            control={control}
            name="examenes"
            render={({ field }) => (
              <ListaTextos
                id="examenes"
                valor={field.value}
                onChange={field.onChange}
                placeholder="Ej. Otoscopia"
                maxLargo={1000}
              />
            )}
          />
        </Field>

        <Field>
          <FieldLabel htmlFor="cirugias">Cirugías</FieldLabel>
          <Controller
            control={control}
            name="cirugias"
            render={({ field }) => (
              <ListaTextos
                id="cirugias"
                valor={field.value}
                onChange={field.onChange}
                placeholder="Ej. Esterilización"
                maxLargo={1000}
              />
            )}
          />
        </Field>

        <Field data-invalid={!!errors.observaciones}>
          <FieldLabel htmlFor="observaciones">
            Observaciones{" "}
            <span className="font-normal text-muted-foreground">
              (opcional)
            </span>
          </FieldLabel>
          <Textarea
            id="observaciones"
            rows={2}
            placeholder="Ej. Control en 2 semanas"
            aria-invalid={!!errors.observaciones}
            {...register("observaciones")}
          />
          <FieldError errors={[errors.observaciones]} />
        </Field>

        <Field data-invalid={!!errors.proximoControl} className="sm:max-w-xs">
          <FieldLabel htmlFor="proximo-control">
            Próximo control{" "}
            <span className="font-normal text-muted-foreground">
              (opcional)
            </span>
          </FieldLabel>
          <Input
            id="proximo-control"
            type="date"
            min={hoy()}
            aria-invalid={!!errors.proximoControl}
            {...register("proximoControl")}
          />
          <FieldError errors={[errors.proximoControl]} />
        </Field>
      </FieldGroup>

      {registrar.isError && (
        <div
          role="alert"
          className="flex items-start gap-2 rounded-lg bg-destructive/5 p-3 text-sm text-destructive"
        >
          <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
          {mensajeDeError(registrar.error, "No se pudo registrar la consulta.")}
        </div>
      )}

      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button asChild variant="outline">
          <Link to={volverA}>Cancelar</Link>
        </Button>
        <Button type="submit" disabled={registrar.isPending}>
          {registrar.isPending ? (
            <LoaderCircle className="animate-spin" />
          ) : (
            <ClipboardCheck />
          )}
          Registrar consulta
        </Button>
      </div>
    </form>
  )
}

function ContextoPaciente({
  cita,
  ficha,
  cargando,
}: {
  cita: Cita
  ficha?: MascotaDetalle
  cargando: boolean
}) {
  const vacunas = useProximasVacunas(cita.mascotaId)
  return (
    <aside className="flex flex-col gap-4 lg:sticky lg:top-24">
      <section className="overflow-hidden rounded-xl border bg-card">
        {cargando ? (
          <Skeleton className="aspect-[4/3] rounded-none" />
        ) : ficha ? (
          <FotoMascota
            nombre={ficha.nombre}
            especie={ficha.especie}
            fotoUrl={ficha.fotoUrl}
          />
        ) : null}
        <dl className="flex flex-col gap-3 p-4 text-sm">
          {ficha && (
            <>
              <Dato etiqueta="Paciente">
                <Link
                  to={`/veterinario/pacientes/${ficha.id}`}
                  className="font-medium hover:underline"
                >
                  {ficha.nombre}
                </Link>{" "}
                · {etiquetaEspecie(ficha.especie)}, {ficha.raza},{" "}
                {edadTexto(ficha.fechaNacimiento)}
              </Dato>
              <Dato etiqueta="Dueño">
                {ficha.duenoNombre}
                <span className="block text-muted-foreground">
                  {ficha.duenoEmail}
                </span>
              </Dato>
            </>
          )}
          {!ficha && !cargando && (
            <Dato etiqueta="Paciente">Mascota #{cita.mascotaId}</Dato>
          )}
          <Dato etiqueta="Cita">
            {formatearFechaHora(cita.fechaHora)} ·{" "}
            {etiquetaServicio(cita.tipoServicio)}
          </Dato>
        </dl>
      </section>

      <section className="rounded-xl border bg-card p-4">
        <h2 className="mb-3 text-sm font-semibold">Vacunas pendientes</h2>
        {vacunas.isLoading ? (
          <Skeleton className="h-12" />
        ) : vacunas.isError ? (
          <p className="text-sm text-muted-foreground">
            No se pudieron cargar.
          </p>
        ) : !vacunas.data?.length ? (
          <p className="text-sm text-muted-foreground">
            No tiene vacunas pendientes.
          </p>
        ) : (
          <ul className="flex flex-col gap-2">
            {vacunas.data.map((v) => (
              <li
                key={`${v.vacuna}-${v.fechaLimite}`}
                className="flex items-center gap-2 rounded-lg bg-alerta-soft px-3 py-2 text-sm text-alerta-texto"
              >
                <Syringe className="size-4 shrink-0 text-alerta" aria-hidden />
                {v.vacuna}, {formatearFechaCorta(v.fechaLimite)}
              </li>
            ))}
          </ul>
        )}
      </section>
    </aside>
  )
}

function Dato({
  etiqueta,
  children,
}: {
  etiqueta: string
  children: ReactNode
}) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd>{children}</dd>
    </div>
  )
}

function CitaNoEncontrada() {
  return (
    <>
      <EncabezadoPagina
        titulo="Registrar consulta"
        volverA="/veterinario/agenda"
      />
      <EstadoVacio
        icono={ClipboardX}
        titulo="No encontramos esta cita en tu agenda."
        accion={
          <Button asChild variant="outline">
            <Link to="/veterinario/agenda">Ir a la agenda</Link>
          </Button>
        }
      />
    </>
  )
}

function ConsultaCargando() {
  return (
    <div aria-busy="true" className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-4 w-16" />
        <Skeleton className="h-8 w-56" />
        <Skeleton className="h-4 w-64" />
      </div>
      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <Skeleton className="h-[480px] rounded-xl" />
        <Skeleton className="h-80 rounded-xl" />
      </div>
    </div>
  )
}
