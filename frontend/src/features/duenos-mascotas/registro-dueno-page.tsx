import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Link, useNavigate, useSearchParams } from "react-router-dom"
import { ArrowLeft, CircleAlert, LoaderCircle } from "lucide-react"
import { toast } from "sonner"
import { mensajeDeError } from "@/api/errores"
import { LayoutEntrada } from "@/features/perfil/layout-entrada"
import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { usePerfil } from "@/features/perfil/perfil-context"
import { useRegistrarDueno } from "./use-registrar-dueno"

// Validación en el navegador (Zod). El back vuelve a validar: el front solo
// ayuda a que el usuario corrija antes de enviar.
const esquema = z.object({
  nombre: z
    .string()
    .trim()
    .min(3, "Escribe tu nombre completo.")
    .max(100, "Máximo 100 caracteres."),
  email: z
    .string()
    .trim()
    .email("Escribe un correo válido, ej. ana@correo.com."),
  telefono: z
    .string()
    .trim()
    .regex(/^\d{7,10}$/, "Solo números, entre 7 y 10 dígitos.")
    .or(z.literal("")),
})

type FormularioDueno = z.infer<typeof esquema>

/** Ruta "/registro": crea un dueño y entra a la app con ese perfil. */
export function RegistroDuenoPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { elegirPerfil } = usePerfil()
  const registrar = useRegistrarDueno()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormularioDueno>({
    resolver: zodResolver(esquema),
    // ?email= viene de la entrada cuando el correo no estaba registrado.
    defaultValues: {
      nombre: "",
      email: params.get("email") ?? "",
      telefono: "",
    },
  })

  function enviar(valores: FormularioDueno) {
    registrar.mutate(
      {
        nombre: valores.nombre,
        email: valores.email,
        telefono: valores.telefono || undefined,
      },
      {
        onSuccess: (dueno) => {
          elegirPerfil({ tipo: "dueno", id: dueno.id, nombre: dueno.nombre })
          toast.success("Registro completo. Ahora agrega a tus mascotas.")
          navigate("/dueno/mascotas")
        },
      },
    )
  }

  return (
    <LayoutEntrada>
      <div className="flex flex-col gap-6">
        <Link
          to="/"
          className="inline-flex w-fit items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-4" aria-hidden />
          Volver
        </Link>

        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">
            Registro de dueño
          </h1>
          <p className="text-sm text-muted-foreground">
            Con tus datos podrás registrar a tus mascotas y agendar citas. Te
            enviaremos las confirmaciones a tu correo.
          </p>
        </div>

        <form
          noValidate
          onSubmit={handleSubmit(enviar)}
          className="flex flex-col gap-6"
        >
          <FieldGroup>
            <Field data-invalid={!!errors.nombre}>
              <FieldLabel htmlFor="nombre">Nombre completo</FieldLabel>
              <Input
                id="nombre"
                autoComplete="name"
                placeholder="Laura Méndez"
                aria-invalid={!!errors.nombre}
                {...register("nombre")}
              />
              <FieldError errors={[errors.nombre]} />
            </Field>

            <Field data-invalid={!!errors.email}>
              <FieldLabel htmlFor="email">Correo electrónico</FieldLabel>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="laura@correo.com"
                aria-invalid={!!errors.email}
                {...register("email")}
              />
              <FieldError errors={[errors.email]} />
            </Field>

            <Field data-invalid={!!errors.telefono}>
              <FieldLabel htmlFor="telefono">
                Teléfono{" "}
                <span className="font-normal text-muted-foreground">
                  (opcional)
                </span>
              </FieldLabel>
              <Input
                id="telefono"
                type="tel"
                inputMode="numeric"
                autoComplete="tel"
                placeholder="3104567812"
                aria-invalid={!!errors.telefono}
                {...register("telefono")}
              />
              <FieldDescription>
                Para avisarte si cambia el horario de una cita.
              </FieldDescription>
              <FieldError errors={[errors.telefono]} />
            </Field>
          </FieldGroup>

          {registrar.isError && (
            <div
              role="alert"
              className="flex items-start gap-2 rounded-lg bg-destructive/5 p-3 text-sm text-destructive"
            >
              <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
              {mensajeDeError(
                registrar.error,
                "No se pudo completar el registro.",
              )}
            </div>
          )}

          <Button type="submit" size="lg" disabled={registrar.isPending}>
            {registrar.isPending && <LoaderCircle className="animate-spin" />}
            {registrar.isPending ? "Registrando…" : "Registrarme"}
          </Button>
        </form>

        <p className="text-sm text-muted-foreground">
          ¿Ya estás registrado?{" "}
          <Link
            to="/?paso=dueno"
            className="font-medium text-primary underline-offset-4 hover:underline"
          >
            Entra con tu correo
          </Link>
        </p>
      </div>
    </LayoutEntrada>
  )
}
