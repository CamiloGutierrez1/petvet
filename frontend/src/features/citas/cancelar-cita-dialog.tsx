import { CircleAlert, LoaderCircle } from "lucide-react"
import { toast } from "sonner"
import { mensajeDeError } from "@/api/errores"
import type { Cita } from "@/api/types"
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
import { formatearFechaHora } from "@/lib/formato"
import { useCancelarCita } from "./use-mis-citas"

interface CancelarCitaDialogProps {
  /** Cita a cancelar; null cierra el diálogo. */
  cita: Cita | null
  nombreMascota: string
  onCerrar: () => void
}

/** Pide confirmación antes de cancelar una cita. */
export function CancelarCitaDialog({
  cita,
  nombreMascota,
  onCerrar,
}: CancelarCitaDialogProps) {
  const cancelar = useCancelarCita()

  function confirmar() {
    if (!cita) return
    cancelar.mutate(cita.id, {
      onSuccess: () => {
        toast.success(`Se canceló la cita de ${nombreMascota}.`)
        onCerrar()
      },
    })
  }

  return (
    <Dialog
      open={cita !== null}
      onOpenChange={(abierto) => {
        if (!abierto) {
          cancelar.reset()
          onCerrar()
        }
      }}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>¿Cancelar esta cita?</DialogTitle>
          <DialogDescription>
            {cita &&
              `La cita de ${nombreMascota} del ${formatearFechaHora(cita.fechaHora)} quedará cancelada y el horario se liberará.`}
          </DialogDescription>
        </DialogHeader>

        {cancelar.isError && (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/5 p-3 text-sm text-destructive"
          >
            <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
            {mensajeDeError(cancelar.error, "No se pudo cancelar la cita.")}
          </div>
        )}

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">Mantener cita</Button>
          </DialogClose>
          <Button
            variant="destructive"
            onClick={confirmar}
            disabled={cancelar.isPending}
          >
            {cancelar.isPending && <LoaderCircle className="animate-spin" />}
            Cancelar cita
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
