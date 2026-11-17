import { useState } from "react"
import { Bird, Cat, Dog, PawPrint, type LucideIcon } from "lucide-react"
import type { Especie } from "@/api/types"
import { cn } from "@/lib/utils"

const ICONO: Record<Especie, LucideIcon> = {
  PERRO: Dog,
  GATO: Cat,
  AVE: Bird,
  OTRO: PawPrint,
}

interface FotoMascotaProps {
  nombre: string
  especie: Especie
  fotoUrl?: string | null
  className?: string
}

/**
 * Foto de la mascota. Si no tiene foto o no carga, muestra el ícono de su
 * especie sobre brand-soft, para que nunca quede un espacio vacío.
 */
export function FotoMascota({
  nombre,
  especie,
  fotoUrl,
  className,
}: FotoMascotaProps) {
  const [fallo, setFallo] = useState<string | null>(null)
  const Icono = ICONO[especie]
  const mostrarFoto = fotoUrl && fallo !== fotoUrl

  return (
    <div
      className={cn(
        "relative aspect-[4/3] overflow-hidden bg-brand-soft",
        className,
      )}
    >
      {mostrarFoto ? (
        <img
          src={fotoUrl}
          alt={`Foto de ${nombre}`}
          loading="lazy"
          onError={() => setFallo(fotoUrl)}
          className="size-full object-cover"
        />
      ) : (
        <div className="flex size-full items-center justify-center">
          <Icono className="size-12 text-primary/60" aria-hidden />
        </div>
      )}
    </div>
  )
}
