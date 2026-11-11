import { useEffect, useMemo, useState, type ChangeEvent } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { CircleAlert, ImagePlus, LoaderCircle } from "lucide-react"
import { toast } from "sonner"
import { mensajeDeError } from "@/api/errores"
import { ahoraIsoLocal } from "@/api/http"
import type { Mascota } from "@/api/types"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { FotoMascota } from "./foto-mascota"
import { ESPECIES } from "./mascota-utils"
import { useGuardarMascota } from "./use-mascotas"

const TIPOS_FOTO = ["image/jpeg", "image/png", "image/webp"]
const MAX_FOTO_MB = 5

const esquema = z.object({
  nombre: z
    .string()
    .trim()
    .min(2, "Escribe el nombre de tu mascota.")
    .max(60, "Máximo 60 caracteres."),
  especie: z.enum(["PERRO", "GATO", "AVE", "OTRO"], {
    error: "Elige la especie.",
  }),
  raza: z
    .string()
    .trim()
    .min(2, "Escribe la raza (o «Criollo» si no tiene).")
    .max(60, "Máximo 60 caracteres."),
  fechaNacimiento: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Elige la fecha de nacimiento.")
    .refine(
      (fecha) => fecha <= ahoraIsoLocal().slice(0, 10),
      "La fecha no puede ser futura.",
    )
    .refine((fecha) => fecha >= "1990-01-01", "Revisa el año de nacimiento."),
  peso: z
    .number({ error: "Escribe el peso en kg." })
    .positive("El peso debe ser mayor que 0.")
    .max(150, "Revisa el peso: máximo 150 kg."),
})

type FormularioMascota = z.infer<typeof esquema>

interface FormularioMascotaDialogProps {
  abierto: boolean
  onAbiertoChange: (abierto: boolean) => void
  duenoId: number
  /** Si se pasa, el diálogo edita esa mascota; si no, registra una nueva. */
  mascota?: Mascota
}

/** Diálogo para registrar o editar una mascota, con foto opcional. */
export function FormularioMascotaDialog({
  abierto,
  onAbiertoChange,
  duenoId,
  mascota,
}: FormularioMascotaDialogProps) {
  return (
    <Dialog open={abierto} onOpenChange={onAbiertoChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        {/* El contenido se crea de nuevo cada vez que se abre el diálogo
            (y la key cambia con la mascota), así el formulario siempre
            empieza limpio o con los datos de la mascota a editar. */}
        <ContenidoFormulario
          key={mascota?.id ?? "nueva"}
          duenoId={duenoId}
          mascota={mascota}
          alTerminar={() => onAbiertoChange(false)}
        />
      </DialogContent>
    </Dialog>
  )
}

interface ContenidoFormularioProps {
  duenoId: number
  mascota?: Mascota
  alTerminar: () => void
}

function ContenidoFormulario({
  duenoId,
  mascota,
  alTerminar,
}: ContenidoFormularioProps) {
  const editando = !!mascota
  const guardar = useGuardarMascota(duenoId)
  const [foto, setFoto] = useState<File | null>(null)
  const [errorFoto, setErrorFoto] = useState<string | null>(null)

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormularioMascota>({
    resolver: zodResolver(esquema),
    defaultValues: mascota
      ? {
          nombre: mascota.nombre,
          especie: mascota.especie,
          raza: mascota.raza,
          fechaNacimiento: mascota.fechaNacimiento,
          peso: mascota.peso,
        }
      : { nombre: "", raza: "", fechaNacimiento: "" },
  })

  // Vista previa de la foto elegida (se libera al cambiarla o cerrar).
  const vistaPrevia = useMemo(
    () => (foto ? URL.createObjectURL(foto) : undefined),
    [foto],
  )
  useEffect(() => {
    return () => {
      if (vistaPrevia) URL.revokeObjectURL(vistaPrevia)
    }
  }, [vistaPrevia])

  function elegirFoto(evento: ChangeEvent<HTMLInputElement>) {
    const archivo = evento.target.files?.[0]
    evento.target.value = ""
    if (!archivo) return
    if (!TIPOS_FOTO.includes(archivo.type)) {
      setErrorFoto("Usa una imagen JPG, PNG o WebP.")
      return
    }
    if (archivo.size > MAX_FOTO_MB * 1024 * 1024) {
      setErrorFoto(`La foto pesa más de ${MAX_FOTO_MB} MB.`)
      return
    }
    setErrorFoto(null)
    setFoto(archivo)
  }

  function enviar(valores: FormularioMascota) {
    guardar.mutate(
      { mascotaId: mascota?.id, datos: valores, foto: foto ?? undefined },
      {
        onSuccess: ({ mascota: guardada, errorFoto: falloFoto }) => {
          if (falloFoto) {
            toast.warning(
              `Se guardó a ${guardada.nombre}, pero no se pudo subir la foto. ${mensajeDeError(falloFoto, "")}`.trim(),
            )
          } else {
            toast.success(
              editando
                ? `Se guardaron los cambios de ${guardada.nombre}.`
                : `${guardada.nombre} ya está en tus mascotas.`,
            )
          }
          alTerminar()
        },
      },
    )
  }

  // Para la vista previa de la foto (nombre en el alt e ícono de la especie).
  const nombreActual = useWatch({ control, name: "nombre" })
  const especieActual = useWatch({ control, name: "especie" })

  return (
    <>
      <DialogHeader>
        <DialogTitle>
          {editando ? `Editar a ${mascota.nombre}` : "Registrar mascota"}
        </DialogTitle>
        <DialogDescription>
          {editando
            ? "Actualiza los datos o cambia la foto."
            : "Con estos datos la clínica calcula su esquema de vacunación."}
        </DialogDescription>
      </DialogHeader>

      <form
        id="formulario-mascota"
        noValidate
        onSubmit={handleSubmit(enviar)}
        className="flex flex-col gap-6"
      >
        <div className="flex items-center gap-4">
          <FotoMascota
            nombre={nombreActual || "la mascota"}
            especie={especieActual ?? mascota?.especie ?? "OTRO"}
            fotoUrl={vistaPrevia ?? mascota?.fotoUrl}
            className="w-28 shrink-0 rounded-lg"
          />
          <div className="flex flex-col gap-2">
            <Button type="button" variant="outline" asChild>
              <label htmlFor="foto-mascota" className="cursor-pointer">
                <ImagePlus />
                {vistaPrevia || mascota?.fotoUrl
                  ? "Cambiar foto"
                  : "Agregar foto"}
              </label>
            </Button>
            <input
              id="foto-mascota"
              type="file"
              accept={TIPOS_FOTO.join(",")}
              className="sr-only"
              onChange={elegirFoto}
            />
            <p className="text-xs text-muted-foreground">
              JPG, PNG o WebP, hasta {MAX_FOTO_MB} MB. Opcional.
            </p>
            {errorFoto && (
              <p role="alert" className="text-xs text-destructive">
                {errorFoto}
              </p>
            )}
          </div>
        </div>

        <FieldGroup>
          <Field data-invalid={!!errors.nombre}>
            <FieldLabel htmlFor="nombre-mascota">Nombre</FieldLabel>
            <Input
              id="nombre-mascota"
              placeholder="Tobby"
              aria-invalid={!!errors.nombre}
              {...register("nombre")}
            />
            <FieldError errors={[errors.nombre]} />
          </Field>

          <div className="grid gap-5 sm:grid-cols-2">
            <Field data-invalid={!!errors.especie}>
              <FieldLabel htmlFor="especie">Especie</FieldLabel>
              <Controller
                control={control}
                name="especie"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger
                      id="especie"
                      aria-invalid={!!errors.especie}
                      className="w-full"
                    >
                      <SelectValue placeholder="Elige la especie" />
                    </SelectTrigger>
                    <SelectContent>
                      {ESPECIES.map((e) => (
                        <SelectItem key={e.valor} value={e.valor}>
                          {e.etiqueta}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              <FieldError errors={[errors.especie]} />
            </Field>

            <Field data-invalid={!!errors.raza}>
              <FieldLabel htmlFor="raza">Raza</FieldLabel>
              <Input
                id="raza"
                placeholder="Golden Retriever"
                aria-invalid={!!errors.raza}
                {...register("raza")}
              />
              <FieldError errors={[errors.raza]} />
            </Field>

            <Field data-invalid={!!errors.fechaNacimiento}>
              <FieldLabel htmlFor="fecha-nacimiento">
                Fecha de nacimiento
              </FieldLabel>
              <Input
                id="fecha-nacimiento"
                type="date"
                max={ahoraIsoLocal().slice(0, 10)}
                aria-invalid={!!errors.fechaNacimiento}
                {...register("fechaNacimiento")}
              />
              <FieldDescription>
                Si no la sabes, una aproximada.
              </FieldDescription>
              <FieldError errors={[errors.fechaNacimiento]} />
            </Field>

            <Field data-invalid={!!errors.peso}>
              <FieldLabel htmlFor="peso">Peso (kg)</FieldLabel>
              <Input
                id="peso"
                type="number"
                inputMode="decimal"
                step="0.01"
                min="0"
                placeholder="31.5"
                aria-invalid={!!errors.peso}
                {...register("peso", { valueAsNumber: true })}
              />
              <FieldError errors={[errors.peso]} />
            </Field>
          </div>
        </FieldGroup>

        {guardar.isError && (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/5 p-3 text-sm text-destructive"
          >
            <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
            {mensajeDeError(guardar.error, "No se pudo guardar la mascota.")}
          </div>
        )}
      </form>

      <DialogFooter>
        <DialogClose asChild>
          <Button type="button" variant="outline">
            Cancelar
          </Button>
        </DialogClose>
        <Button
          type="submit"
          form="formulario-mascota"
          disabled={guardar.isPending}
        >
          {guardar.isPending && <LoaderCircle className="animate-spin" />}
          {editando ? "Guardar cambios" : "Registrar mascota"}
        </Button>
      </DialogFooter>
    </>
  )
}
